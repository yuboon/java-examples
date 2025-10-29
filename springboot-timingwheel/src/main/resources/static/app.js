// 修复404错误的取消任务功能版本

// 全局变量
let autoRefresh = true;
let refreshInterval;
let performanceChart;
let performanceData = {
    labels: [],
    activeTasks: [],
    completedTasks: [],
    failedTasks: [],
    completionRate: []
};

// API基础URL
const API_BASE = '/api/timingwheel';

// 初始化应用
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
});

function initializeApp() {
    initializeTimingWheel();
    initializePerformanceChart();
    startAutoRefresh();
    updateCurrentTime();
    setInterval(updateCurrentTime, 1000);

    // 初始加载数据
    loadAllData();
}

// 初始化时间轮可视化
function initializeTimingWheel() {
    const wheel = document.getElementById('timingWheel');
    const numSlots = 512;

    // 清空现有槽位
    wheel.querySelectorAll('.slot').forEach(slot => slot.remove());

    // 创建槽位元素
    for (let i = 0; i < numSlots; i++) {
        const slot = document.createElement('div');
        slot.className = 'slot';
        slot.id = `slot-${i}`;

        // 计算槽位位置（圆形分布）- 适配400px尺寸
        const angle = (i * 360 / numSlots) - 90;
        const radius = 190; // 适应新的400px尺寸
        const centerX = 200;
        const centerY = 200;
        const slotSize = 6;

        const x = centerX + radius * Math.cos(angle * Math.PI / 180) - slotSize/2;
        const y = centerY + radius * Math.sin(angle * Math.PI / 180) - slotSize/2;

        slot.style.left = x + 'px';
        slot.style.top = y + 'px';
        slot.title = `槽位 ${i}`;

        wheel.appendChild(slot);
    }
}

// 初始化性能图表
function initializePerformanceChart() {
    const ctx = document.getElementById('performanceChart').getContext('2d');
    performanceChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                {
                    label: '活跃任务数',
                    data: [],
                    borderColor: 'rgb(245, 158, 11)',
                    backgroundColor: 'rgba(245, 158, 11, 0.1)',
                    tension: 0.4
                },
                {
                    label: '已完成任务',
                    data: [],
                    borderColor: 'rgb(16, 185, 129)',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    tension: 0.4
                },
                {
                    label: '失败任务数',
                    data: [],
                    borderColor: 'rgb(239, 68, 68)',
                    backgroundColor: 'rgba(239, 68, 68, 0.1)',
                    tension: 0.4
                },
                {
                    label: '任务完成率(%)',
                    data: [],
                    borderColor: 'rgb(139, 92, 246)',
                    backgroundColor: 'rgba(139, 92, 246, 0.1)',
                    tension: 0.4,
                    yAxisID: 'y1',
                    borderDash: [5, 5]
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: {
                duration: 0
            },
            interaction: {
                mode: 'index',
                intersect: false,
            },
            layout: {
                padding: {
                    top: 10,
                    right: 10,
                    bottom: 10,
                    left: 10
                }
            },
            scales: {
                x: {
                    display: true,
                    title: {
                        display: false
                    },
                    ticks: {
                        maxRotation: 45,
                        minRotation: 45,
                        autoSkip: true,
                        maxTicksLimit: 8
                    }
                },
                y: {
                    type: 'linear',
                    display: true,
                    position: 'left',
                    beginAtZero: true,
                    max: 100,
                    title: {
                        display: true,
                        text: '任务数量'
                    },
                    ticks: {
                        stepSize: 10
                    }
                },
                y1: {
                    type: 'linear',
                    display: true,
                    position: 'right',
                    beginAtZero: true,
                    max: 100,
                    title: {
                        display: true,
                        text: '完成率(%)'
                    },
                    grid: {
                        drawOnChartArea: false,
                    },
                    ticks: {
                        stepSize: 20
                    }
                }
            },
            plugins: {
                legend: {
                    position: 'top',
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            let label = context.dataset.label || '';
                            if (label) {
                                label += ': ';
                            }
                            if (context.datasetIndex === 3) {
                                label += context.parsed.y.toFixed(1) + '%';
                            } else {
                                label += context.parsed.y;
                            }
                            return label;
                        }
                    }
                }
            }
        }
    });
}

