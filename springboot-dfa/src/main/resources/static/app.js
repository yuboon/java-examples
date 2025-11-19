// API 基础 URL
const API_BASE_URL = '/api/sensitive-word';

// 全局变量
let currentResult = null;

/**
 * 显示/隐藏加载提示
 */
function showLoading(show = true) {
    const overlay = document.getElementById('loadingOverlay');
    if (show) {
        overlay.classList.remove('hidden');
    } else {
        overlay.classList.add('hidden');
    }
}

/**
 * 显示提示消息
 */
function showToast(title, message, type = 'success') {
    const toast = document.getElementById('toast');
    const toastIcon = document.getElementById('toastIcon');
    const toastTitle = document.getElementById('toastTitle');
    const toastMessage = document.getElementById('toastMessage');

    // 设置图标和颜色
    toastIcon.className = 'fas text-2xl';
    if (type === 'success') {
        toastIcon.classList.add('fa-check-circle', 'text-green-500');
    } else if (type === 'error') {
        toastIcon.classList.add('fa-exclamation-circle', 'text-red-500');
    } else if (type === 'warning') {
        toastIcon.classList.add('fa-exclamation-triangle', 'text-yellow-500');
    } else {
        toastIcon.classList.add('fa-info-circle', 'text-blue-500');
    }

    toastTitle.textContent = title;
    toastMessage.textContent = message;

    toast.classList.remove('hidden');

    // 3秒后自动隐藏
    setTimeout(() => {
        toast.classList.add('hidden');
    }, 3000);
}

/**
 * 检查敏感词
 */
async function checkSensitiveWord() {
    const text = document.getElementById('textInput').value.trim();

    if (!text) {
        showToast('输入错误', '请输入要检测的文本', 'warning');
        return;
    }

    showLoading(true);

    try {
        const response = await fetch(`${API_BASE_URL}/check?text=${encodeURIComponent(text)}`);
        const result = await response.json();

        if (result.success) {
            displayCheckResult(result);
            showToast('检测完成',
                result.hasSensitive ? '发现敏感词' : '未发现敏感词',
                result.hasSensitive ? 'warning' : 'success'
            );
        } else {
            showToast('检测失败', result.error, 'error');
        }
    } catch (error) {
        console.error('检查敏感词失败:', error);
        showToast('网络错误', '请检查网络连接后重试', 'error');
    } finally {
        showLoading(false);
    }
}

/**
 * 过滤文本
 */
async function filterText() {
    const text = document.getElementById('textInput').value.trim();
    const replacement = document.getElementById('replacement').value || '*';

    if (!text) {
        showToast('输入错误', '请输入要过滤的文本', 'warning');
        return;
    }

    showLoading(true);

    try {
        const response = await fetch(
            `${API_BASE_URL}/filter?text=${encodeURIComponent(text)}&replacement=${encodeURIComponent(replacement)}`
        );
        const result = await response.json();

        if (result.success) {
            currentResult = result;
            displayFilterResult(result);
            showToast('过滤完成', `发现 ${result.sensitiveWordCount} 个敏感词`,
                result.hasSensitive ? 'warning' : 'success');
        } else {
            showToast('过滤失败', result.error, 'error');
        }
    } catch (error) {
        console.error('过滤文本失败:', error);
        showToast('网络错误', '请检查网络连接后重试', 'error');
    } finally {
        showLoading(false);
    }
}

/**
 * 查找所有敏感词
 */
async function findAllSensitiveWords() {
    const text = document.getElementById('textInput').value.trim();

    if (!text) {
        showToast('输入错误', '请输入要检测的文本', 'warning');
        return;
    }

    showLoading(true);

    try {
        const response = await fetch(`${API_BASE_URL}/find-all?text=${encodeURIComponent(text)}`);
        const result = await response.json();

        if (result.success) {
            displayAllSensitiveWords(result);
            showToast('查找完成', `发现 ${result.count} 个敏感词`,
                result.count > 0 ? 'warning' : 'success');
        } else {
            showToast('查找失败', result.error, 'error');
        }
    } catch (error) {
        console.error('查找敏感词失败:', error);
        showToast('网络错误', '请检查网络连接后重试', 'error');
    } finally {
        showLoading(false);
    }
}

/**
 * 显示检查结果
 */
