/**
 * SQL调用树可视化系统 - 前端JavaScript
 * 使用D3.js实现SQL调用树的可视化展示
 */

class SqlTreeVisualizer {
    constructor() {
        this.apiBase = '/api/sql-tree';
        this.currentData = null;
        this.svg = null;
        this.tooltip = null;
        this.width = 0;
        this.height = 0;
        this.margin = { top: 20, right: 90, bottom: 30, left: 90 };

        this.currentHighlightedNode = null; // 当前高亮的节点，避免重复高亮
        this.highlightTimeout = null; // 防抖定时器
        this.thresholdTimeout = null; // 慢SQL阈值变化防抖定时器
        
        this.init();
    }
    
    init() {
        this.setupEventListeners();
        this.setupTooltip();
        this.loadInitialData();
        
        // 定期刷新数据
        setInterval(() => {
            if (document.getElementById('traceEnabled').checked) {
                this.loadSqlTree();
            }
        }, 5000);
    }
    
    setupEventListeners() {
        // 辅助函数：安全地添加事件监听器
        const safeAddEventListener = (id, event, handler) => {
            const element = document.getElementById(id);
            if (element) {
                element.addEventListener(event, handler);
            } else {
                console.warn(`Element with id '${id}' not found`);
            }
        };
        
        // 刷新按钮
        safeAddEventListener('refreshBtn', 'click', () => {
            this.loadSqlTree();
        });
        
        // 清空按钮
        safeAddEventListener('clearBtn', 'click', () => {
            this.clearSqlTree();
        });
        
        // 导出按钮
        safeAddEventListener('exportBtn', 'click', () => {
            this.exportData();
        });
        
        // 应用配置按钮
        safeAddEventListener('applyConfigBtn', 'click', () => {
            this.applyConfiguration();
        });
        
        // 测试SQL按钮
        safeAddEventListener('testSqlBtn', 'click', () => {
            this.executeTestSql();
        });
        
        // 会话选择变化
        safeAddEventListener('sessionSelect', 'change', () => {
            this.loadSqlTree();
        });
        
        // 慢SQL阈值变化时自动应用配置
        safeAddEventListener('slowSqlThreshold', 'input', () => {
            // 防抖处理，避免频繁调用
            clearTimeout(this.thresholdTimeout);
            this.thresholdTimeout = setTimeout(() => {
                this.applyConfiguration();
            }, 500);
        });
        
        // 注意：当前使用面板模式，不是模态框模式
        // 如果需要模态框功能，需要在HTML中添加相应的模态框结构
        
        // 时间过滤按钮
        safeAddEventListener('applyTimeFilterBtn', 'click', () => {
            this.applyTimeFilter();
        });
        
        // 清除时间过滤按钮
        safeAddEventListener('clearTimeFilterBtn', 'click', () => {
            this.clearTimeFilter();
        });
        
        // 仅显示慢SQL按钮
        safeAddEventListener('showSlowSqlBtn', 'click', () => {
            this.showSlowSqlOnly();
        });
        
        // 仅显示错误SQL按钮
        safeAddEventListener('showErrorSqlBtn', 'click', () => {
            this.showErrorSqlOnly();
        });
        
        // 隐藏普通SQL按钮
        safeAddEventListener('showImportantSqlBtn', 'click', () => {
            this.showImportantSqlOnly();
        });
        
        // 深度过滤输入框
        safeAddEventListener('maxDepthFilter', 'input', () => {
            if (this.currentData) {
                this.filteredData = this.applyDepthFilter(this.currentData);
                this.updateDataInfo(this.filteredData);
                this.renderTree(this.filteredData);
            }
        });
        
        // 高级操作面板折叠功能
        safeAddEventListener('toggleAdvancedPanel', 'click', function() {
            const panel = document.getElementById('advancedPanel');
            const icon = document.getElementById('toggleIcon');
            
            if (panel && icon) {
                if (panel.classList.contains('hidden')) {
                    panel.classList.remove('hidden');
                    icon.classList.remove('fa-chevron-down');
                    icon.classList.add('fa-chevron-up');
                } else {
                    panel.classList.add('hidden');
                    icon.classList.remove('fa-chevron-up');
                    icon.classList.add('fa-chevron-down');
                }
            }
        });
        

        
        // 数据量控制切换
        safeAddEventListener('dataLimitSelect', 'change', () => {
            this.loadSqlTree();
        });
        
        // 窗口大小变化时重新绘制
        window.addEventListener('resize', () => {
            if (this.currentData) {
                this.renderTree(this.currentData);
            }
        });
        
        // 添加键盘快捷键支持
        document.addEventListener('keydown', (event) => {
            // 避免在输入框中触发快捷键
            if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA' || event.target.tagName === 'SELECT') {
                return;
            }
            
            switch(event.key.toLowerCase()) {
                case 's':
                    event.preventDefault();
                    this.showSlowSqlOnly();
                    break;
                case 'e':
                    event.preventDefault();
                    this.showErrorSqlOnly();
                    break;
                case 'i':
                    event.preventDefault();
                    this.showImportantSqlOnly();
                    break;
                case 'c':
                    event.preventDefault();
                    this.clearTimeFilter();
                    break;
            }
        });
    }
    
    setupTooltip() {
        this.tooltip = d3.select('#tooltip');
    }
    
    async loadInitialData() {
        await this.loadConfiguration();
        await this.loadThreadIds();
        await this.loadSqlTree();
        await this.loadStatistics();
    }
    
    async loadConfiguration() {
        try {
            const response = await fetch(`${this.apiBase}/config`);
            const config = await response.json();
            
            if (config.success) {
                document.getElementById('slowSqlThreshold').value = config.data.slowSqlThreshold;
                document.getElementById('traceEnabled').checked = config.data.enabled;
            }
        } catch (error) {
            console.error('加载配置失败:', error);
        }
    }
    
    async loadThreadIds() {
        try {
            const response = await fetch(`${this.apiBase}/thread-ids`);
            const result = await response.json();
            if (result.success && result.data) {
                this.updateThreadSelector(result.data);
            }
        } catch (error) {
            console.error('加载线程ID列表失败:', error);
        }
    }
    


    updateThreadSelector(threadIds) {
        const sessionSelect = document.getElementById('sessionSelect');
        const currentValue = sessionSelect.value;
        
        // 清空现有选项，只保留全部线程选项
        sessionSelect.innerHTML = `
            <option value="all">全部线程</option>
        `;
        
        // 添加具体的线程ID选项
        threadIds.forEach(threadId => {
            const option = document.createElement('option');
            option.value = threadId;
            option.textContent = threadId;
            sessionSelect.appendChild(option);
        });
        
        // 恢复之前的选择，如果该选项仍然存在
        if (Array.from(sessionSelect.options).some(option => option.value === currentValue)) {
            sessionSelect.value = currentValue;
        } else {
            sessionSelect.value = 'all'; // 默认选择全部线程
        }
    }
    
    async loadStatistics() {
        try {
            const response = await fetch(`${this.apiBase}/statistics`);
            const stats = await response.json();
            
            if (stats.success) {
                document.getElementById('totalSqlCount').textContent = stats.data.totalSqlCount || 0;
                document.getElementById('slowSqlCount').textContent = stats.data.slowSqlCount || 0;
                document.getElementById('totalExecutionTime').textContent = `${stats.data.totalExecutionTime || 0}ms`;
                document.getElementById('maxDepth').textContent = stats.data.maxDepth || 0;
            }
        } catch (error) {
            console.error('加载统计信息失败:', error);
        }
    }
    
    async loadSqlTree() {
        this.showLoading(true);
        
        try {
            const sessionType = document.getElementById('sessionSelect').value;
            const dataLimit = document.getElementById('dataLimitSelect').value;
            let endpoint;
            
            if (sessionType === 'current') {
                endpoint = '/current';
            } else if (sessionType === 'all') {
                endpoint = '/sessions';
            } else {
                // 具体的线程ID
                endpoint = `/threads/${sessionType}`;
            }
            
            // 添加数据量限制参数
            const params = new URLSearchParams();
            if (dataLimit === 'latest') {
                params.append('limit', '10');
                params.append('sort', 'latest');
            } else if (dataLimit === 'slowest') {
                params.append('limit', '10');
                params.append('sort', 'slowest');
            }
            
            const queryString = params.toString();
            const url = `${this.apiBase}${endpoint}${queryString ? '?' + queryString : ''}`;
            const response = await fetch(url);
            const result = await response.json();
            
            if (result.success && result.data) {
                this.currentData = result.data;
                
                // 处理不同的数据格式
                let treeData = result.data;
                if (sessionType === 'all' && typeof result.data === 'object' && !Array.isArray(result.data)) {
                    // 将Map<String, List<SqlNode>>转换为数组格式
                    treeData = [];
                    for (const [sessionId, nodes] of Object.entries(result.data)) {
                        if (nodes && nodes.length > 0) {
                            // 为每个会话创建一个根节点
                            const sessionRoot = {
                                nodeId: `thread-${sessionId}`,
                                sql: `线程: ${sessionId}`,
                                sqlType: 'THREAD',
                                depth: 0,
                                executionTime: nodes.reduce((sum, node) => sum + (node.executionTime || 0), 0),
                                children: nodes,
                                sessionId: sessionId,
                                slowSql: false // 线程根节点默认不是慢SQL
                            };
                            treeData.push(sessionRoot);
                        }
                    }
                }
                
                // 应用深度过滤
                this.filteredData = this.applyDepthFilter(treeData);
                this.updateDataInfo(this.filteredData);
                this.renderTree(this.filteredData);
                this.showEmptyState(false);
            } else {
                this.showEmptyState(true);
            }
            
            await this.loadStatistics();
        } catch (error) {
            console.error('加载SQL调用树失败:', error);
            this.showEmptyState(true);
        } finally {
            this.showLoading(false);
        }
    }
    
    async clearSqlTree() {
        try {
            const response = await fetch(`${this.apiBase}/clear-all`, { method: 'POST' });
            const result = await response.json();
            
            if (result.success) {
                this.currentData = null;
                this.clearVisualization();
                this.showEmptyState(true);
                await this.loadStatistics();
                this.showNotification('所有SQL调用树已清空', 'success');
                
                // 延迟1秒后自动刷新页面
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            }
        } catch (error) {
            console.error('清空SQL调用树失败:', error);
            this.showNotification('清空失败', 'error');
        }
    }
    
    async exportData() {
        try {
            const response = await fetch(`${this.apiBase}/export`);
            const result = await response.json();
            
            if (result.success) {
                const dataStr = JSON.stringify(result.data, null, 2);
                const dataBlob = new Blob([dataStr], { type: 'application/json' });
                const url = URL.createObjectURL(dataBlob);
                
                const link = document.createElement('a');
                link.href = url;
                link.download = `sql-tree-export-${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.json`;
                link.click();
                
                URL.revokeObjectURL(url);
                this.showNotification('数据导出成功', 'success');
            }
        } catch (error) {
            console.error('导出数据失败:', error);
            this.showNotification('导出失败', 'error');
        }
    }
    
    async applyConfiguration() {
        try {
            const config = {
                slowSqlThreshold: parseInt(document.getElementById('slowSqlThreshold').value),
                enabled: document.getElementById('traceEnabled').checked
            };
            
            const response = await fetch(`${this.apiBase}/config`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(config)
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.showNotification('配置已更新', 'success');
                await this.loadSqlTree();
            }
        } catch (error) {
            console.error('应用配置失败:', error);
            this.showNotification('配置更新失败', 'error');
        }
    }
    
    async executeTestSql() {
        try {
            this.showNotification('正在执行测试SQL...', 'info');
            
            // 定义多种测试场景，包括递归嵌套测试
            const testScenarios = [
                {
                    name: 'Service调用链测试(推荐)',
                    endpoints: ['/api/test/service-call-chain']
                },
                {
                    name: '递归嵌套SQL测试',
                    endpoints: ['/api/test/real-recursive']
                },
                {
                    name: '基础查询测试',
                    endpoints: ['/api/users']
                },
                {
                    name: '多层级SQL调用演示',
                    endpoints: ['/api/test/multi-level-demo']
                },
                {
                    name: '复杂订单查询(深层调用树)',
                    endpoints: ['/api/orders']
                },
                {
                    name: '慢SQL测试',
                    endpoints: ['/api/test/slow-sql']
                },
                {
                    name: '批量操作测试',
                    endpoints: ['/api/test/batch-operations']
                },
                {
                    name: '复杂查询测试(包含慢SQL)',
                    endpoints: ['/api/test/complex-query']
                },
                {
                    name: '并发查询测试',
                    endpoints: ['/api/users', '/api/orders', '/api/test/multi-level-demo'],
                    concurrent: true
                }
            ];
            
            let completedScenarios = 0;
            const totalScenarios = testScenarios.length;
            
            // 逐个执行测试场景
            for (const scenario of testScenarios) {
                this.showNotification(`执行${scenario.name}... (${completedScenarios + 1}/${totalScenarios})`, 'info');
                
                if (scenario.concurrent) {
                    // 并发执行
                    const promises = scenario.endpoints.map(endpoint => 
                        fetch(endpoint).catch(e => {
                            console.log(`并发请求 ${endpoint} 完成:`, e.message || 'success');
                        })
                    );
                    await Promise.all(promises);
                } else {
                    // 顺序执行
                    for (const endpoint of scenario.endpoints) {
                        try {
                            await fetch(endpoint);
                            console.log(`请求 ${endpoint} 完成`);
                            // 根据接口类型设置不同的延迟
                            let delay = 200; // 默认延迟
                            if (endpoint.includes('/test/slow-sql') || endpoint.includes('/test/deep-nested')) {
                                delay = 500; // 慢SQL和深层嵌套需要更长时间
                            } else if (endpoint.includes('/test/mixed-operations')) {
                                delay = 800; // 混合操作需要最长时间
                            } else if (endpoint.includes('/api/test/real-recursive')) {
                                delay = 300; // 递归嵌套测试需要适中时间
                            }
                            await new Promise(resolve => setTimeout(resolve, delay));
                        } catch (e) {
                            console.log(`请求 ${endpoint} 完成:`, e.message || 'success');
                        }
                    }
                }
                
                completedScenarios++;
                // 场景之间添加延迟
                await new Promise(resolve => setTimeout(resolve, 500));
            }
            
            // 额外的压力测试
            this.showNotification('执行压力测试...', 'info');
            const stressTestPromises = [];
            const stressTestEndpoints = ['/api/users', '/api/orders', '/api/test/multi-level-demo', '/api/test/real-recursive'];
            for (let i = 0; i < 5; i++) {
                const endpoint = stressTestEndpoints[i % stressTestEndpoints.length];
                stressTestPromises.push(
                    fetch(endpoint).catch(e => console.log(`压力测试 ${i + 1} 完成`))
                );
            }
            await Promise.all(stressTestPromises);
            
            // 等待所有SQL执行完成
            setTimeout(async () => {
                await this.loadSqlTree();
                await this.loadStatistics();
                this.showNotification(`测试SQL执行完成！共执行${totalScenarios}个测试场景，包含递归嵌套测试`, 'success');
            }, 1500);
            
        } catch (error) {
            console.error('执行测试SQL失败:', error);
            this.showNotification('测试SQL执行失败', 'error');
        }
    }
    
    renderTree(data) {
        if (!data) return;
        
        this.clearVisualization();
        
        const container = document.getElementById('sqlTreeContainer');
        const containerRect = container.getBoundingClientRect();
        
        this.width = containerRect.width - this.margin.left - this.margin.right;
        
        // 计算实际节点数量
        const root = this.convertToD3Tree(data);
        const nodeCount = root.descendants().length;
        this.height = Math.max(500, nodeCount * 60) - this.margin.top - this.margin.bottom;
        
        this.svg = d3.select('#sqlTreeContainer')
            .append('svg')
            .attr('width', this.width + this.margin.left + this.margin.right)
            .attr('height', this.height + this.margin.top + this.margin.bottom)
            .append('g')
            .attr('transform', `translate(${this.margin.left},${this.margin.top})`);
        
        // 创建树布局
        const treeLayout = d3.tree().size([this.height, this.width]);
        
        // 使用已转换的数据格式
        const treeData = treeLayout(root);
        
        // 绘制连接线
        this.svg.selectAll('.sql-tree-link')
            .data(treeData.links())
            .enter()
            .append('path')
            .attr('class', 'sql-tree-link')
            .attr('d', d3.linkHorizontal()
                .x(d => d.y)
                .y(d => d.x));
        
        // 绘制节点
        const nodes = this.svg.selectAll('.sql-tree-node')
            .data(treeData.descendants())
            .enter()
            .append('g')
            .attr('class', 'sql-tree-node')
            .attr('transform', d => `translate(${d.y},${d.x})`)
            .style('cursor', 'pointer')
            .on('mouseover', (event, d) => {
                this.showTooltip(event, d.data);
                this.highlightPath(d);
            })
            .on('mouseout', (event, d) => {
                this.hideTooltip();
                // 清除防抖定时器并重置高亮状态
                if (this.highlightTimeout) {
                    clearTimeout(this.highlightTimeout);
                    this.highlightTimeout = null;
                }
                this.currentHighlightedNode = null;
                this.clearHighlight();
            })
            .on('click', (event, d) => {
                this.showSqlDetail(d.data);
                this.highlightSelectedPath(d);
            });
        
        // 添加节点背景圆圈（用于更好的视觉效果）
        nodes.append('circle')
            .attr('r', 12)
            .attr('fill', 'white')
            .attr('stroke', d => {
                if (d.data.errorMessage) return '#dc2626';
                if (d.data.slowSql) return '#f59e0b';
                return '#10b981';
            })
            .attr('stroke-width', 2)
            .style('opacity', 0.9);
        
        // 添加内部圆圈
        nodes.append('circle')
            .attr('r', 8)
            .attr('fill', d => this.getNodeColor(d.data))
            .attr('stroke', 'white')
            .attr('stroke-width', 1);
        
        // 添加节点图标
        nodes.append('text')
            .attr('text-anchor', 'middle')
            .attr('dy', '.35em')
            .style('font-family', 'FontAwesome')
            .style('font-size', '10px')
            .style('fill', 'white')
            .text(d => {
                if (d.data.errorMessage) return '⚠';
                if (d.data.slowSql) return '⏳';
                return '✓';
            });
        
        // 添加Service名称标签（如果存在）
        nodes.append('text')
            .attr('dy', '-35px')
            .attr('x', 0)
            .style('text-anchor', 'middle')
            .style('font-size', '10px')
            .style('font-weight', 'bold')
            .style('fill', '#7c3aed')
            .text(d => {
                if (d.data.serviceName) {
                    // 只显示Service类名，不显示方法名
                    return d.data.serviceName;
                }
                return '';
            })
            .style('display', d => {
                return d.data.serviceName ? 'block' : 'none';
            });
        
        // 添加主标签（SQL类型和执行时间）
        nodes.append('text')
            .attr('dy', d => {
                // 如果有Service信息，主标签向下移动
                return d.data.serviceName ? '-18px' : '-25px';
            })
            .attr('x', 0)
            .style('text-anchor', 'middle')
            .style('font-size', '11px')
            .style('font-weight', 'bold')
            .style('fill', '#1f2937')
            .text(d => {
                const sqlType = d.data.sqlType || 'SQL';
                const executionTime = d.data.executionTime || 0;
                return `${sqlType} (${executionTime}ms)`;
            });
        
        // 添加副标签（SQL语句预览）
        nodes.append('text')
            .attr('dy', d => {
                // 如果有Service信息，副标签向下移动
                return d.data.serviceName ? '35px' : '28px';
            })
            .attr('x', 0)
            .style('text-anchor', 'middle')
            .style('font-size', '9px')
            .style('fill', '#6b7280')
            .text(d => {
                const sql = d.data.sql || 'Unknown SQL';
                return sql.length > 25 ? sql.substring(0, 25) + '...' : sql;
            });
        
        // 添加线程ID标签
        nodes.append('text')
            .attr('dy', d => {
                // 如果有Service信息，线程标签向下移动
                return d.data.serviceName ? '48px' : '40px';
            })
            .attr('x', 0)
            .style('text-anchor', 'middle')
            .style('font-size', '8px')
            .style('fill', '#9ca3af')
            .text(d => {
                const threadName = d.data.threadName || d.data.nodeId || '';
                // 显示完整的线程名，不再截断
                return threadName || 'Unknown Thread';
            });
    }
    
    convertToD3Tree(data) {
        // 如果data是数组，创建一个虚拟根节点
        if (Array.isArray(data)) {
            if (data.length === 0) {
                // 空数组，创建一个空的根节点
                const emptyRoot = {
                    nodeId: 'empty-root',
                    sql: 'No SQL calls',
                    sqlType: 'EMPTY',
                    depth: 0,
                    executionTime: 0,
                    children: []
                };
                return d3.hierarchy(emptyRoot, d => d.children);
            } else if (data.length === 1) {
                // 单个根节点，直接使用
                return d3.hierarchy(data[0], d => d.children);
            } else {
                // 多个根节点，创建虚拟根节点
                const virtualRoot = {
                    nodeId: 'virtual-root',
                    sql: 'Multiple SQL Threads',
                    sqlType: 'ROOT',
                    depth: -1,
                    executionTime: data.reduce((sum, node) => sum + (node.executionTime || 0), 0),
                    children: data
                };
                return d3.hierarchy(virtualRoot, d => d.children);
            }
        } else {
            // 单个对象，直接使用
            return d3.hierarchy(data, d => d.children);
        }
    }
    
    getNodeColor(nodeData) {
        if (nodeData.errorMessage) {
            return '#dc3545'; // 红色 - 错误
        } else if (nodeData.slowSql) {
            return '#dc3545'; // 红色 - 慢SQL
        } else {
            return '#28a745'; // 绿色 - 正常
        }
    }
    
    getNodeLabel(nodeData) {
        const sqlType = nodeData.sqlType || 'UNKNOWN';
        
        // 特殊处理线程节点
        if (sqlType === 'THREAD') {
            const threadId = nodeData.sql.replace('线程: ', '');
            return `线程-${threadId}`; // 显示完整的线程ID，不限制长度
        }
        
        // 对于非线程节点，保持原有的长度限制
        const maxLength = 20;
        if (sqlType.length > maxLength) {
            return sqlType.substring(0, maxLength) + '...';
        }
        return sqlType;
    }
    
    showTooltip(event, nodeData) {
        const tooltip = this.tooltip;
        
        let content;
        if (nodeData.sqlType === 'THREAD') {
            // 线程节点的提示信息
            const threadId = nodeData.sql.replace('线程: ', '');
            const childCount = nodeData.children ? nodeData.children.length : 0;
            content = `
                <strong>线程ID:</strong> ${threadId}<br>
                <strong>总执行时间:</strong> ${nodeData.executionTime || 0}ms<br>
                <strong>SQL调用数:</strong> ${childCount}<br>
                <em>点击查看该线程的SQL调用详情</em>
            `;
        } else {
            // 普通SQL节点的提示信息
            content = `
                <strong>SQL类型:</strong> ${nodeData.sqlType}<br>
                <strong>执行时间:</strong> ${nodeData.executionTime}ms<br>
                <strong>调用深度:</strong> ${nodeData.depth}<br>
                <strong>线程:</strong> ${nodeData.threadName}<br>
                ${nodeData.slowSql ? '<strong style="color: #dc3545;">慢SQL</strong><br>' : ''}
                ${nodeData.errorMessage ? `<strong style="color: #dc3545;">错误:</strong> ${nodeData.errorMessage}<br>` : ''}
                <em>点击查看详情</em>
            `;
        }
        
        tooltip.html(content)
            .style('left', (event.pageX + 10) + 'px')
            .style('top', (event.pageY - 10) + 'px')
            .classed('hidden', false);
    }
    
    hideTooltip() {
        this.tooltip.classed('hidden', true);
    }
    
    /**
     * 高亮调用路径（带防抖机制）
     */
    highlightPath(node) {
        // 检查是否已经高亮了相同的节点，避免重复操作
        if (this.currentHighlightedNode === node) {
            return;
        }
        
        // 清除之前的防抖定时器
        if (this.highlightTimeout) {
            clearTimeout(this.highlightTimeout);
            this.highlightTimeout = null;
        }
        
        // 使用防抖机制，延迟执行高亮
        this.highlightTimeout = setTimeout(() => {
            // 再次检查节点是否仍然有效（防止在延迟期间节点被移除）
            if (!node || !this.svg) {
                return;
            }
            
            // 清除之前的高亮
            this.clearHighlight();
            
            // 记录当前高亮的节点
            this.currentHighlightedNode = node;
            
            this.performHighlight(node);
            
            // 清除定时器引用
            this.highlightTimeout = null;
        }, 100); // 增加到100ms防抖延迟，减少频繁切换
    }
    
    /**
     * 执行实际的高亮操作
     */
    performHighlight(node) {
        
        // 获取从根节点到当前节点的路径
        const pathNodes = [];
        let current = node;
        while (current) {
            pathNodes.unshift(current);
            current = current.parent;
        }
        
        // 高亮路径上的所有节点
        this.svg.selectAll('.sql-tree-node')
            .filter(d => pathNodes.includes(d))
            .classed('highlighted-path', true);
        
        // 高亮路径上的连接线
        this.svg.selectAll('.sql-tree-link')
            .filter(d => pathNodes.includes(d.target))
            .classed('highlighted-link', true);
    }
    
    /**
     * 高亮选中节点的调用链
     */
    highlightSelectedPath(node) {
        // 清除之前的选中状态
        this.svg.selectAll('.selected-node').classed('selected-node', false);
        this.svg.selectAll('.selected-link').classed('selected-link', false);
        
        // 标记选中的节点
        this.svg.selectAll('.sql-tree-node')
            .filter(d => d === node)
            .classed('selected-node', true);
        
        // 高亮从根到选中节点的完整路径
        const pathNodes = [];
        let current = node;
        while (current) {
            pathNodes.unshift(current);
            current = current.parent;
        }
        
        // 高亮路径上的连接线
        this.svg.selectAll('.sql-tree-link')
            .filter(d => pathNodes.includes(d.target))
            .classed('selected-link', true);
    }
    
    /**
     * 清除高亮效果
     */
    clearHighlight() {
        // 清除防抖定时器
        if (this.highlightTimeout) {
            clearTimeout(this.highlightTimeout);
            this.highlightTimeout = null;
        }
        
        // 安全地清除高亮样式
        if (this.svg) {
            try {
                this.svg.selectAll('.highlighted-path').classed('highlighted-path', false);
                this.svg.selectAll('.highlighted-link').classed('highlighted-link', false);
            } catch (e) {
                console.warn('清除高亮样式时出错:', e);
            }
        }
        
        this.currentHighlightedNode = null; // 清除当前高亮节点记录
    }
    
    showSqlDetail(nodeData) {
        const panel = document.getElementById('sqlDetailPanel');
        
        // 处理线程名称显示
        const threadName = nodeData.threadName || (nodeData.sqlType === 'THREAD' ? 
            nodeData.sql.replace('线程: ', '') : '未知线程');
        
        // 增强的时间显示格式化
        const formatTime = (timestamp) => {
            if (!timestamp || timestamp === 'undefined' || isNaN(new Date(timestamp).getTime())) {
                return '<span class="text-gray-400">未知时间</span>';
            }
            const date = new Date(timestamp);
            const dateStr = date.toLocaleDateString('zh-CN');
            const timeStr = date.toLocaleTimeString('zh-CN', { hour12: false });
            const msStr = String(date.getMilliseconds()).padStart(3, '0');
            return `<span class="text-gray-800">${dateStr}</span><br><span class="text-blue-600 font-mono">${timeStr}.${msStr}</span>`;
        };
        
        // 计算执行时长的可读性显示
        const formatDuration = (ms) => {
            if (!ms || ms < 0) return '0ms';
            if (ms < 1000) return `${ms}ms`;
            if (ms < 60000) return `${(ms / 1000).toFixed(2)}s`;
            return `${(ms / 60000).toFixed(2)}min`;
        };
        
        // 获取性能等级颜色
        const getPerformanceColor = (executionTime) => {
            if (!executionTime) return 'text-gray-500';
            if (executionTime < 100) return 'text-green-600';
            if (executionTime < 500) return 'text-yellow-600';
            if (executionTime < 1000) return 'text-orange-600';
            return 'text-red-600';
        };
        
        const detailHtml = `
            <div class="space-y-4">
                <!-- 节点标题 -->
                <div class="border-b pb-3">
                    <div class="flex items-center justify-between">
                        <h4 class="font-semibold text-gray-800">节点详情 #${nodeData.nodeId || 'N/A'}</h4>
                        <span class="px-3 py-1 text-xs rounded-full font-medium ${
                            nodeData.errorMessage ? 'bg-red-100 text-red-800 border border-red-200' : 
                            nodeData.slowSql ? 'bg-yellow-100 text-yellow-800 border border-yellow-200' : 
                            'bg-green-100 text-green-800 border border-green-200'
                        }">
                            ${nodeData.errorMessage ? '❌ 执行失败' : nodeData.slowSql ? '⚠️ 慢查询' : '✅ 执行成功'}
                        </span>
                    </div>
                </div>
                
                <!-- 关键指标 -->
                <div class="grid grid-cols-2 gap-3 text-sm">
                    <div class="bg-gradient-to-br from-blue-50 to-blue-100 p-4 rounded-lg border border-blue-200">
                        <div class="text-blue-600 font-medium mb-1">⏱️ 执行时间</div>
                        <div class="text-xl font-bold ${getPerformanceColor(nodeData.executionTime)}">${formatDuration(nodeData.executionTime || 0)}</div>
                        <div class="text-xs text-blue-500 mt-1">${nodeData.executionTime || 0}ms</div>
                    </div>
                    <div class="bg-gradient-to-br from-purple-50 to-purple-100 p-4 rounded-lg border border-purple-200">
                        <div class="text-purple-600 font-medium mb-1">📊 调用深度</div>
                        <div class="text-xl font-bold text-purple-800">${nodeData.depth || 0}</div>
                        <div class="text-xs text-purple-500 mt-1">层级</div>
                    </div>
                </div>
                
                <!-- 执行信息 -->
                <div class="bg-gray-50 p-4 rounded-lg border">
                    <h5 class="font-medium text-gray-800 mb-3 flex items-center">
                        <i class="fas fa-info-circle mr-2 text-blue-500"></i>执行信息
                    </h5>
                    <div class="grid grid-cols-1 gap-3 text-sm">
                        <div class="flex justify-between items-center py-2 border-b border-gray-200">
                            <span class="text-gray-600 flex items-center">
                                <i class="fas fa-microchip mr-2 text-gray-400"></i>线程ID:
                            </span>
                            <span class="font-mono text-gray-800 bg-white px-2 py-1 rounded border">${threadName}</span>
                        </div>
                        <div class="flex justify-between items-center py-2 border-b border-gray-200">
                            <span class="text-gray-600 flex items-center">
                                <i class="fas fa-database mr-2 text-gray-400"></i>SQL类型:
                            </span>
                            <span class="font-medium px-2 py-1 rounded text-xs ${
                                nodeData.sqlType === 'SELECT' ? 'bg-blue-100 text-blue-800' :
                                nodeData.sqlType === 'INSERT' ? 'bg-green-100 text-green-800' :
                                nodeData.sqlType === 'UPDATE' ? 'bg-yellow-100 text-yellow-800' :
                                nodeData.sqlType === 'DELETE' ? 'bg-red-100 text-red-800' :
                                'bg-gray-100 text-gray-800'
                            }">${nodeData.sqlType || 'N/A'}</span>
                        </div>
                        <div class="flex justify-between items-center py-2 border-b border-gray-200">
                            <span class="text-gray-600 flex items-center">
                                <i class="fas fa-list-ol mr-2 text-gray-400"></i>影响行数:
                            </span>
                            <span class="font-bold ${nodeData.affectedRows > 0 ? 'text-green-600' : 'text-gray-500'}">${nodeData.affectedRows || 0} 行</span>
                        </div>
                        ${nodeData.serviceName ? `
                        <div class="flex justify-between items-center py-2 border-b border-gray-200">
                            <span class="text-gray-600 flex items-center">
                                <i class="fas fa-cogs mr-2 text-gray-400"></i>Service:
                            </span>
                            <span class="font-medium px-2 py-1 rounded text-xs bg-purple-100 text-purple-800">${nodeData.serviceName}</span>
                        </div>
                        ` : ''}
                        ${nodeData.methodName ? `
                        <div class="flex justify-between items-center py-2 border-b border-gray-200">
                            <span class="text-gray-600 flex items-center">
                                <i class="fas fa-function mr-2 text-gray-400"></i>方法:
                            </span>
                            <span class="font-medium px-2 py-1 rounded text-xs bg-indigo-100 text-indigo-800">${nodeData.methodName}</span>
                        </div>
                        ` : ''}
                        ${nodeData.serviceCallPath ? `
                        <div class="flex justify-between items-start py-2 border-b border-gray-200">
                            <span class="text-gray-600 flex items-center">
                                <i class="fas fa-route mr-2 text-gray-400"></i>调用路径:
                            </span>
                            <div class="text-right text-xs font-mono bg-gray-100 px-2 py-1 rounded max-w-xs">
                                <div class="break-all">${nodeData.serviceCallPath}</div>
                            </div>
                        </div>
                        ` : ''}
                        <div class="flex justify-between items-start py-2">
                            <span class="text-gray-600 flex items-center">
                                <i class="fas fa-clock mr-2 text-gray-400"></i>开始时间:
                            </span>
                            <div class="text-right text-xs">${formatTime(nodeData.startTime)}</div>
                        </div>
                    </div>
                </div>
                
                ${nodeData.errorMessage ? `
                    <!-- 错误信息 -->
                    <div class="bg-red-50 border-l-4 border-red-400 p-4 rounded-r-lg">
                        <div class="flex items-center mb-2">
                            <i class="fas fa-exclamation-triangle text-red-500 mr-2"></i>
                            <div class="text-red-800 font-medium text-sm">执行错误</div>
                        </div>
                        <div class="bg-red-100 p-3 rounded border border-red-200">
                            <div class="text-red-700 text-xs font-mono whitespace-pre-wrap leading-relaxed">${nodeData.errorMessage}</div>
                        </div>
                    </div>
                ` : ''}
                
                <!-- SQL语句 -->
                <div class="bg-white border rounded-lg p-4">
                    <div class="flex items-center justify-between mb-3">
                        <h5 class="text-gray-800 font-medium text-sm flex items-center">
                            <i class="fas fa-code mr-2 text-green-500"></i>SQL语句
                        </h5>
                        <span class="text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded">
                            ${nodeData.sql ? `${nodeData.sql.length} 字符` : '无语句'}
                        </span>
                    </div>
                    <div class="bg-gray-900 text-green-400 p-4 rounded-lg text-sm font-mono overflow-x-auto max-h-40 border-2 border-gray-700">
                        <pre class="leading-relaxed">${this.formatSql(nodeData.sql) || '<span class="text-gray-500">无SQL语句</span>'}</pre>
                    </div>
                </div>
                
                <!-- SQL参数 -->
                ${nodeData.parameters && nodeData.parameters.length > 0 ? `
                    <div class="bg-white border rounded-lg p-4">
                        <div class="flex items-center justify-between mb-3">
                            <h5 class="text-gray-800 font-medium text-sm flex items-center">
                                <i class="fas fa-list mr-2 text-blue-500"></i>SQL参数
                            </h5>
                            <span class="text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded">
                                ${nodeData.parameters.length} 个参数
                            </span>
                        </div>
                        <div class="bg-gray-900 text-blue-400 p-4 rounded-lg text-sm font-mono overflow-x-auto max-h-32 border-2 border-gray-700">
                            <pre class="leading-relaxed">${JSON.stringify(nodeData.parameters, null, 2)}</pre>
                        </div>
                    </div>
                ` : nodeData.sql && nodeData.sqlType !== 'THREAD' ? `
                    <div class="bg-gray-50 border rounded-lg p-4">
                        <div class="flex items-center mb-2">
                            <i class="fas fa-info-circle mr-2 text-gray-400"></i>
                            <span class="text-gray-600 text-sm">此SQL语句无参数</span>
                        </div>
                    </div>
                ` : ''}
                
                <!-- 子节点信息 -->
                ${nodeData.children && nodeData.children.length > 0 ? `
                    <div class="bg-white border rounded-lg p-4">
                        <div class="flex items-center justify-between mb-3">
                            <h5 class="text-gray-800 font-medium text-sm flex items-center">
                                <i class="fas fa-sitemap mr-2 text-purple-500"></i>子节点
                            </h5>
                            <span class="text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded">
                                ${nodeData.children.length} 个子节点
                            </span>
                        </div>
                        <div class="space-y-2 max-h-40 overflow-y-auto">
                            ${nodeData.children.map((child, index) => `
                                <div class="bg-gradient-to-r from-gray-50 to-gray-100 p-3 rounded-lg border-l-4 ${
                                    child.errorMessage ? 'border-red-400 bg-gradient-to-r from-red-50 to-red-100' : 
                                    child.slowSql ? 'border-yellow-400 bg-gradient-to-r from-yellow-50 to-yellow-100' : 
                                    'border-green-400'
                                } hover:shadow-sm transition-shadow cursor-pointer">
                                    <div class="flex items-center justify-between mb-2">
                                        <div class="flex items-center">
                                            <span class="text-xs font-bold text-gray-500 bg-white px-2 py-1 rounded-full mr-2">#${index + 1}</span>
                                            <span class="font-medium text-gray-800 text-sm">${child.sqlType || 'SQL'}</span>
                                            <span class="ml-2 text-xs px-2 py-1 rounded ${
                                                child.errorMessage ? 'bg-red-200 text-red-800' : 
                                                child.slowSql ? 'bg-yellow-200 text-yellow-800' : 
                                                'bg-green-200 text-green-800'
                                            }">
                                                ${child.errorMessage ? '错误' : child.slowSql ? '慢查询' : '正常'}
                                            </span>
                                        </div>
                                        <div class="text-right">
                                            <div class="text-sm font-bold ${
                                                (child.executionTime || 0) > 1000 ? 'text-red-600' :
                                                (child.executionTime || 0) > 500 ? 'text-yellow-600' :
                                                'text-green-600'
                                            }">${child.executionTime || 0}ms</div>
                                        </div>
                                    </div>
                                    <div class="text-gray-600 text-xs font-mono bg-white p-2 rounded border truncate" title="${child.sql || 'N/A'}">
                                        ${child.sql ? (child.sql.length > 60 ? child.sql.substring(0, 60) + '...' : child.sql) : 'N/A'}
                                    </div>
                                </div>
                            `).join('')}
                        </div>
                    </div>
                ` : nodeData.sqlType !== 'THREAD' ? `
                    <div class="bg-gray-50 border rounded-lg p-4">
                        <div class="flex items-center mb-2">
                            <i class="fas fa-info-circle mr-2 text-gray-400"></i>
                            <span class="text-gray-600 text-sm">此节点无子节点</span>
                        </div>
                    </div>
                ` : ''}
            </div>
        `;
        
        panel.innerHTML = detailHtml;
    }
    
    closeModal() {
        // 当前使用面板模式，不需要关闭模态框
        // 如果需要清空详情面板，可以调用 clearVisualization()
    }
    
    formatSql(sql) {
        if (!sql) return '';
        
        // 简单的SQL格式化
        return sql
            .replace(/\bSELECT\b/gi, '\nSELECT')
            .replace(/\bFROM\b/gi, '\nFROM')
            .replace(/\bWHERE\b/gi, '\nWHERE')
            .replace(/\bAND\b/gi, '\n  AND')
            .replace(/\bOR\b/gi, '\n  OR')
            .replace(/\bORDER BY\b/gi, '\nORDER BY')
            .replace(/\bGROUP BY\b/gi, '\nGROUP BY')
            .replace(/\bHAVING\b/gi, '\nHAVING')
            .replace(/\bINSERT\b/gi, '\nINSERT')
            .replace(/\bUPDATE\b/gi, '\nUPDATE')
            .replace(/\bDELETE\b/gi, '\nDELETE')
            .replace(/\bSET\b/gi, '\nSET')
            .replace(/\bVALUES\b/gi, '\nVALUES')
            .trim();
    }
    
    clearVisualization() {
        d3.select('#sqlTreeContainer').selectAll('*').remove();
    }
    
    showLoading(show) {
        const loading = document.getElementById('loadingIndicator');
        if (show) {
            loading.classList.remove('hidden');
        } else {
            loading.classList.add('hidden');
        }
    }
    
    showEmptyState(show) {
        const emptyState = document.getElementById('emptyState');
        if (show) {
            emptyState.classList.remove('hidden');
        } else {
            emptyState.classList.add('hidden');
        }
    }
    
    showNotification(message, type = 'info') {
        // 创建通知元素
        const notification = document.createElement('div');
        notification.className = `fixed top-4 right-4 px-6 py-3 rounded-lg shadow-lg z-50 fade-in ${
            type === 'success' ? 'bg-green-500 text-white' :
            type === 'error' ? 'bg-red-500 text-white' :
            type === 'warning' ? 'bg-yellow-500 text-white' :
            'bg-blue-500 text-white'
        }`;
        notification.textContent = message;
        
        document.body.appendChild(notification);
        
        // 3秒后自动移除
        setTimeout(() => {
            notification.remove();
        }, 3000);
    }
    
    /**
     * 应用时间过滤
     */
    applyTimeFilter() {
        const startTimeInput = document.getElementById('startTime');
        const endTimeInput = document.getElementById('endTime');
        
        const startTime = startTimeInput.value;
        const endTime = endTimeInput.value;
        
        if (!startTime && !endTime) {
            this.showNotification('请选择开始时间或结束时间', 'warning');
            return;
        }
        
        if (startTime && endTime && new Date(startTime) >= new Date(endTime)) {
            this.showNotification('开始时间必须早于结束时间', 'error');
            return;
        }
        
        // 过滤当前数据
        if (this.currentData) {
            const filteredData = this.filterDataByTime(this.currentData, startTime, endTime);
            this.filteredData = filteredData;
            
            // 显示筛选状态指示器
            const filterStatus = document.getElementById('filterStatus');
            if (filterStatus) {
                filterStatus.classList.remove('hidden');
                let timeRange = '';
                if (startTime && endTime) {
                    timeRange = `${startTime} 至 ${endTime}`;
                } else if (startTime) {
                    timeRange = `从 ${startTime} 开始`;
                } else if (endTime) {
                    timeRange = `到 ${endTime} 结束`;
                }
                filterStatus.innerHTML = `<i class="fas fa-filter mr-1"></i>当前筛选: 时间范围 (${timeRange})`;
            }
            
            this.renderTree(this.filteredData);
            this.showNotification('时间过滤已应用', 'success');
        } else {
            this.showNotification('没有数据可过滤', 'warning');
        }
    }
    
    /**
     * 清除所有过滤条件
     */
    clearTimeFilter() {
        // 清除时间输入框
        document.getElementById('startTime').value = '';
        document.getElementById('endTime').value = '';
        
        // 隐藏筛选状态指示器
        const filterStatus = document.getElementById('filterStatus');
        if (filterStatus) {
            filterStatus.classList.add('hidden');
        }
        
        // 重新渲染原始数据
        if (this.currentData) {
            this.filteredData = this.currentData;
            this.renderTree(this.filteredData);
            this.showNotification('已清除所有过滤条件', 'success');
        }
    }
    
    /**
     * 应用深度过滤
     * @param {Array} data - 原始数据
     * @returns {Array} 过滤后的数据
     */
    applyDepthFilter(data) {
        const maxDepthInput = document.getElementById('maxDepthFilter');
        if (!maxDepthInput || !maxDepthInput.value) {
            return data; // 没有设置深度限制，返回原始数据
        }
        
        const maxDepth = parseInt(maxDepthInput.value);
        if (isNaN(maxDepth) || maxDepth < 0) {
            return data;
        }
        
        return this.filterNodesByDepth(data, maxDepth);
    }
    
    /**
     * 递归过滤节点深度
     * @param {Array} nodes - 节点数组
     * @param {number} maxDepth - 最大深度
     * @returns {Array} 过滤后的节点
     */
    filterNodesByDepth(nodes, maxDepth) {
        if (!Array.isArray(nodes)) {
            return nodes;
        }
        
        return nodes.map(node => {
            const filteredNode = { ...node };
            
            // 如果当前节点深度超过限制，则不包含其子节点
            if (node.depth >= maxDepth) {
                delete filteredNode.children;
            } else if (node.children && Array.isArray(node.children)) {
                // 递归过滤子节点
                filteredNode.children = this.filterNodesByDepth(node.children, maxDepth);
            }
            
            return filteredNode;
        });
    }
    
    /**
     * 根据时间范围过滤数据
     */
    filterDataByTime(data, startTime, endTime) {
        if (!data) return data;
        
        const startDate = startTime ? new Date(startTime) : null;
        const endDate = endTime ? new Date(endTime) : null;
        
        const filterNode = (node) => {
            if (!node) return null;
            
            // 检查节点的开始时间是否在范围内
            let nodeInRange = true;
            if (node.startTime) {
                const nodeStartTime = new Date(node.startTime);
                if (startDate && nodeStartTime < startDate) {
                    nodeInRange = false;
                }
                if (endDate && nodeStartTime > endDate) {
                    nodeInRange = false;
                }
            }
            
            // 递归过滤子节点
            const filteredChildren = [];
            if (node.children && node.children.length > 0) {
                for (const child of node.children) {
                    const filteredChild = filterNode(child);
                    if (filteredChild) {
                        filteredChildren.push(filteredChild);
                    }
                }
            }
            
            // 如果节点本身在范围内，或者有符合条件的子节点，则保留该节点
            if (nodeInRange || filteredChildren.length > 0) {
                return {
                    ...node,
                    children: filteredChildren
                };
            }
            
            return null;
        };
        
        // 处理不同的数据格式
        if (Array.isArray(data)) {
            const filteredArray = [];
            for (const item of data) {
                const filteredItem = filterNode(item);
                if (filteredItem) {
                    filteredArray.push(filteredItem);
                }
            }
            return filteredArray;
        } else if (typeof data === 'object') {
            // 处理Map格式的数据
            const filteredData = {};
            for (const [key, nodes] of Object.entries(data)) {
                if (Array.isArray(nodes)) {
                    const filteredNodes = [];
                    for (const node of nodes) {
                        const filteredNode = filterNode(node);
                        if (filteredNode) {
                            filteredNodes.push(filteredNode);
                        }
                    }
                    if (filteredNodes.length > 0) {
                        filteredData[key] = filteredNodes;
                    }
                }
            }
            return filteredData;
        }
        
        return data;
    }
    


     
     /**
      * 更新数据信息显示
      */
     updateDataInfo(data) {
         const dataInfo = document.getElementById('dataInfo');
         if (!dataInfo) return;
         
         let totalCount = 0;
         if (Array.isArray(data)) {
             totalCount = this.countTotalNodes(data);
         } else if (typeof data === 'object' && data !== null) {
             // 处理按线程分组的数据结构
             for (const threadData of Object.values(data)) {
                 if (Array.isArray(threadData)) {
                     totalCount += this.countTotalNodes(threadData);
                 }
             }
         }
         
         dataInfo.textContent = `共 ${totalCount} 条SQL记录`;
     }
     
     /**
      * 递归计算节点总数
      */
     countTotalNodes(nodes) {
         if (!Array.isArray(nodes)) return 0;
         
         let count = 0;
         for (const node of nodes) {
             count++; // 当前节点
             if (node.children && Array.isArray(node.children)) {
                 count += this.countTotalNodes(node.children); // 递归计算子节点
             }
         }
         return count;
     }
     
     /**
      * 仅显示慢SQL记录
      */
     showSlowSqlOnly() {
         // 使用filteredData而不是currentData，因为filteredData已经经过了深度过滤等处理
         const dataToFilter = this.filteredData || this.currentData;
         if (!dataToFilter) {
             this.showNotification('暂无数据可过滤', 'warning');
             return;
         }
         
         // 直接对当前数据进行慢SQL筛选，无论是数组还是对象格式
         let filteredData;
         if (Array.isArray(dataToFilter)) {
             // 如果是数组格式（全部线程转换后的数据），直接筛选
             filteredData = this.filterSlowSqlNodes(dataToFilter);
         } else {
             // 如果是对象格式（原始Map数据），使用原有逻辑
             filteredData = this.filterSlowSqlData(dataToFilter);
         }
         
         if (this.isDataEmpty(filteredData)) {
             this.showNotification('未找到慢SQL记录', 'warning');
             return;
         }
         
         // 显示筛选状态指示器
         const filterStatus = document.getElementById('filterStatus');
         if (filterStatus) {
             filterStatus.classList.remove('hidden');
             filterStatus.innerHTML = '<i class="fas fa-filter mr-1"></i>当前筛选: 慢SQL';
         }
         
         // 更新过滤后的数据并重新分页
         this.filteredData = filteredData;
         this.renderTree(this.filteredData);
         
         this.showNotification('已过滤显示慢SQL记录', 'success');
      }
     
     /**
      * 过滤慢SQL数据
      */
     filterSlowSqlData(data) {
         if (Array.isArray(data)) {
             return this.filterSlowSqlNodes(data);
         } else if (typeof data === 'object' && data !== null) {
             const filteredData = {};
             for (const [threadId, threadData] of Object.entries(data)) {
                 if (Array.isArray(threadData)) {
                     const filteredNodes = this.filterSlowSqlNodes(threadData);
                     if (filteredNodes.length > 0) {
                         filteredData[threadId] = filteredNodes;
                     }
                 }
             }
             return filteredData;
         }
         return data;
     }
     
     /**
      * 递归过滤慢SQL节点
      */
     filterSlowSqlNodes(nodes) {
         if (!Array.isArray(nodes)) return [];
         
         const result = [];
         const slowSqlThreshold = parseInt(document.getElementById('slowSqlThreshold').value) || 1;
         
         for (const node of nodes) {
             const filteredNode = { ...node };
             
             // 递归过滤子节点
             if (node.children && Array.isArray(node.children)) {
                 filteredNode.children = this.filterSlowSqlNodes(node.children);
             }
             
             // 判断是否为慢SQL：优先使用后端标记，否则根据执行时间判断
             const isSlowSql = node.slowSql || (node.executionTime && node.executionTime > slowSqlThreshold);
             
             // 线程根节点（THREAD类型）如果有子节点就保留，其他节点需要是慢SQL或有慢SQL子节点
             const isThreadRoot = node.sqlType === 'THREAD';
             const shouldKeep = isSlowSql || (filteredNode.children && filteredNode.children.length > 0) || 
                               (isThreadRoot && node.children && node.children.length > 0);
             
             if (shouldKeep) {
                 result.push(filteredNode);
             }
         }
         
         return result;
     }
     
     /**
      * 仅显示错误SQL记录
      */
     showErrorSqlOnly() {
         // 使用filteredData而不是currentData，因为filteredData已经经过了深度过滤等处理
         const dataToFilter = this.filteredData || this.currentData;
         if (!dataToFilter) {
             this.showNotification('暂无数据可过滤', 'warning');
             return;
         }
         
         // 直接对当前数据进行错误SQL筛选，无论是数组还是对象格式
         let filteredData;
         if (Array.isArray(dataToFilter)) {
             // 如果是数组格式（全部线程转换后的数据），直接筛选
             filteredData = this.filterErrorSqlNodes(dataToFilter);
         } else {
             // 如果是对象格式（原始Map数据），使用原有逻辑
             filteredData = this.filterErrorSqlData(dataToFilter);
         }
         
         if (this.isDataEmpty(filteredData)) {
             this.showNotification('未找到错误SQL记录', 'warning');
             return;
         }
         
         // 显示筛选状态指示器
         const filterStatus = document.getElementById('filterStatus');
         if (filterStatus) {
             filterStatus.classList.remove('hidden');
             filterStatus.innerHTML = '<i class="fas fa-filter mr-1"></i>当前筛选: 错误SQL';
         }
         
         // 更新过滤后的数据并重新渲染
         this.filteredData = filteredData;
         this.renderTree(this.filteredData);
         
         this.showNotification('已过滤显示错误SQL记录', 'success');
      }
     
     /**
      * 过滤错误SQL数据
      */
     filterErrorSqlData(data) {
         if (Array.isArray(data)) {
             return this.filterErrorSqlNodes(data);
         } else if (typeof data === 'object' && data !== null) {
             const filteredData = {};
             for (const [threadId, threadData] of Object.entries(data)) {
                 if (Array.isArray(threadData)) {
                     const filteredNodes = this.filterErrorSqlNodes(threadData);
                     if (filteredNodes.length > 0) {
                         filteredData[threadId] = filteredNodes;
                     }
                 }
             }
             return filteredData;
         }
         return data;
     }
     
     /**
      * 递归过滤错误SQL节点
      */
     filterErrorSqlNodes(nodes) {
         if (!Array.isArray(nodes)) return [];
         
         const result = [];
         for (const node of nodes) {
             const filteredNode = { ...node };
             
             // 递归过滤子节点
             if (node.children && Array.isArray(node.children)) {
                 filteredNode.children = this.filterErrorSqlNodes(node.children);
             }
             
             // 如果当前节点有错误信息，或者有错误SQL子节点，则保留
             if (node.errorMessage || (filteredNode.children && filteredNode.children.length > 0)) {
                 result.push(filteredNode);
             }
         }
         
         return result;
     }
     
     /**
      * 仅显示慢SQL和错误SQL，隐藏普通SQL
      */
     showImportantSqlOnly() {
         // 使用filteredData而不是currentData，因为filteredData已经经过了深度过滤等处理
         const dataToFilter = this.filteredData || this.currentData;
         if (!dataToFilter) {
             this.showNotification('暂无数据可过滤', 'warning');
             return;
         }
         
         // 直接对当前数据进行重要SQL筛选，无论是数组还是对象格式
         let filteredData;
         if (Array.isArray(dataToFilter)) {
             // 如果是数组格式（全部线程转换后的数据），直接筛选
             filteredData = this.filterImportantSqlNodes(dataToFilter);
         } else {
             // 如果是对象格式（原始Map数据），使用原有逻辑
             filteredData = this.filterImportantSqlData(dataToFilter);
         }
         
         if (this.isDataEmpty(filteredData)) {
             this.showNotification('未找到慢SQL或错误SQL记录', 'warning');
             return;
         }
         
         // 显示筛选状态指示器
         const filterStatus = document.getElementById('filterStatus');
         if (filterStatus) {
             filterStatus.classList.remove('hidden');
             filterStatus.innerHTML = '<i class="fas fa-filter mr-1"></i>当前筛选: 慢SQL和错误SQL';
         }
         
         // 更新过滤后的数据并重新渲染
         this.filteredData = filteredData;
         this.renderTree(this.filteredData);
         
         this.showNotification('已过滤显示慢SQL和错误SQL记录', 'success');
     }
     
     /**
      * 过滤慢SQL和错误SQL数据
      */
     filterImportantSqlData(data) {
         if (Array.isArray(data)) {
             return this.filterImportantSqlNodes(data);
         } else if (typeof data === 'object' && data !== null) {
             const filteredData = {};
             for (const [threadId, threadData] of Object.entries(data)) {
                 if (Array.isArray(threadData)) {
                     const filteredNodes = this.filterImportantSqlNodes(threadData);
                     if (filteredNodes.length > 0) {
                         filteredData[threadId] = filteredNodes;
                     }
                 }
             }
             return filteredData;
         }
         return data;
     }
     
     /**
      * 递归过滤重要SQL节点（慢SQL和错误SQL）
      */
     filterImportantSqlNodes(nodes) {
         if (!Array.isArray(nodes)) return [];
         
         const result = [];
         const slowSqlThreshold = parseInt(document.getElementById('slowSqlThreshold').value) || 1;
         
         for (const node of nodes) {
             const filteredNode = { ...node };
             
             // 递归过滤子节点
             if (node.children && Array.isArray(node.children)) {
                 filteredNode.children = this.filterImportantSqlNodes(node.children);
             }
             
             // 判断是否为慢SQL：优先使用后端标记，否则根据执行时间判断
             const isSlowSql = node.slowSql || (node.executionTime && node.executionTime > slowSqlThreshold);
             const hasError = node.errorMessage;
             
             // 线程根节点（THREAD类型）如果有子节点就保留，其他节点需要是慢SQL、错误SQL或有重要SQL子节点
             const isThreadRoot = node.sqlType === 'THREAD';
             const shouldKeep = isSlowSql || hasError || (filteredNode.children && filteredNode.children.length > 0) ||
                               (isThreadRoot && node.children && node.children.length > 0);
             
             if (shouldKeep) {
                 result.push(filteredNode);
             }
         }
         
         return result;
     }
     
     /**
      * 检查数据是否为空
      */
     isDataEmpty(data) {
         if (!data) return true;
         
         if (Array.isArray(data)) {
             return data.length === 0;
         } else if (typeof data === 'object') {
             return Object.keys(data).length === 0;
         }
         
         return true;
     }
}

// 初始化应用
document.addEventListener('DOMContentLoaded', () => {
    new SqlTreeVisualizer();
});