// 开始自动刷新
function startAutoRefresh() {
    if (autoRefresh && !refreshInterval) {
        refreshInterval = setInterval(loadAllData, 2000);
    }
}

// 停止自动刷新
function stopAutoRefresh() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
        refreshInterval = null;
    }
}

// 切换自动刷新状态
function toggleAutoRefresh() {
    autoRefresh = !autoRefresh;
    const statusElement = document.getElementById('refreshStatus');

    if (autoRefresh) {
        startAutoRefresh();
        statusElement.textContent = '自动刷新: 开启';
        showNotification('自动刷新已开启', 'success');
    } else {
        stopAutoRefresh();
        statusElement.textContent = '自动刷新: 关闭';
        showNotification('自动刷新已关闭', 'info');
    }
}

// 加载所有数据
async function loadAllData() {
    try {
        await Promise.all([
            loadStats(),
            loadExecutionStats(),
            loadActiveTasks(),
            loadSystemInfo()
        ]);
    } catch (error) {
        console.error('加载数据失败:', error);
        showNotification('加载数据失败: ' + error.message, 'error');
    }
}

// 加载统计信息
async function loadStats() {
    try {
        const response = await fetch(`${API_BASE}/stats`);
        if (!response.ok) throw new Error('获取统计信息失败');

        const stats = await response.json();
        updateStatsDisplay(stats);
        updateTimingWheel(stats);
        updatePerformanceChart(stats);

    } catch (error) {
        console.error('加载统计信息失败:', error);
    }
}

// 更新统计信息显示
function updateStatsDisplay(stats) {
    document.getElementById('totalTasks').textContent = stats.totalTasks || 0;
    document.getElementById('completedTasks').textContent = stats.completedTasks || 0;
    document.getElementById('failedTasks').textContent = stats.failedTasks || 0;
    document.getElementById('activeTasks').textContent = stats.activeTaskCount || 0;
    document.getElementById('currentSlot').textContent = stats.currentSlot || 0;
    document.getElementById('totalSlots').textContent = stats.slotSize || 512;
    document.getElementById('tickDuration').textContent = stats.tickDuration || 100;
}

// 更新时间轮可视化
function updateTimingWheel(stats) {
    const currentSlot = stats.currentSlot || 0;
    const slotInfos = stats.slotInfos || [];

    // 清除所有状态
    document.querySelectorAll('.slot').forEach(slot => {
        slot.classList.remove('current', 'has-tasks', 'active');
    });

    // 设置当前槽位
    const currentSlotElement = document.getElementById(`slot-${currentSlot}`);
    if (currentSlotElement) {
        currentSlotElement.classList.add('current');
    }

    // 设置有任务的槽位
    slotInfos.forEach(slotInfo => {
        if (slotInfo.taskCount > 0) {
            const slotElement = document.getElementById(`slot-${slotInfo.slotIndex}`);
            if (slotElement && slotInfo.slotIndex !== currentSlot) {
                slotElement.classList.add('has-tasks');
            }
        }
    });
}

// 更新性能图表
function updatePerformanceChart(stats) {
    if (!performanceChart || performanceChart.destroyed) {
        return;
    }

    const now = new Date();
    const timeLabel = now.toLocaleTimeString();

    const totalTasks = stats.totalTasks || 0;
    const completedTasks = stats.completedTasks || 0;
    const failedTasks = stats.failedTasks || 0;
    const activeTasks = stats.activeTaskCount || 0;
    const completionRate = totalTasks > 0 ? (completedTasks / totalTasks * 100) : 0;

    const maxDataPoints = 15;
    if (performanceData.labels.length >= maxDataPoints) {
        performanceData.labels.shift();
        performanceData.activeTasks.shift();
        performanceData.completedTasks.shift();
        performanceData.failedTasks.shift();
        performanceData.completionRate.shift();
    }

    performanceData.labels.push(timeLabel);
    performanceData.activeTasks.push(activeTasks);
    performanceData.completedTasks.push(completedTasks);
    performanceData.failedTasks.push(failedTasks);
    performanceData.completionRate.push(completionRate);

    try {
        performanceChart.data.labels = performanceData.labels;
        performanceChart.data.datasets[0].data = performanceData.activeTasks;
        performanceChart.data.datasets[1].data = performanceData.completedTasks;
        performanceChart.data.datasets[2].data = performanceData.failedTasks;
        performanceChart.data.datasets[3].data = performanceData.completionRate;
        performanceChart.update('none');
    } catch (error) {
        console.error('图表更新失败:', error);
        initializePerformanceChart();
    }
}