function displayCheckResult(result) {
    const resultArea = document.getElementById('resultArea');
    const resultStats = document.getElementById('resultStats');
    const textComparison = document.getElementById('textComparison');
    const sensitiveWordsList = document.getElementById('sensitiveWordsList');

    resultArea.classList.remove('hidden');

    // 显示统计信息
    resultStats.innerHTML = `
        <div class="text-center">
            <div class="text-3xl font-bold ${result.hasSensitive ? 'text-red-600' : 'text-green-600'}">
                ${result.hasSensitive ? '是' : '否'}
            </div>
            <div class="text-sm text-gray-600 mt-1">是否包含敏感词</div>
        </div>
        <div class="text-center">
            <div class="text-3xl font-bold text-blue-600">
                ${result.text ? result.text.length : 0}
            </div>
            <div class="text-sm text-gray-600 mt-1">文本长度</div>
        </div>
        <div class="text-center">
            <div class="text-3xl font-bold text-purple-600">
                ${result.text ? result.text.split(' ').length : 0}
            </div>
            <div class="text-sm text-gray-600 mt-1">词语数量</div>
        </div>
        <div class="text-center">
            <div class="text-3xl font-bold text-gray-600">
                1
            </div>
            <div class="text-sm text-gray-600 mt-1">检测次数</div>
        </div>
    `;

    // 显示文本对比
    textComparison.innerHTML = `
        <div>
            <h4 class="text-md font-semibold mb-2 text-gray-800">原始文本：</h4>
            <div class="p-4 bg-gray-50 border rounded-lg">
                <pre class="whitespace-pre-wrap text-sm text-gray-700">${escapeHtml(result.text || '')}</pre>
            </div>
        </div>
    `;

    sensitiveWordsList.classList.add('hidden');
}

/**
 * 显示过滤结果
 */
function displayFilterResult(result) {
    const resultArea = document.getElementById('resultArea');
    const resultStats = document.getElementById('resultStats');
    const textComparison = document.getElementById('textComparison');
    const sensitiveWordsList = document.getElementById('sensitiveWordsList');

    resultArea.classList.remove('hidden');

    // 显示统计信息
    resultStats.innerHTML = `
        <div class="text-center">
            <div class="text-3xl font-bold ${result.hasSensitive ? 'text-red-600' : 'text-green-600'}">
                ${result.sensitiveWordCount}
            </div>
            <div class="text-sm text-gray-600 mt-1">敏感词数量</div>
        </div>
        <div class="text-center">
            <div class="text-3xl font-bold text-blue-600">
                ${result.originalText ? result.originalText.length : 0}
            </div>
            <div class="text-sm text-gray-600 mt-1">原始长度</div>
        </div>
        <div class="text-center">
            <div class="text-3xl font-bold text-green-600">
                ${result.filteredText ? result.filteredText.length : 0}
            </div>
            <div class="text-sm text-gray-600 mt-1">过滤后长度</div>
        </div>
        <div class="text-center">
            <div class="text-3xl font-bold text-purple-600">
                ${result.replacement || '*'}
            </div>
            <div class="text-sm text-gray-600 mt-1">替换字符</div>
        </div>
    `;

    // 显示文本对比
    textComparison.innerHTML = `
        <div>
            <h4 class="text-md font-semibold mb-2 text-gray-800">原始文本：</h4>
            <div class="p-4 bg-red-50 border border-red-200 rounded-lg">
                <pre class="whitespace-pre-wrap text-sm text-gray-700">${escapeHtml(result.originalText || '')}</pre>
            </div>
        </div>
        <div>
            <h4 class="text-md font-semibold mb-2 text-gray-800">过滤后文本：</h4>
            <div class="p-4 bg-green-50 border border-green-200 rounded-lg">
                <pre class="whitespace-pre-wrap text-sm text-gray-700">${escapeHtml(result.filteredText || '')}</pre>
            </div>
        </div>
    `;

    // 显示敏感词列表
    if (result.sensitiveWords && result.sensitiveWords.length > 0) {
        sensitiveWordsList.classList.remove('hidden');
        const sensitiveWordsContainer = document.getElementById('sensitiveWordsContainer');

        let html = '<div class="space-y-2">';
        result.sensitiveWords.forEach((word, index) => {
            html += `
                <div class="flex items-center justify-between bg-white p-3 rounded border border-red-300">
                    <div class="flex items-center space-x-3">
                        <span class="text-red-600 font-semibold">${index + 1}.</span>
                        <span class="font-mono font-semibold text-red-700">${escapeHtml(word.word)}</span>
                        <span class="text-sm text-gray-500">位置: ${word.start}-${word.end}</span>
                    </div>
                    <span class="text-xs bg-red-100 text-red-800 px-2 py-1 rounded">敏感词</span>
                </div>
            `;
        });
        html += '</div>';

        sensitiveWordsContainer.innerHTML = html;
    } else {
        sensitiveWordsList.classList.add('hidden');
    }
}

/**
 * 显示所有敏感词
 */
