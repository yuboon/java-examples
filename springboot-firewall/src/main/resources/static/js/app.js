class FirewallApp {
    constructor() {
        this.apiBase = '/api/firewall';
        this.currentPage = 'dashboard';
        this.rules = [];
        this.editingRuleId = null;
        this.charts = {};
        this.refreshInterval = null;
        this.logs = [];
        this.currentLogsPage = 1;
        this.logsPerPage = 10;
        this.filteredLogs = [];
        this.blacklist = [];
        this.whitelist = [];
        this.editingBlacklistId = null;
        this.editingWhitelistId = null;
        this.init().catch(error => {
            console.error('初始化失败:', error);
        });
    }

    async init() {
        this.bindEvents();
        await this.loadDashboardData();
        this.initCharts();
        await this.loadRules();
        await this.loadBlacklist();
        await this.loadWhitelist();
        await this.showPage('dashboard');
    }

    bindEvents() {
        // 导航事件
        document.querySelectorAll('.nav-btn, .mobile-nav-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const page = e.currentTarget.dataset.page;
                this.showPage(page).catch(error => {
                    console.error('页面切换失败:', error);
                });
            });
        });

        // 刷新按钮
        const refreshBtn = document.querySelector('[title="刷新"]');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => {
                this.refreshCurrentPage();
            });
        }

        // 添加规则按钮
        const addRuleBtn = document.getElementById('add-rule-btn');
        if (addRuleBtn) {
            addRuleBtn.addEventListener('click', () => {
                this.showRuleModal();
            });
        }

        // 添加黑名单按钮
        const addBlacklistBtn = document.getElementById('add-blacklist-btn');
        if (addBlacklistBtn) {
            addBlacklistBtn.addEventListener('click', () => {
                this.showBlacklistModal();
            });
        }

        // 添加白名单按钮
        const addWhitelistBtn = document.getElementById('add-whitelist-btn');
        if (addWhitelistBtn) {
            addWhitelistBtn.addEventListener('click', () => {
                this.showWhitelistModal();
            });
        }

        // 模态框事件
        this.bindModalEvents();
        this.bindBlacklistModalEvents();
        this.bindWhitelistModalEvents();

        // 表单提交
        const ruleForm = document.getElementById('rule-form');
        if (ruleForm) {
            ruleForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.saveRule().catch(error => {
                    console.error('保存规则失败:', error);
                });
            });
        }

        // 趋势图表周期选择
        const trendPeriod = document.getElementById('trend-period');
        if (trendPeriod) {
            trendPeriod.addEventListener('change', () => {
                this.updateTrendChart().catch(error => {
                    console.error('更新趋势图表失败:', error);
                });
            });
        }

        // 访问日志搜索
        const searchLogsBtn = document.getElementById('search-logs-btn');
        if (searchLogsBtn) {
            searchLogsBtn.addEventListener('click', () => {
                this.searchLogs();
            });
        }

        // 回车键搜索
        const ipFilter = document.getElementById('ip-filter');
        if (ipFilter) {
            ipFilter.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.searchLogs();
                }
            });
        }
    }

    async showPage(pageId) {
        // 隐藏所有页面
        document.querySelectorAll('.page-content').forEach(page => {
            page.classList.add('hidden');
        });

        // 显示目标页面
        const targetPage = document.getElementById(`${pageId}-page`);
        if (targetPage) {
            targetPage.classList.remove('hidden');
        }

        // 更新导航状态
        document.querySelectorAll('.nav-btn, .mobile-nav-btn').forEach(btn => {
            btn.classList.remove('active');
            if (btn.dataset.page === pageId) {
                btn.classList.add('active');
            }
        });

        this.currentPage = pageId;

        // 根据页面加载相应数据
        switch (pageId) {
            case 'dashboard':
                await this.loadDashboardData();
                break;
            case 'rules':
                await this.loadRules();
                break;
            case 'blacklist':
                await this.loadBlacklist();
                break;
            case 'whitelist':
                await this.loadWhitelist();
                break;
            case 'logs':
                await this.loadLogs();
                break;
        }
    }

    refreshCurrentPage() {
        this.showMessage('正在刷新...', 'info');
        this.showPage(this.currentPage).catch(error => {
            console.error('页面刷新失败:', error);
        });
    }

    // API调用方法
    async apiCall(endpoint, method = 'GET', data = null) {
        const url = this.apiBase + endpoint;
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json'
            }
        };

        if (data) {
            options.body = JSON.stringify(data);
        }

        const response = await fetch(url, options);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        return await response.json();
    }

    // 启动自动刷新
    startAutoRefresh() {
        // 每30秒刷新一次仪表盘数据
        this.refreshInterval = setInterval(() => {
            if (this.currentPage === 'dashboard') {
                this.loadDashboardData();
            }
        }, 30000);
    }

    // 停止自动刷新
    stopAutoRefresh() {
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
            this.refreshInterval = null;
        }
    }

    async loadDashboardData() {
        try {
            // 加载概览数据
            const overviewData = await this.apiCall('/overview');
            this.updateOverviewCards(overviewData);

            // 加载热门API
            const topApis = await this.apiCall('/top-apis?limit=5');
            this.updateTopAPIs(topApis);

            // 加载活跃IP
            const topIps = await this.apiCall('/top-ips?limit=5');
            this.updateTopIPs(topIps);

            // 更新趋势图表
            await this.updateTrendChart();
        } catch (error) {
            console.error('加载仪表盘数据失败:', error);
            this.showMessage('加载数据失败，请稍后重试', 'error');
            // 降级到模拟数据
            this.loadMockDashboardData();
        }
    }

    loadMockDashboardData() {
        this.updateOverviewCards({
            totalRequests: 15420,
            blockedRequests: 1250,
            avgResponseTime: 125,
            blockRate: 8.1
        });
        this.updateTopAPIs([
            { path: '/api/users', count: 2340, change: '+12%' },
            { path: '/api/login', count: 1890, change: '+8%' },
            { path: '/api/data', count: 1560, change: '-3%' },
            { path: '/api/upload', count: 980, change: '+15%' },
            { path: '/api/search', count: 750, change: '+5%' }
        ]);
        this.updateTopIPs([
            { ip: '192.168.1.100', count: 450, status: 'normal' },
            { ip: '10.0.0.50', count: 320, status: 'blocked' },
            { ip: '172.16.0.25', count: 280, status: 'normal' },
            { ip: '203.0.113.10', count: 150, status: 'suspicious' },
            { ip: '198.51.100.5', count: 120, status: 'normal' }
        ]);
        // 异步更新趋势图表，不等待结果
        this.updateTrendChart().catch(error => {
            console.error('更新趋势图表失败:', error);
        });
    }

    updateOverviewCards(data) {
        // 处理后端返回的数据格式
        const stats = data.todayStats || data;
        const yesterdayStats = data.yesterdayStats || {};
        
        document.getElementById('total-requests').textContent = (stats.totalRequests || 0).toLocaleString();
        document.getElementById('blocked-requests').textContent = (stats.blockedRequests || 0).toLocaleString();
        document.getElementById('avg-response-time').textContent = `${(stats.avgResponseTime || 0).toFixed(1)}ms`;
        
        // 计算拦截率
        const blockRate = stats.totalRequests > 0 
            ? ((stats.blockedRequests / stats.totalRequests) * 100).toFixed(2)
            : '0.00';
        document.getElementById('block-rate').textContent = `${blockRate}%`;
        
        // 计算并显示较昨日变化
        this.updateChangeIndicator('total-requests-change', stats.totalRequests || 0, yesterdayStats.totalRequests || 0);
        this.updateChangeIndicator('blocked-requests-change', stats.blockedRequests || 0, yesterdayStats.blockedRequests || 0);
        this.updateChangeIndicator('avg-response-time-change', stats.avgResponseTime || 0, yesterdayStats.avgResponseTime || 0, true);
        
        const todayBlockRate = stats.totalRequests > 0 ? (stats.blockedRequests / stats.totalRequests) * 100 : 0;
        const yesterdayBlockRate = yesterdayStats.totalRequests > 0 ? (yesterdayStats.blockedRequests / yesterdayStats.totalRequests) * 100 : 0;
        this.updateChangeIndicator('block-rate-change', todayBlockRate, yesterdayBlockRate);
    }
    
    updateChangeIndicator(elementId, todayValue, yesterdayValue, isReverse = false) {
        const element = document.getElementById(elementId);
        if (!element) return;
        
        if (yesterdayValue === 0) {
            element.textContent = todayValue > 0 ? '+100%' : '0%';
            element.className = 'font-medium text-gray-500';
            return;
        }
        
        const change = ((todayValue - yesterdayValue) / yesterdayValue) * 100;
        const changeText = change >= 0 ? `+${change.toFixed(1)}%` : `${change.toFixed(1)}%`;
        
        element.textContent = changeText;
        
        // 对于响应时间，降低是好的（绿色），增加是坏的（红色）
        // 对于其他指标，增加通常是好的（绿色），减少是坏的（红色）
        if (isReverse) {
            if (change > 0) {
                element.className = 'font-medium text-red-600';
            } else if (change < 0) {
                element.className = 'font-medium text-green-600';
            } else {
                element.className = 'font-medium text-gray-500';
            }
        } else {
            if (change > 0) {
                element.className = 'font-medium text-green-600';
            } else if (change < 0) {
                element.className = 'font-medium text-red-600';
            } else {
                element.className = 'font-medium text-gray-500';
            }
        }
    }

    updateTopAPIs(apis) {
        const container = document.getElementById('top-apis');
        if (!container) return;

        if (!apis || apis.length === 0) {
            container.innerHTML = '<div class="text-center text-gray-500 py-4">暂无数据</div>';
            return;
        }

        container.innerHTML = apis.map(api => {
            const path = api.API_PATH || api.apiPath || api.path || '未知路径';
            const count = api.COUNT || api.count || 0;
            const change = api.change || '+0%';
            
            return `
                <div class="flex items-center justify-between py-2">
                    <div class="flex-1">
                        <div class="text-sm font-medium text-gray-900">${path}</div>
                        <div class="text-xs text-gray-500">${count} 次请求</div>
                    </div>
                    <div class="text-xs font-medium ${
                        change.startsWith('+') ? 'text-green-600' : 'text-red-600'
                    }">
                        ${change}
                    </div>
                </div>
            `;
        }).join('');
    }

    updateTopIPs(ips) {
        const container = document.getElementById('top-ips');
        if (!container) return;

        if (!ips || ips.length === 0) {
            container.innerHTML = '<div class="text-center text-gray-500 py-4">暂无数据</div>';
            return;
        }

        const statusColors = {
            normal: 'bg-green-100 text-green-800',
            blocked: 'bg-red-100 text-red-800',
            suspicious: 'bg-yellow-100 text-yellow-800'
        };

        const statusTexts = {
            normal: '正常',
            blocked: '已拦截',
            suspicious: '可疑'
        };

        container.innerHTML = ips.map(ip => {
            const address = ip.IP_ADDRESS || ip.ipAddress || ip.ip || '未知IP';
            const count = ip.COUNT || ip.count || 0;
            const status = ip.status || 'normal';
            
            return `
                <div class="flex items-center justify-between py-2">
                    <div class="flex-1">
                        <div class="text-sm font-medium text-gray-900">${address}</div>
                        <div class="text-xs text-gray-500">${count} 次访问</div>
                    </div>
                    <span class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                        statusColors[status]
                    }">
                        ${statusTexts[status]}
                    </span>
                </div>
            `;
        }).join('');
    }

    initCharts() {
        // 初始化趋势图表
        const trendCtx = document.getElementById('trend-chart');
        if (trendCtx) {
            this.charts.trend = new Chart(trendCtx, {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [{
                        label: '总请求',
                        data: [],
                        borderColor: '#3b82f6',
                        backgroundColor: 'rgba(59, 130, 246, 0.1)',
                        tension: 0.4
                    }, {
                        label: '拦截请求',
                        data: [],
                        borderColor: '#ef4444',
                        backgroundColor: 'rgba(239, 68, 68, 0.1)',
                        tension: 0.4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'top'
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true
                        }
                    }
                }
            });
        }

        // 初始化请求类型分布图表
        const requestTypeCtx = document.getElementById('request-type-chart');
        if (requestTypeCtx) {
            this.charts.requestType = new Chart(requestTypeCtx, {
                type: 'doughnut',
                data: {
                    labels: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'],
                    datasets: [{
                        data: [45, 25, 15, 10, 5],
                        backgroundColor: [
                            '#3b82f6',
                            '#10b981',
                            '#f59e0b',
                            '#ef4444',
                            '#8b5cf6'
                        ]
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'bottom'
                        }
                    }
                }
            });
        }

        // 初始化攻击类型统计图表
        const attackTypeCtx = document.getElementById('attack-type-chart');
        if (attackTypeCtx) {
            this.charts.attackType = new Chart(attackTypeCtx, {
                type: 'bar',
                data: {
                    labels: ['SQL注入', 'XSS', 'CSRF', '暴力破解', '扫描探测'],
                    datasets: [{
                        label: '攻击次数',
                        data: [120, 85, 45, 230, 180],
                        backgroundColor: '#ef4444'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: false
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true
                        }
                    }
                }
            });
        }
    }

    async updateTrendChart() {
        if (!this.charts.trend) return;

        const period = document.getElementById('trend-period')?.value || '24h';
        
        try {
            // 根据选择的时间段确定天数
            let days;
            switch (period) {
                case '24h':
                    days = 1;
                    break;
                case '7d':
                    days = 7;
                    break;
                case '30d':
                    days = 30;
                    break;
                default:
                    days = 7;
            }
            
            // 调用趋势API获取真实数据
            const trendData = await this.apiCall(`/trend?days=${days}`);
            
            if (trendData && trendData.length > 0) {
                const labels = [];
                const totalData = [];
                const blockedData = [];
                
                trendData.forEach(data => {
                    labels.push(data.date);
                    totalData.push(data.totalRequests || 0);
                    blockedData.push(data.blockedRequests || 0);
                });
                
                this.charts.trend.data.labels = labels;
                this.charts.trend.data.datasets[0].data = totalData;
                this.charts.trend.data.datasets[1].data = blockedData;
            } else {
                // 如果没有数据，使用空数据
                this.charts.trend.data.labels = [];
                this.charts.trend.data.datasets[0].data = [];
                this.charts.trend.data.datasets[1].data = [];
            }
            
        } catch (error) {
            console.error('加载趋势图数据失败:', error);
            // 降级到模拟数据
            this.loadMockTrendData(period);
        }
        
        this.charts.trend.update();
    }
    
    loadMockTrendData(period) {
        let labels, totalData, blockedData;
        
        switch (period) {
            case '24h':
                labels = Array.from({length: 24}, (_, i) => `${i}:00`);
                totalData = Array.from({length: 24}, () => Math.floor(Math.random() * 100) + 50);
                blockedData = Array.from({length: 24}, () => Math.floor(Math.random() * 20) + 5);
                break;
            case '7d':
                labels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
                totalData = [1200, 1350, 1100, 1400, 1600, 900, 800];
                blockedData = [120, 135, 110, 140, 160, 90, 80];
                break;
            case '30d':
                labels = Array.from({length: 30}, (_, i) => `${i + 1}日`);
                totalData = Array.from({length: 30}, () => Math.floor(Math.random() * 2000) + 800);
                blockedData = Array.from({length: 30}, () => Math.floor(Math.random() * 200) + 50);
                break;
        }
        
        this.charts.trend.data.labels = labels;
        this.charts.trend.data.datasets[0].data = totalData;
        this.charts.trend.data.datasets[1].data = blockedData;
    }

    async loadRules() {
        try {
            const rules = await this.apiCall('/rules');
            this.rules = rules;
            this.updateRulesTable();
        } catch (error) {
            console.error('加载规则失败:', error);
            this.showMessage('加载规则失败，请稍后重试', 'error');
            // 降级到模拟数据
            this.loadMockRules();
        }
    }

    loadMockRules() {
        // 模拟规则数据
        const mockRules = [
            {
                id: 1,
                name: 'IP黑名单',
                type: 'IP',
                pattern: '192.168.1.100',
                action: 'BLOCK',
                priority: 1,
                enabled: true,
                createTime: '2024-01-15 10:30:00'
            },
            {
                id: 2,
                name: 'SQL注入防护',
                type: 'URL',
                pattern: '.*select.*from.*',
                action: 'BLOCK',
                priority: 2,
                enabled: true,
                createTime: '2024-01-15 09:15:00'
            },
            {
                id: 3,
                name: 'API访问限制',
                type: 'URL',
                pattern: '/api/admin/.*',
                action: 'ALLOW',
                priority: 3,
                enabled: false,
                createTime: '2024-01-14 16:45:00'
            },
            {
                id: 4,
                name: 'User-Agent过滤',
                type: 'HEADER',
                pattern: 'bot|crawler|spider',
                action: 'BLOCK',
                priority: 4,
                enabled: true,
                createTime: '2024-01-14 14:20:00'
            }
        ];
        
        this.rules = mockRules;
        this.updateRulesTable();
    }

    getRuleTypeDisplay(rule) {
        // 根据规则的特征判断类型
        if (rule.apiPattern) {
            if (rule.apiPattern.includes('*')) {
                return 'URL模式';
            } else {
                return 'API路径';
            }
        }
        return '通用规则';
    }

    updateRulesTable() {
        const tbody = document.querySelector('#rules-table tbody');
        if (!tbody) return;

        // 检查规则数据是否存在
        if (!this.rules || this.rules.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-gray-500">暂无规则数据</td></tr>';
            return;
        }

        tbody.innerHTML = this.rules.map(rule => `
            <tr>
                <td class="px-6 py-4 text-sm text-gray-900">${rule.ruleName || '-'}</td>
                <td class="px-6 py-4 text-sm text-gray-900">${this.getRuleTypeDisplay(rule)}</td>
                <td class="px-6 py-4 text-sm text-gray-900 font-mono">${rule.apiPattern || '-'}</td>
                <td class="px-6 py-4 text-sm text-gray-900">
                    <div class="space-y-1">
                        <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                            QPS: ${rule.qpsLimit || 0}
                        </span>
                        <br>
                        <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                            用户: ${rule.userLimit || 0}
                        </span>
                    </div>
                </td>
                <td class="px-6 py-4 text-sm text-gray-900">
                    <label class="relative inline-flex items-center cursor-pointer">
                        <input type="checkbox" class="sr-only peer" ${rule.enabled ? 'checked' : ''} 
                               data-rule-id="${rule.id}">
                        <div class="w-9 h-5 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-blue-600"></div>
                    </label>
                </td>
                <td class="px-6 py-4 text-sm text-gray-900">
                    <div class="flex space-x-2">
                        <button data-action="edit" data-rule-id="${rule.id}" 
                                class="text-blue-600 hover:text-blue-800 transition-colors">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button data-action="delete" data-rule-id="${rule.id}" 
                                class="text-red-600 hover:text-red-800 transition-colors">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
        
        // 绑定事件监听器
        tbody.querySelectorAll('input[type="checkbox"]').forEach(checkbox => {
            checkbox.addEventListener('change', (e) => {
                const ruleId = parseInt(e.target.dataset.ruleId);
                this.toggleRule(ruleId).catch(error => {
                    console.error('切换规则状态失败:', error);
                });
            });
        });
        
        tbody.querySelectorAll('button[data-action]').forEach(button => {
            button.addEventListener('click', (e) => {
                const action = e.currentTarget.dataset.action;
                const ruleId = parseInt(e.currentTarget.dataset.ruleId);
                
                if (action === 'edit') {
                    this.editRule(ruleId);
                } else if (action === 'delete') {
                    this.deleteRule(ruleId).catch(error => {
                        console.error('删除规则失败:', error);
                    });
                }
            });
        });
    }

    async loadLogs() {
        try {
            const logs = await this.apiCall('/logs?limit=50');
            this.updateLogsTable(logs);
        } catch (error) {
            console.error('加载日志失败:', error);
            this.showMessage('加载日志失败，请稍后重试', 'error');
            // 显示空数据状态
            this.updateLogsTable([]);
        }
    }



    async searchLogs() {
        const timeRange = document.getElementById('time-range-filter').value;
        const status = document.getElementById('status-filter').value;
        const ipFilter = document.getElementById('ip-filter').value.trim();

        try {
            // 构建查询参数
            const params = new URLSearchParams();
            
            // 处理时间范围
            if (timeRange && timeRange !== 'all') {
                const now = new Date();
                let startTime;
                switch (timeRange) {
                    case '1h':
                        startTime = new Date(now.getTime() - 60 * 60 * 1000);
                        break;
                    case '24h':
                        startTime = new Date(now.getTime() - 24 * 60 * 60 * 1000);
                        break;
                    case '7d':
                        startTime = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
                        break;
                    case '30d':
                        startTime = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
                        break;
                }
                if (startTime) {
                    // 格式化为后端期望的格式：yyyy-MM-ddTHH:mm:ss
                    const formatDateTime = (date) => {
                        const year = date.getFullYear();
                        const month = String(date.getMonth() + 1).padStart(2, '0');
                        const day = String(date.getDate()).padStart(2, '0');
                        const hours = String(date.getHours()).padStart(2, '0');
                        const minutes = String(date.getMinutes()).padStart(2, '0');
                        const seconds = String(date.getSeconds()).padStart(2, '0');
                        return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
                    };
                    
                    params.append('startTime', formatDateTime(startTime));
                    params.append('endTime', formatDateTime(now));
                }
            }
            
            // IP地址过滤
            if (ipFilter) {
                params.append('ipAddress', ipFilter);
            }
            
            params.append('limit', '100');

            // 调用API搜索
            const apiUrl = `/logs?${params.toString()}`;
            console.log('搜索日志API调用:', apiUrl);
            const logs = await this.apiCall(apiUrl);
            console.log('API返回的日志数据:', logs);
            
            // 如果有状态过滤，在前端进行过滤
            let filteredLogs = logs;
            if (status && status !== 'all') {
                if (status === 'blocked') {
                    filteredLogs = logs.filter(log => log.blockReason);
                } else if (status === 'success') {
                    filteredLogs = logs.filter(log => !log.blockReason && log.statusCode < 400);
                } else if (status === 'error') {
                    filteredLogs = logs.filter(log => !log.blockReason && log.statusCode >= 400);
                }
            }

            this.currentLogsPage = 1; // 重置到第一页
            this.updateLogsTable(filteredLogs);
            this.showMessage(`找到 ${filteredLogs.length} 条匹配的日志记录`, 'success');
            
            // 如果没有找到数据，显示调试信息
            if (filteredLogs.length === 0) {
                console.log('搜索参数:', {
                    timeRange,
                    status,
                    ipFilter,
                    apiUrl
                });
            }
        } catch (error) {
            console.error('搜索日志失败:', error);
            this.showMessage('搜索日志失败，请稍后重试', 'error');
            // 显示空结果
            this.updateLogsTable([]);
        }
    }

    updateLogsTable(logs) {
        const tbody = document.getElementById('logs-table-body');
        if (!tbody) return;

        this.filteredLogs = logs || [];
        
        if (this.filteredLogs.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-gray-500">暂无日志数据</td></tr>';
            this.updateLogsPagination();
            return;
        }

        // 计算分页
        const startIndex = (this.currentLogsPage - 1) * this.logsPerPage;
        const endIndex = startIndex + this.logsPerPage;
        const pageData = this.filteredLogs.slice(startIndex, endIndex);

        tbody.innerHTML = pageData.map(log => {
            // 格式化时间显示
            const formatTime = (timeStr) => {
                if (!timeStr) return '-';
                try {
                    const date = new Date(timeStr);
                    return date.toLocaleString('zh-CN', {
                        year: 'numeric',
                        month: '2-digit',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit',
                        second: '2-digit'
                    });
                } catch (e) {
                    return timeStr;
                }
            };
            
            // 格式化响应时间显示
            const formatResponseTime = (time) => {
                if (time === null || time === undefined) return '-';
                return time + 'ms';
            };
            
            return `
                <tr>
                    <td class="px-6 py-4 text-sm text-gray-900">${formatTime(log.requestTime)}</td>
                    <td class="px-6 py-4 text-sm text-gray-900">${log.ipAddress || '-'}</td>
                    <td class="px-6 py-4 text-sm text-gray-900">${log.requestMethod || '-'}</td>
                    <td class="px-6 py-4 text-sm text-gray-900">${log.apiPath || '-'}</td>
                    <td class="px-6 py-4 text-sm text-gray-900">${log.statusCode || '-'}</td>
                    <td class="px-6 py-4 text-sm text-gray-900">${formatResponseTime(log.responseTime)}</td>
                    <td class="px-6 py-4 text-sm text-gray-900">
                        <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            log.blockReason ? 'bg-red-100 text-red-800' :
                            log.statusCode >= 400 ? 'bg-yellow-100 text-yellow-800' :
                            'bg-green-100 text-green-800'
                        }">
                            ${log.blockReason ? '被拦截' : log.statusCode >= 400 ? '异常' : '正常'}
                        </span>
                    </td>
                </tr>
            `;
        }).join('');
        
        this.updateLogsPagination();
    }

    showRuleModal(rule = null) {
        const modal = document.getElementById('rule-modal');
        const title = document.getElementById('modal-title');
        const form = document.getElementById('rule-form');

        if (rule) {
            title.textContent = '编辑规则';
            this.editingRuleId = rule.id;
            form.elements.ruleName.value = rule.ruleName || '';
            form.elements.apiPattern.value = rule.apiPattern || '';
            form.elements.qpsLimit.value = rule.qpsLimit || 100;
            form.elements.userLimit.value = rule.userLimit || 60;
            form.elements.description.value = rule.description || '';
        } else {
            title.textContent = '添加规则';
            this.editingRuleId = null;
            form.reset();
        }

        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }

    hideRuleModal() {
        const modal = document.getElementById('rule-modal');
        modal.classList.add('hidden');
        modal.classList.remove('flex');
        this.editingRuleId = null;
    }

    bindModalEvents() {
        // 关闭模态框
        const closeButtons = document.querySelectorAll('.modal-close');
        if (closeButtons) {
            closeButtons.forEach(btn => {
                btn.addEventListener('click', () => {
                    this.hideRuleModal();
                });
            });
        }

        // 取消按钮
        const cancelBtn = document.getElementById('cancel-btn');
        if (cancelBtn) {
            cancelBtn.addEventListener('click', () => {
                this.hideRuleModal();
            });
        }

        // 保存按钮
        const saveBtn = document.getElementById('save-btn');
        if (saveBtn) {
            saveBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.saveRule();
            });
        }

        // 点击背景关闭
        const modal = document.getElementById('rule-modal');
        if (modal) {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    this.hideRuleModal();
                }
            });
        }
    }

    async saveRule() {
        const form = document.getElementById('rule-form');
        const formData = new FormData(form);
        const ruleData = {
            ruleName: formData.get('ruleName'),
            apiPattern: formData.get('apiPattern'),
            qpsLimit: parseInt(formData.get('qpsLimit')) || 100,
            userLimit: parseInt(formData.get('userLimit')) || 60,
            timeWindow: 60, // 默认时间窗口为60秒
            enabled: true,
            description: formData.get('description') || ''
        };

        // 验证数据
        if (!ruleData.ruleName || !ruleData.apiPattern) {
            this.showMessage('请填写完整的规则信息', 'error');
            return;
        }

        // 调用后端API
        try {
            if (this.editingRuleId) {
                // 编辑规则
                ruleData.id = this.editingRuleId;
                const updatedRule = await this.apiCall(`/rules/${this.editingRuleId}`, 'PUT', ruleData);
                // 重新加载规则列表
                await this.loadRules();
                this.showMessage('规则更新成功', 'success');
            } else {
                // 添加新规则
                const newRule = await this.apiCall('/rules', 'POST', ruleData);
                // 重新加载规则列表
                await this.loadRules();
                this.showMessage('规则添加成功', 'success');
            }

            this.hideRuleModal();
        } catch (error) {
            console.error('保存规则失败:', error);
            this.showMessage('保存规则失败，请检查输入信息', 'error');
        }
    }

    editRule(id) {
        const rule = this.rules.find(r => r.id === id);
        if (rule) {
            this.showRuleModal(rule);
        }
    }

    async deleteRule(id) {
        if (!confirm('确定要删除这条规则吗？')) {
            return;
        }

        try {
            await this.apiCall(`/rules/${id}`, 'DELETE');
            // 重新加载规则列表
            await this.loadRules();
            this.showMessage('规则删除成功', 'success');
        } catch (error) {
            console.error('删除规则失败:', error);
            this.showMessage('删除规则失败', 'error');
        }
    }

    async toggleRule(id) {
        const rule = this.rules.find(r => r.id === id);
        if (!rule) return;

        try {
            const newStatus = !rule.enabled;
            await this.apiCall(`/rules/${id}/toggle`, 'PUT', { enabled: newStatus });
            // 重新加载规则列表
            await this.loadRules();
            this.showMessage(`规则已${newStatus ? '启用' : '禁用'}`, 'info');
        } catch (error) {
            console.error('切换规则状态失败:', error);
            this.showMessage('操作失败', 'error');
            // 恢复checkbox状态
            const checkbox = document.querySelector(`input[onchange="app.toggleRule(${id})"]`);
            if (checkbox) {
                checkbox.checked = rule.enabled;
            }
        }
    }

    showMessage(message, type = 'info') {
        // 创建消息提示
        const messageDiv = document.createElement('div');
        const bgColor = {
            success: 'bg-green-500',
            error: 'bg-red-500',
            warning: 'bg-yellow-500',
            info: 'bg-blue-500'
        }[type] || 'bg-blue-500';

        messageDiv.className = `fixed top-4 right-4 ${bgColor} text-white px-6 py-3 rounded-lg shadow-lg z-50 transform transition-all duration-300 translate-x-full`;
        messageDiv.innerHTML = `
            <div class="flex items-center space-x-2">
                <i class="fas fa-${type === 'success' ? 'check' : type === 'error' ? 'times' : 'info'}-circle"></i>
                <span>${message}</span>
            </div>
        `;

        document.body.appendChild(messageDiv);

        // 显示动画
        setTimeout(() => {
            messageDiv.classList.remove('translate-x-full');
        }, 100);

        // 自动隐藏
        setTimeout(() => {
            messageDiv.classList.add('translate-x-full');
            setTimeout(() => {
                document.body.removeChild(messageDiv);
            }, 300);
        }, 3000);
    }

    updateLogsPagination() {
        const paginationContainer = document.getElementById('logs-pagination');
        if (!paginationContainer) return;

        const totalPages = Math.ceil(this.filteredLogs.length / this.logsPerPage);
        
        if (totalPages <= 1) {
            paginationContainer.innerHTML = '';
            return;
        }

        let paginationHTML = `
            <div class="flex items-center justify-between px-4 py-3 bg-white border-t border-gray-200 sm:px-6">
                <div class="flex justify-between flex-1 sm:hidden">
                    <button ${this.currentLogsPage === 1 ? 'disabled' : ''} 
                            onclick="app.goToLogsPage(${this.currentLogsPage - 1})"
                            class="relative inline-flex items-center px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 ${this.currentLogsPage === 1 ? 'opacity-50 cursor-not-allowed' : ''}">
                        上一页
                    </button>
                    <button ${this.currentLogsPage === totalPages ? 'disabled' : ''}
                            onclick="app.goToLogsPage(${this.currentLogsPage + 1})"
                            class="relative ml-3 inline-flex items-center px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 ${this.currentLogsPage === totalPages ? 'opacity-50 cursor-not-allowed' : ''}">
                        下一页
                    </button>
                </div>
                <div class="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
                    <div>
                        <p class="text-sm text-gray-700">
                            显示第 <span class="font-medium">${(this.currentLogsPage - 1) * this.logsPerPage + 1}</span> 到 
                            <span class="font-medium">${Math.min(this.currentLogsPage * this.logsPerPage, this.filteredLogs.length)}</span> 条，
                            共 <span class="font-medium">${this.filteredLogs.length}</span> 条记录
                        </p>
                    </div>
                    <div>
                        <nav class="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
        `;

        // 上一页按钮
        paginationHTML += `
            <button ${this.currentLogsPage === 1 ? 'disabled' : ''}
                    onclick="app.goToLogsPage(${this.currentLogsPage - 1})"
                    class="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 ${this.currentLogsPage === 1 ? 'opacity-50 cursor-not-allowed' : ''}">
                <i class="fas fa-chevron-left"></i>
            </button>
        `;

        // 页码按钮
        const startPage = Math.max(1, this.currentLogsPage - 2);
        const endPage = Math.min(totalPages, this.currentLogsPage + 2);

        if (startPage > 1) {
            paginationHTML += `
                <button onclick="app.goToLogsPage(1)"
                        class="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50">
                    1
                </button>
            `;
            if (startPage > 2) {
                paginationHTML += `
                    <span class="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700">
                        ...
                    </span>
                `;
            }
        }

        for (let i = startPage; i <= endPage; i++) {
            paginationHTML += `
                <button onclick="app.goToLogsPage(${i})"
                        class="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium ${
                            i === this.currentLogsPage 
                                ? 'z-10 bg-blue-50 border-blue-500 text-blue-600' 
                                : 'bg-white text-gray-700 hover:bg-gray-50'
                        }">
                    ${i}
                </button>
            `;
        }

        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                paginationHTML += `
                    <span class="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700">
                        ...
                    </span>
                `;
            }
            paginationHTML += `
                <button onclick="app.goToLogsPage(${totalPages})"
                        class="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50">
                    ${totalPages}
                </button>
            `;
        }

        // 下一页按钮
        paginationHTML += `
            <button ${this.currentLogsPage === totalPages ? 'disabled' : ''}
                    onclick="app.goToLogsPage(${this.currentLogsPage + 1})"
                    class="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 ${this.currentLogsPage === totalPages ? 'opacity-50 cursor-not-allowed' : ''}">
                <i class="fas fa-chevron-right"></i>
            </button>
        `;

        paginationHTML += `
                        </nav>
                    </div>
                </div>
            </div>
        `;

        paginationContainer.innerHTML = paginationHTML;
    }

    goToLogsPage(page) {
        const totalPages = Math.ceil(this.filteredLogs.length / this.logsPerPage);
        if (page < 1 || page > totalPages) return;
        
        this.currentLogsPage = page;
        this.updateLogsTable(this.filteredLogs);
    }

    changeLogsPerPage(perPage) {
        this.logsPerPage = parseInt(perPage);
        this.currentLogsPage = 1;
        this.updateLogsTable(this.filteredLogs);
    }

    // 黑名单管理方法
    async loadBlacklist() {
        try {
            const response = await this.apiCall('/blacklist');
            this.blacklist = response;
            this.updateBlacklistTable();
        } catch (error) {
            console.error('加载黑名单失败:', error);
            this.showMessage('加载黑名单失败', 'error');
        }
    }

    updateBlacklistTable() {
        const tbody = document.querySelector('#blacklist-table-body');
        if (!tbody) return;

        tbody.innerHTML = '';
        this.blacklist.forEach(item => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td class="px-6 py-4 text-sm text-gray-900">${item.ipAddress}</td>
                <td class="px-6 py-4 text-sm text-gray-900">${item.reason || '-'}</td>
                <td class="px-6 py-4 text-sm text-gray-900">${item.expireTime ? (isNaN(new Date(item.expireTime)) ? '无效日期' : new Date(item.expireTime).toLocaleString()) : '永久'}</td>
                <td class="px-6 py-4 text-sm text-gray-900">${item.createdTime ? (isNaN(new Date(item.createdTime)) ? '无效日期' : new Date(item.createdTime).toLocaleString()) : '-'}</td>
                <td class="px-6 py-4 text-sm">
                    <button class="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded text-xs mr-2" onclick="app.editBlacklist(${item.id})">
                        编辑
                    </button>
                    <button class="bg-red-600 hover:bg-red-700 text-white px-3 py-1 rounded text-xs" onclick="app.deleteBlacklist(${item.id})">
                        删除
                    </button>
                </td>
            `;
            tbody.appendChild(row);
        });
    }

    showBlacklistModal(item = null) {
        this.editingBlacklistId = item ? item.id : null;
        const modal = document.getElementById('blacklist-modal');
        const form = document.getElementById('blacklist-form');
        
        if (item) {
            form.elements.ipAddress.value = item.ipAddress;
            form.elements.reason.value = item.reason || '';
            // 修复时区问题：保持本地时间，不进行UTC转换
            if (item.expireTime) {
                const date = new Date(item.expireTime);
                // 获取本地时间的年月日时分，避免时区转换
                const year = date.getFullYear();
                const month = String(date.getMonth() + 1).padStart(2, '0');
                const day = String(date.getDate()).padStart(2, '0');
                const hours = String(date.getHours()).padStart(2, '0');
                const minutes = String(date.getMinutes()).padStart(2, '0');
                form.elements.expireTime.value = `${year}-${month}-${day}T${hours}:${minutes}`;
            } else {
                form.elements.expireTime.value = '';
            }
        } else {
            form.reset();
        }
        
        modal.style.display = 'flex';
    }

    hideBlacklistModal() {
        document.getElementById('blacklist-modal').style.display = 'none';
        this.editingBlacklistId = null;
    }

    bindBlacklistModalEvents() {
        const modal = document.getElementById('blacklist-modal');
        if (!modal) return;
        
        const closeBtn = modal.querySelector('.blacklist-modal-close');
        const cancelBtn = document.getElementById('blacklist-cancel-btn');
        const saveBtn = document.getElementById('blacklist-save-btn');
        
        if (closeBtn) closeBtn.addEventListener('click', () => this.hideBlacklistModal());
        if (cancelBtn) cancelBtn.addEventListener('click', () => this.hideBlacklistModal());
        if (saveBtn) saveBtn.addEventListener('click', () => this.saveBlacklist());
        
        window.addEventListener('click', (event) => {
            if (event.target === modal) {
                this.hideBlacklistModal();
            }
        });
    }

    async saveBlacklist() {
        const form = document.getElementById('blacklist-form');
        let expireTime = form.elements.expireTime.value;
        if (expireTime) {
            // 将datetime-local格式(yyyy-MM-ddTHH:mm)转换为后端期望的格式(yyyy-MM-dd HH:mm:ss)
            expireTime = expireTime.replace('T', ' ') + ':00';
        }
        const data = {
            ipAddress: form.elements.ipAddress.value,
            reason: form.elements.reason.value,
            expireTime: expireTime || null
        };

        try {
            if (this.editingBlacklistId) {
                await this.apiCall(`/blacklist/${this.editingBlacklistId}`, 'PUT', data);
                this.showMessage('黑名单更新成功', 'success');
            } else {
                await this.apiCall('/blacklist', 'POST', data);
                this.showMessage('黑名单添加成功', 'success');
            }
            
            this.hideBlacklistModal();
            await this.loadBlacklist();
        } catch (error) {
            console.error('保存黑名单失败:', error);
            this.showMessage('保存黑名单失败', 'error');
        }
    }

    editBlacklist(id) {
        const item = this.blacklist.find(b => b.id === id);
        if (item) {
            this.showBlacklistModal(item);
        }
    }

    async deleteBlacklist(id) {
        if (!confirm('确定要删除这个黑名单项吗？')) {
            return;
        }

        try {
            await this.apiCall(`/blacklist/${id}`, 'DELETE');
            this.showMessage('黑名单删除成功', 'success');
            await this.loadBlacklist();
        } catch (error) {
            console.error('删除黑名单失败:', error);
            this.showMessage('删除黑名单失败', 'error');
        }
    }

    // 白名单管理方法
    async loadWhitelist() {
        try {
            const response = await this.apiCall('/whitelist');
            this.whitelist = response;
            this.updateWhitelistTable();
        } catch (error) {
            console.error('加载白名单失败:', error);
            this.showMessage('加载白名单失败', 'error');
        }
    }

    updateWhitelistTable() {
        const tbody = document.querySelector('#whitelist-table-body');
        if (!tbody) return;

        tbody.innerHTML = '';
        this.whitelist.forEach(item => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td class="px-6 py-4 text-sm text-gray-900">${item.ipAddress}</td>
                <td class="px-6 py-4 text-sm text-gray-900">${item.description || '-'}</td>
                <td class="px-6 py-4 text-sm text-gray-900">永久</td>
                <td class="px-6 py-4 text-sm text-gray-900">${item.createdTime ? (isNaN(new Date(item.createdTime)) ? '无效日期' : new Date(item.createdTime).toLocaleString()) : '-'}</td>
                <td class="px-6 py-4 text-sm">
                    <button class="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded text-xs mr-2" onclick="app.editWhitelist(${item.id})">
                        编辑
                    </button>
                    <button class="bg-red-600 hover:bg-red-700 text-white px-3 py-1 rounded text-xs" onclick="app.deleteWhitelist(${item.id})">
                        删除
                    </button>
                </td>
            `;
            tbody.appendChild(row);
        });
    }

    showWhitelistModal(item = null) {
        this.editingWhitelistId = item ? item.id : null;
        const modal = document.getElementById('whitelist-modal');
        const form = document.getElementById('whitelist-form');
        
        if (item) {
            form.elements.ipAddress.value = item.ipAddress;
            form.elements.description.value = item.description || '';
            // 白名单没有过期时间字段，移除expireTime设置
        } else {
            form.reset();
        }
        
        modal.style.display = 'flex';
    }

    hideWhitelistModal() {
        document.getElementById('whitelist-modal').style.display = 'none';
        this.editingWhitelistId = null;
    }

    bindWhitelistModalEvents() {
        const modal = document.getElementById('whitelist-modal');
        if (!modal) return;
        
        const closeBtn = modal.querySelector('.whitelist-modal-close');
        const cancelBtn = document.getElementById('whitelist-cancel-btn');
        const saveBtn = document.getElementById('whitelist-save-btn');
        
        if (closeBtn) closeBtn.addEventListener('click', () => this.hideWhitelistModal());
        if (cancelBtn) cancelBtn.addEventListener('click', () => this.hideWhitelistModal());
        if (saveBtn) saveBtn.addEventListener('click', () => this.saveWhitelist());
        
        window.addEventListener('click', (event) => {
            if (event.target === modal) {
                this.hideWhitelistModal();
            }
        });
    }

    async saveWhitelist() {
        const form = document.getElementById('whitelist-form');
        const data = {
            ipAddress: form.elements.ipAddress.value,
            description: form.elements.description.value
        };

        try {
            if (this.editingWhitelistId) {
                await this.apiCall(`/whitelist/${this.editingWhitelistId}`, 'PUT', data);
                this.showMessage('白名单更新成功', 'success');
            } else {
                await this.apiCall('/whitelist', 'POST', data);
                this.showMessage('白名单添加成功', 'success');
            }
            
            this.hideWhitelistModal();
            await this.loadWhitelist();
        } catch (error) {
            console.error('保存白名单失败:', error);
            this.showMessage('保存白名单失败', 'error');
        }
    }

    editWhitelist(id) {
        const item = this.whitelist.find(w => w.id === id);
        if (item) {
            this.showWhitelistModal(item);
        }
    }

    async deleteWhitelist(id) {
        if (!confirm('确定要删除这个白名单项吗？')) {
            return;
        }

        try {
            await this.apiCall(`/whitelist/${id}`, 'DELETE');
            this.showMessage('白名单删除成功', 'success');
            await this.loadWhitelist();
        } catch (error) {
            console.error('删除白名单失败:', error);
            this.showMessage('删除白名单失败', 'error');
        }
    }
}

// 初始化应用
let app;
document.addEventListener('DOMContentLoaded', () => {
    app = new FirewallApp();
    app.startAutoRefresh();
});

// 页面卸载时清理
window.addEventListener('beforeunload', () => {
    if (app) {
        app.stopAutoRefresh();
    }
});