// 加载执行统计
async function loadExecutionStats() {
    try {
        const response = await fetch(`${API_BASE}/execution-stats`);
        if (!response.ok) throw new Error('获取执行统计失败');

        const stats = await response.json();
        updateExecutionStatsDisplay(stats);

    } catch (error) {
        console.error('加载执行统计失败:', error);
    }
}

// 更新执行统计显示
function updateExecutionStatsDisplay(stats) {
    const container = document.getElementById('executionStats');

    const statsHtml = `
        <div class="grid grid-cols-2 gap-3">
            <div class="bg-gray-50 p-3 rounded">
                <p class="text-xs text-gray-600">平均调度时间</p>
                <p class="text-lg font-semibold text-gray-800">${(stats.averageScheduleTime || 0).toFixed(2)} ms</p>
            </div>
            <div class="bg-gray-50 p-3 rounded">
                <p class="text-xs text-gray-600">平均执行时间</p>
                <p class="text-lg font-semibold text-gray-800">${(stats.averageExecutionTime || 0).toFixed(2)} ms</p>
            </div>
            <div class="bg-gray-50 p-3 rounded">
                <p class="text-xs text-gray-600">总执行次数</p>
                <p class="text-lg font-semibold text-gray-800">${stats.totalExecutions || 0}</p>
            </div>
            <div class="bg-gray-50 p-3 rounded">
                <p class="text-xs text-gray-600">成功率</p>
                <p class="text-lg font-semibold text-gray-800">${(stats.successRate || 0).toFixed(1)}%</p>
            </div>
        </div>
    `;

    container.innerHTML = statsHtml;
}

// 加载活跃任务
async function loadActiveTasks() {
    try {
        const response = await fetch(`${API_BASE}/tasks`);
        if (!response.ok) throw new Error('获取活跃任务失败');

        const tasks = await response.json();

        // 调试：输出任务数据结构
        if (tasks && tasks.length > 0) {
            console.log('任务数据结构示例:', tasks[0]);
            console.log('所有任务状态:', tasks.map(t => ({ id: t.taskId, status: t.status })));
        }

        updateActiveTasksList(tasks);

    } catch (error) {
        console.error('加载活跃任务失败:', error);
    }
}