function displayAllSensitiveWords(result) {
    const resultArea = document.getElementById('resultArea');
    const resultStats = document.getElementById('resultStats');
    const textComparison = document.getElementById('textComparison');
    const sensitiveWordsList = document.getElementById('sensitiveWordsList');

    resultArea.classList.remove('hidden');

    // 显示统计信息
    resultStats.innerHTML = `
        <div class="text-center">
            <div class="text-3xl font-bold ${result.count > 0 ? 'text-red-600' : 'text-green-600'}">
                ${result.count}
            </div>
            <div class="text-sm text-gray-600 mt-1">敏感词数量</div>
        </div>
        <div class="text-center">
            <div class="text-3xl font-bold text-blue-600">
                ${result.text ? result.text.length : 0}
            </div>
            <div class="text-sm text-gray-600 mt-1">文本长度</div>
        </div>
        <div class="text-center">
            <div class="text-3xl font-bold text-purple-600">
                ${result.count > 0 ? Math.round((result.count / (result.text.length / 10)) * 100) : 0}%
            </div>
            <div class="text-sm text-gray-600 mt-1">敏感词密度</div>
        </div>
        <div class="text-center">
            <div class="text-3xl font-bold text-gray-600">
                1
            </div>
            <div class="text-sm text-gray-600 mt-1">检测次数</div>
        </div>
    `;

    // 显示原始文本
    textComparison.innerHTML = `
        <div class="md:col-span-2">
            <h4 class="text-md font-semibold mb-2 text-gray-800">原始文本（高亮敏感词）：</h4>
            <div class="p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
                <pre class="whitespace-pre-wrap text-sm text-gray-700">${highlightSensitiveWords(result.text || '', result.sensitiveWords || [])}</pre>
            </div>
        </div>
    `;

    // 显示敏感词列表
    if (result.sensitiveWords && result.sensitiveWords.length > 0) {
        sensitiveWordsList.classList.remove('hidden');
        const sensitiveWordsContainer = document.getElementById('sensitiveWordsContainer');

        let html = '<div class="space-y-2">';
        result.sensitiveWords.forEach((word, index) => {
            html += `
                <div class="flex items-center justify-between bg-white p-3 rounded border border-red-300">
                    <div class="flex items-center space-x-3">
                        <span class="text-red-600 font-semibold">${index + 1}.</span>
                        <span class="font-mono font-semibold text-red-700">${escapeHtml(word.word)}</span>
                        <span class="text-sm text-gray-500">位置: ${word.start}-${word.end}</span>
                        <span class="text-sm text-gray-500">长度: ${word.word.length}</span>
                    </div>
                    <span class="text-xs bg-red-100 text-red-800 px-2 py-1 rounded">敏感词</span>
                </div>
            `;
        });
        html += '</div>';

        sensitiveWordsContainer.innerHTML = html;
    } else {
        sensitiveWordsList.classList.add('hidden');
    }
}

/**
 * 高亮敏感词
 */
function highlightSensitiveWords(text, sensitiveWords) {
    if (!sensitiveWords || sensitiveWords.length === 0) {
        return escapeHtml(text);
    }

    let result = escapeHtml(text);

    // 按位置排序敏感词（从后往前处理，避免位置偏移）
    const sortedWords = [...sensitiveWords].sort((a, b) => b.start - a.start);

    sortedWords.forEach(word => {
        const before = result.substring(0, word.start);
        const highlighted = `<span class="highlight">${escapeHtml(word.word)}</span>`;
        const after = result.substring(word.end + 1);
        result = before + highlighted + after;
    });

    return result;
}

/**
 * 清空结果
 */
function clearResults() {
    document.getElementById('resultArea').classList.add('hidden');
    document.getElementById('textInput').value = '';
    currentResult = null;
    showToast('清空完成', '所有结果已清空', 'info');
}

/**
 * 设置替换字符
 */
function setReplacement(char) {
    document.getElementById('replacement').value = char;
}

/**
 * 添加敏感词
 */
async function addSensitiveWord() {
    const word = document.getElementById('newSensitiveWord').value.trim();

    if (!word) {
        showToast('输入错误', '请输入敏感词', 'warning');
        return;
    }

    showLoading(true);

    try {
        const response = await fetch(`${API_BASE_URL}/add`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ word: word })
        });

        const result = await response.json();

        if (result.success) {
            document.getElementById('newSensitiveWord').value = '';
            showToast('添加成功', `敏感词 "${word}" 已添加到词库`, 'success');
        } else {
            showToast('添加失败', result.error, 'error');
        }
    } catch (error) {
        console.error('添加敏感词失败:', error);
        showToast('网络错误', '请检查网络连接后重试', 'error');
    } finally {
        showLoading(false);
    }
}

/**
 * 批量添加敏感词
 */