// 更新活跃任务列表（网格布局版本）
function updateActiveTasksList(tasks) {
    const container = document.getElementById('activeTasksList');
    const countElement = document.getElementById('activeTasksCount');
    const emptyState = document.getElementById('emptyState');
    const gridContainer = container.querySelector('.grid');

    // 过滤出真正的活跃任务（只显示未完成的任务）
    const activeTasks = tasks ? tasks.filter(task => {
        const status = task.status || 'PENDING';
        return status !== 'COMPLETED' && status !== 'FAILED' && status !== 'CANCELLED';
    }) : [];

    // 更新任务数量
    if (countElement) {
        countElement.textContent = activeTasks.length;
    }

    if (!activeTasks || activeTasks.length === 0) {
        gridContainer.style.display = 'none';
        emptyState.style.display = 'flex';
        return;
    }

    // 显示网格，隐藏空状态
    gridContainer.style.display = 'grid';
    emptyState.style.display = 'none';

    // 创建任务卡片网格
    const tasksHtml = activeTasks.map(task => {
        // 安全获取任务描述
        let taskDescription = '未命名任务';
        if (task.task && task.task.description) {
            taskDescription = task.task.description;
        } else if (task.description) {
            taskDescription = task.description;
        }

        // 安全获取过期时间
        let expireTimeStr = '未知时间';
        const expireTime = task.expireTime || task.expiryTime;
        if (expireTime && typeof expireTime === 'number') {
            try {
                const expireDate = new Date(expireTime);
                const now = new Date();
                const timeDiff = expireDate - now;
                const isExpired = timeDiff < 0;

                expireTimeStr = expireDate.toLocaleString();

                // 添加过期状态样式
                if (isExpired) {
                    expireTimeStr += ' ⚠️ 已过期';
                }
            } catch (e) {
                expireTimeStr = '时间格式错误';
            }
        } else if (expireTime) {
            expireTimeStr = String(expireTime);
        }

        // 安全获取轮次信息
        let roundsStr = '未知';
        if (task.rounds !== undefined && task.rounds !== null) {
            if (typeof task.rounds === 'object') {
                roundsStr = task.rounds.value || task.rounds.get ? task.rounds.get() : '对象';
            } else if (typeof task.rounds === 'number') {
                roundsStr = task.rounds;
            } else {
                roundsStr = String(task.rounds);
            }
        } else if (task.remainingRounds !== undefined) {
            roundsStr = task.remainingRounds;
        }

        // 任务状态
        const status = task.status || 'PENDING';
        const statusColor = {
            'PENDING': 'bg-yellow-100 text-yellow-800 border-yellow-200',
            'RUNNING': 'bg-blue-100 text-blue-800 border-blue-200',
            'COMPLETED': 'bg-green-100 text-green-800 border-green-200',
            'FAILED': 'bg-red-100 text-red-800 border-red-200',
            'CANCELLED': 'bg-gray-100 text-gray-800 border-gray-200'
        }[status] || 'bg-gray-100 text-gray-800 border-gray-200';

        const statusIcon = {
            'PENDING': 'fa-clock',
            'RUNNING': 'fa-spinner fa-spin',
            'COMPLETED': 'fa-check-circle',
            'FAILED': 'fa-times-circle',
            'CANCELLED': 'fa-ban'
        }[status] || 'fa-question-circle';

        const statusText = {
            'PENDING': '等待中',
            'RUNNING': '执行中',
            'COMPLETED': '已完成',
            'FAILED': '失败',
            'CANCELLED': '已取消'
        }[status] || status;

        return `
            <div class="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-all duration-200 hover:border-blue-300 fade-in relative">
                <!-- 状态标签 -->
                <div class="absolute top-2 right-2">
                    <span class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${statusColor} border">
                        <i class="fas ${statusIcon} mr-1"></i>
                        ${statusText}
                    </span>
                </div>

                <!-- 任务内容 -->
                <div class="mb-3">
                    <h3 class="font-medium text-gray-800 text-sm mb-1 pr-16">${taskDescription}</h3>
                    <p class="text-xs text-gray-500 font-mono break-all">${task.taskId}</p>
                </div>

                <!-- 任务详情 -->
                <div class="space-y-2 text-xs">
                    <div class="flex items-center text-gray-600">
                        <i class="fas fa-clock mr-2 text-gray-400"></i>
                        <span class="truncate">${expireTimeStr}</span>
                    </div>
                    <div class="flex items-center text-gray-600">
                        <i class="fas fa-redo mr-2 text-gray-400"></i>
                        <span>轮次: ${roundsStr}</span>
                    </div>
                    ${task.delayMs ? `
                        <div class="flex items-center text-gray-600">
                            <i class="fas fa-hourglass-half mr-2 text-gray-400"></i>
                            <span>延迟: ${task.delayMs}ms</span>
                        </div>
                    ` : ''}
                    ${task.createTime ? `
                        <div class="flex items-center text-gray-600">
                            <i class="fas fa-plus-circle mr-2 text-gray-400"></i>
                            <span>创建: ${new Date(task.createTime).toLocaleTimeString()}</span>
                        </div>
                    ` : ''}
                    ${task.errorMessage ? `
                        <div class="flex items-start text-red-600">
                            <i class="fas fa-exclamation-triangle mr-2 mt-0.5 flex-shrink-0"></i>
                            <span class="break-words">${task.errorMessage}</span>
                        </div>
                    ` : ''}
                </div>

                <!-- 操作按钮 -->
                <div class="mt-3 pt-3 border-t border-gray-100">
                    <button onclick="cancelTask('${task.taskId}')"
                            data-task-id="${task.taskId}"
                            class="w-full bg-red-50 hover:bg-red-100 text-red-600 text-xs font-medium py-2 px-3 rounded transition-colors"
                            title="点击取消此任务">
                        <i class="fas fa-times mr-1"></i>
                        取消任务
                    </button>
                </div>
            </div>
        `;
    }).join('');

    gridContainer.innerHTML = tasksHtml;
}

// 加载系统信息
async function loadSystemInfo() {
    try {
        const response = await fetch(`${API_BASE}/system-info`);
        if (!response.ok) throw new Error('获取系统信息失败');

        const info = await response.json();
        updateSystemInfoDisplay(info);

    } catch (error) {
        console.error('加载系统信息失败:', error);
    }
}

// 更新系统信息显示
function updateSystemInfoDisplay(info) {
    const container = document.getElementById('systemInfo');

    const formatBytes = (bytes) => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    };

    const memoryUsagePercent = ((info.usedMemory / info.totalMemory) * 100).toFixed(1);

    const infoHtml = `
        <div class="space-y-3">
            <div class="flex justify-between items-center">
                <span class="text-sm text-gray-600">CPU核心数</span>
                <span class="text-sm font-medium">${info.availableProcessors}</span>
            </div>
            <div class="border-t pt-3">
                <div class="flex justify-between items-center mb-2">
                    <span class="text-sm text-gray-600">内存使用率</span>
                    <span class="text-sm font-medium">${memoryUsagePercent}%</span>
                </div>
                <div class="w-full bg-gray-200 rounded-full h-2">
                    <div class="bg-blue-600 h-2 rounded-full" style="width: ${memoryUsagePercent}%"></div>
                </div>
                <div class="mt-2 text-xs text-gray-600">
                    <div>已用: ${formatBytes(info.usedMemory)}</div>
                    <div>总计: ${formatBytes(info.totalMemory)}</div>
                    <div>最大: ${formatBytes(info.maxMemory)}</div>
                </div>
            </div>
            <div class="border-t pt-3">
                <div class="flex justify-between items-center">
                    <span class="text-sm text-gray-600">当前时间</span>
                    <span class="text-sm font-medium">${new Date(info.currentTime).toLocaleTimeString()}</span>
                </div>
            </div>
        </div>
    `;

    container.innerHTML = infoHtml;
}

// 创建示例任务
async function createSampleTask() {
    const type = document.getElementById('taskType').value;
    const delay = parseInt(document.getElementById('taskDelay').value);

    try {
        const response = await fetch(`${API_BASE}/tasks/sample`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ type, delay })
        });

        if (!response.ok) throw new Error('创建任务失败');

        const result = await response.json();
        showNotification(`任务创建成功: ${result.taskId}`, 'success');

        loadAllData();

    } catch (error) {
        console.error('创建任务失败:', error);
        showNotification('创建任务失败: ' + error.message, 'error');
    }
}

// 批量创建任务
async function createBatchTasks() {
    const count = parseInt(document.getElementById('batchCount').value);
    const minDelay = parseInt(document.getElementById('minDelay').value);
    const maxDelay = parseInt(document.getElementById('maxDelay').value);

    try {
        const response = await fetch(`${API_BASE}/tasks/batch`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ count, minDelay, maxDelay })
        });

        if (!response.ok) throw new Error('批量创建任务失败');

        const result = await response.json();
        showNotification(`成功创建 ${result.count} 个任务`, 'success');

        loadAllData();

    } catch (error) {
        console.error('批量创建任务失败:', error);
        showNotification('批量创建任务失败: ' + error.message, 'error');
    }
}

// 创建自定义任务
async function createCustomTask() {
    const description = document.getElementById('customDescription').value || '自定义任务';
    const delay = parseInt(document.getElementById('customDelay').value);
    const action = document.getElementById('customAction').value;

    try {
        const response = await fetch(`${API_BASE}/tasks/custom`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ description, delay, action })
        });

        if (!response.ok) throw new Error('创建自定义任务失败');

        const result = await response.json();
        showNotification(`自定义任务创建成功: ${result.taskId}`, 'success');

        document.getElementById('customDescription').value = '';

        loadAllData();

    } catch (error) {
        console.error('创建自定义任务失败:', error);
        showNotification('创建自定义任务失败: ' + error.message, 'error');
    }
}