async function addBatchSensitiveWords() {
    const batchText = document.getElementById('batchSensitiveWords').value.trim();

    if (!batchText) {
        showToast('输入错误', '请输入敏感词列表', 'warning');
        return;
    }

    const words = batchText.split('\n')
        .map(word => word.trim())
        .filter(word => word.length > 0);

    if (words.length === 0) {
        showToast('输入错误', '没有有效的敏感词', 'warning');
        return;
    }

    showLoading(true);

    try {
        const response = await fetch(`${API_BASE_URL}/add-batch`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ words: words })
        });

        const result = await response.json();

        if (result.success) {
            document.getElementById('batchSensitiveWords').value = '';
            showToast('批量添加成功', `成功添加 ${result.count} 个敏感词`, 'success');
        } else {
            showToast('批量添加失败', result.error, 'error');
        }
    } catch (error) {
        console.error('批量添加敏感词失败:', error);
        showToast('网络错误', '请检查网络连接后重试', 'error');
    } finally {
        showLoading(false);
    }
}

/**
 * 检查系统状态
 */
async function checkSystemStatus() {
    showLoading(true);

    try {
        const response = await fetch(`${API_BASE_URL}/status`);
        const result = await response.json();

        if (result.success) {
            displaySystemStatus(result);
            document.getElementById('systemStatus').scrollIntoView({ behavior: 'smooth' });
            showToast('状态查询成功', '系统运行正常', 'success');
        } else {
            showToast('状态查询失败', result.error, 'error');
        }
    } catch (error) {
        console.error('检查系统状态失败:', error);
        showToast('网络错误', '请检查网络连接后重试', 'error');
    } finally {
        showLoading(false);
    }
}

/**
 * 显示系统状态
 */
function displaySystemStatus(status) {
    const systemStatusContent = document.getElementById('systemStatusContent');
    const systemStatus = document.getElementById('systemStatus');

    systemStatus.classList.remove('hidden');

    let featuresHtml = '';
    if (status.features && status.features.length > 0) {
        featuresHtml = '<div class="mt-4"><h4 class="text-md font-semibold mb-2 text-gray-800">功能特性：</h4><ul class="list-disc list-inside space-y-1 text-gray-600">';
        status.features.forEach(feature => {
            featuresHtml += `<li>${escapeHtml(feature)}</li>`;
        });
        featuresHtml += '</ul></div>';
    }

    systemStatusContent.innerHTML = `
        <div class="grid md:grid-cols-2 gap-6">
            <div>
                <h3 class="text-lg font-semibold mb-3 text-gray-800">基本信息</h3>
                <div class="space-y-2">
                    <div class="flex justify-between">
                        <span class="text-gray-600">系统状态：</span>
                        <span class="font-semibold text-green-600">${status.status || 'Unknown'}</span>
                    </div>
                    <div class="flex justify-between">
                        <span class="text-gray-600">算法：</span>
                        <span class="font-semibold">${status.algorithm || 'Unknown'}</span>
                    </div>
                    <div class="flex justify-between">
                        <span class="text-gray-600">数据结构：</span>
                        <span class="font-semibold">${status.dataStructure || 'Unknown'}</span>
                    </div>
                </div>
            </div>
            <div>
                <h3 class="text-lg font-semibold mb-3 text-gray-800">性能指标</h3>
                <div class="space-y-2">
                    <div class="flex justify-between">
                        <span class="text-gray-600">时间复杂度：</span>
                        <span class="font-semibold text-blue-600">O(n)</span>
                    </div>
                    <div class="flex justify-between">
                        <span class="text-gray-600">空间效率：</span>
                        <span class="font-semibold text-green-600">前缀共享</span>
                    </div>
                    <div class="flex justify-between">
                        <span class="text-gray-600">响应时间：</span>
                        <span class="font-semibold text-purple-600">毫秒级</span>
                    </div>
                </div>
            </div>
        </div>
        ${featuresHtml}
    `;
}

/**
 * HTML 转义
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * 页面加载完成后的初始化
 */
document.addEventListener('DOMContentLoaded', function() {
    // 绑定回车键事件
    document.getElementById('textInput').addEventListener('keypress', function(e) {
        if (e.key === 'Enter' && e.ctrlKey) {
            filterText();
        }
    });

    document.getElementById('newSensitiveWord').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            addSensitiveWord();
        }
    });

    // 初始化示例文本
    const sampleText = '这是一个包含app内容的测试文本，还有orange和apple等敏感词汇。';
    document.getElementById('textInput').placeholder = sampleText;

    // 自动检查系统健康状态
    fetch(`${API_BASE_URL}/health`)
        .then(response => response.json())
        .then(result => {
            if (result.status === 'UP') {
                console.log('系统健康检查通过');
            }
        })
        .catch(error => {
            console.warn('系统健康检查失败:', error);
        });
});