// 取消任务（修复404错误版本）
async function cancelTask(taskId) {
    const cancelButton = document.querySelector(`[data-task-id="${taskId}"]`);
    if (cancelButton) {
        cancelButton.disabled = true;
        cancelButton.textContent = '取消中...';
    }

    try {
        // 先检查任务是否存在以及状态
        console.log(`检查任务状态: ${taskId}`);
        const checkResponse = await fetch(`${API_BASE}/tasks/${taskId}`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        });

        if (checkResponse.status === 404) {
            showNotification(`任务不存在或已自动完成: ${taskId}`, 'info');
            if (cancelButton) {
                const taskRow = cancelButton.closest('div');
                if (taskRow) {
                    taskRow.style.opacity = '0.5';
                    cancelButton.textContent = '已完成';
                }
            }
            refreshTasks(); // 刷新任务列表
            return;
        }

        if (!checkResponse.ok) {
            const errorText = await checkResponse.text();
            showNotification(`检查任务状态失败: ${errorText}`, 'error');
            return;
        }

        const taskData = await checkResponse.json();
        console.log(`任务当前状态:`, taskData);

        // 检查任务是否已经完成或失败
        if (taskData.status === 'COMPLETED') {
            showNotification(`任务已完成，无需取消: ${taskId}`, 'info');
            if (cancelButton) {
                const taskRow = cancelButton.closest('div');
                if (taskRow) {
                    taskRow.style.opacity = '0.5';
                    cancelButton.textContent = '已完成';
                }
            }
            refreshTasks();
            return;
        }

        if (taskData.status === 'CANCELLED') {
            showNotification(`任务已被取消: ${taskId}`, 'info');
            if (cancelButton) {
                const taskRow = cancelButton.closest('div');
                if (taskRow) {
                    taskRow.style.opacity = '0.5';
                    cancelButton.textContent = '已取消';
                }
            }
            refreshTasks();
            return;
        }

        if (taskData.status === 'FAILED') {
            showNotification(`任务已失败，无需取消: ${taskId}`, 'info');
            if (cancelButton) {
                const taskRow = cancelButton.closest('div');
                if (taskRow) {
                    taskRow.style.opacity = '0.5';
                    cancelButton.textContent = '已失败';
                }
            }
            refreshTasks();
            return;
        }

        // 尝试取消任务
        console.log(`尝试取消任务: ${taskId}`);
        const deleteResponse = await fetch(`${API_BASE}/tasks/${taskId}`, {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json' }
        });

        if (deleteResponse.ok) {
            showNotification('任务取消成功', 'success');
            refreshTasks();
        } else if (deleteResponse.status === 404) {
            showNotification(`取消失败: 任务不存在或已完成 (${taskId})`, 'info');
            refreshTasks();
        } else {
            const errorText = await deleteResponse.text();
            console.error(`取消任务失败:`, {
                status: deleteResponse.status,
                statusText: deleteResponse.statusText,
                errorText: errorText,
                taskId: taskId
            });
            showNotification(`取消任务失败: ${errorText}`, 'error');
        }
    } catch (error) {
        console.error(`取消任务异常:`, error);
        showNotification('取消任务失败: ' + error.message, 'error');
    } finally {
        if (cancelButton) {
            cancelButton.disabled = false;
            // 根据最新的任务状态来更新按钮文本
            const buttonText = cancelButton.textContent;
            if (buttonText === '取消中...') {
                cancelButton.textContent = '取消任务';
            }
        }
    }
}

// 清理已完成的任务
async function cleanupTasks() {
    try {
        const response = await fetch(`${API_BASE}/cleanup`, {
            method: 'POST'
        });

        if (!response.ok) throw new Error('清理任务失败');

        const result = await response.json();
        showNotification(`清理完成，移除了 ${result.removedCount} 个任务`, 'success');

        loadAllData();

    } catch (error) {
        console.error('清理任务失败:', error);
        showNotification('清理任务失败: ' + error.message, 'error');
    }
}

// 执行压力测试
async function performStressTest() {
    const taskCount = parseInt(document.getElementById('stressTaskCount').value);
    const minDelay = parseInt(document.getElementById('stressMinDelay').value);
    const maxDelay = parseInt(document.getElementById('stressMaxDelay').value);

    const resultsContainer = document.getElementById('stressTestResults');
    resultsContainer.innerHTML = '<div class="text-blue-500"><i class="fas fa-spinner fa-spin mr-2"></i>测试进行中...</div>';

    try {
        const startTime = performance.now();

        const response = await fetch(`${API_BASE}/stress-test`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ taskCount, minDelay, maxDelay })
        });

        if (!response.ok) throw new Error('压力测试失败');

        const result = await response.json();
        const endTime = performance.now();

        resultsContainer.innerHTML = `
            <div class="text-green-600">
                <div class="text-sm font-medium">测试完成</div>
                <div class="text-xs text-gray-600 mt-1">
                    创建 ${result.taskCount} 个任务<br>
                    创建时间: ${result.creationTime}ms<br>
                    吞吐量: ${result.throughput.toFixed(2)} 任务/秒
                </div>
            </div>
        `;

        showNotification(`压力测试完成，创建了 ${result.taskCount} 个任务`, 'success');

        loadAllData();

    } catch (error) {
        console.error('压力测试失败:', error);
        resultsContainer.innerHTML = '<div class="text-red-500">测试失败</div>';
        showNotification('压力测试失败: ' + error.message, 'error');
    }
}

// 刷新任务列表
function refreshTasks() {
    loadActiveTasks();
    showNotification('任务列表已刷新', 'info');
}

// 更新当前时间
function updateCurrentTime() {
    const now = new Date();
    document.getElementById('currentTime').textContent = now.toLocaleString();
}

// 显示通知
function showNotification(message, type = 'success') {
    const notification = document.getElementById('notification');
    const notificationText = document.getElementById('notificationText');

    notificationText.textContent = message;

    notification.className = 'fixed top-4 right-4 px-6 py-3 rounded-lg shadow-lg transform transition-transform duration-300 z-50';

    switch (type) {
        case 'success':
            notification.classList.add('bg-green-500', 'text-white');
            break;
        case 'error':
            notification.classList.add('bg-red-500', 'text-white');
            break;
        case 'info':
            notification.classList.add('bg-blue-500', 'text-white');
            break;
        case 'warning':
            notification.classList.add('bg-yellow-500', 'text-white');
            break;
        default:
            notification.classList.add('bg-gray-500', 'text-white');
    }

    notification.style.transform = 'translateX(0)';

    setTimeout(() => {
        notification.style.transform = 'translateX(100%)';
    }, 3000);
}

// 图表控制函数
function resetChartData() {
    if (performanceChart) {
        performanceChart.destroy();
    }

    performanceData = {
        labels: [],
        activeTasks: [],
        completedTasks: [],
        failedTasks: [],
        completionRate: []
    };

    initializePerformanceChart();

    showNotification('图表数据已重置', 'info');
}

function changeChartType() {
    const chartType = document.getElementById('chartType').value;
    performanceChart.config.type = chartType;
    performanceChart.update();
}

function toggleChartDataset(datasetIndex, isVisible) {
    const dataset = performanceChart.data.datasets[datasetIndex];
    dataset.hidden = !isVisible;
    performanceChart.update();
}

// 清理图表资源
function cleanupChart() {
    if (performanceChart && !performanceChart.destroyed) {
        performanceChart.destroy();
        performanceChart = null;
    }
}

// 页面卸载时清理资源
window.addEventListener('beforeunload', function() {
    cleanupChart();
    stopAutoRefresh();
});

// 错误处理
window.addEventListener('error', function(event) {
    console.error('全局错误:', event.error);
    showNotification('发生未知错误，请刷新页面重试', 'error');
});

// 网络错误处理
window.addEventListener('unhandledrejection', function(event) {
    console.error('未处理的Promise拒绝:', event.reason);
    showNotification('网络请求失败，请检查网络连接', 'error');
});

console.log('404错误修复版本已加载 - 改进了任务状态检查和错误处理');