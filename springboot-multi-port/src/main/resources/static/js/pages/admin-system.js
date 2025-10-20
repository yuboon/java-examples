/**
 * 管理端系统监控页面
 */

const AdminSystemPage = {
    updateInterval: null,

    /**
     * 渲染页面
     */
    async render(container) {
        container.innerHTML = `
            <div class="space-y-6">
                <!-- 页面标题 -->
                <div class="flex justify-between items-center">
                    <h1 class="text-2xl font-bold text-gray-900">
                        <i class="fas fa-tachometer-alt mr-2 text-red-600"></i>
                        系统监控
                    </h1>
                    <div class="flex space-x-2">
                        <button onclick="AdminSystemPage.toggleAutoRefresh()"
                                id="autoRefreshBtn"
                                class="bg-green-500 hover:bg-green-600 text-white px-4 py-2 rounded-lg transition">
                            <i class="fas fa-sync-alt mr-2"></i>
                            <span id="autoRefreshText">开始自动刷新</span>
                        </button>
                        <button onclick="AdminSystemPage.refreshSystemData()"
                                class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg transition">
                            <i class="fas fa-sync mr-2"></i>手动刷新
                        </button>
                    </div>
                </div>

                <!-- 系统状态概览 -->
                <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <!-- 用户端服务状态 -->
                    <div class="bg-white rounded-lg shadow-md p-6">
                        <div class="flex items-center justify-between mb-4">
                            <h3 class="text-lg font-semibold text-gray-900">用户端服务</h3>
                            <span id="userPortStatus" class="px-2 py-1 rounded-full text-xs font-medium">
                                <i class="fas fa-circle mr-1"></i>
                                检测中...
                            </span>
                        </div>
                        <div class="space-y-3">
                            <div class="flex justify-between">
                                <span class="text-gray-600">端口:</span>
                                <span class="font-medium">8082</span>
                            </div>
                            <div class="flex justify-between">
                                <span class="text-gray-600">响应时间:</span>
                                <span id="userResponseTime" class="font-medium">--ms</span>
                            </div>
                            <div class="flex justify-between">
                                <span class="text-gray-600">健康检查:</span>
                                <span id="userHealthCheck" class="font-medium">--</span>
                            </div>
                            <div class="flex justify-between">
                                <span class="text-gray-600">最后检查:</span>
                                <span id="userLastCheck" class="font-medium">--</span>
                            </div>
                        </div>
                    </div>

                    <!-- 管理端服务状态 -->
                    <div class="bg-white rounded-lg shadow-md p-6">
                        <div class="flex items-center justify-between mb-4">
                            <h3 class="text-lg font-semibold text-gray-900">管理端服务</h3>
                            <span id="adminPortStatus" class="px-2 py-1 rounded-full text-xs font-medium">
                                <i class="fas fa-circle mr-1"></i>
                                检测中...
                            </span>
                        </div>
                        <div class="space-y-3">
                            <div class="flex justify-between">
                                <span class="text-gray-600">端口:</span>
                                <span class="font-medium">8083</span>
                            </div>
                            <div class="flex justify-between">
                                <span class="text-gray-600">响应时间:</span>
                                <span id="adminResponseTime" class="font-medium">--ms</span>
                            </div>
                            <div class="flex justify-between">
                                <span class="text-gray-600">健康检查:</span>
                                <span id="adminHealthCheck" class="font-medium">--</span>
                            </div>
                            <div class="flex justify-between">
                                <span class="text-gray-600">最后检查:</span>
                                <span id="adminLastCheck" class="font-medium">--</span>
                            </div>
                        </div>
                    </div>

                    <!-- 系统信息 -->
                    <div class="bg-white rounded-lg shadow-md p-6">
                        <div class="flex items-center justify-between mb-4">
                            <h3 class="text-lg font-semibold text-gray-900">系统信息</h3>
                            <i class="fas fa-server text-gray-400"></i>
                        </div>
                        <div class="space-y-3">
                            <div class="flex justify-between">
                                <span class="text-gray-600">Java版本:</span>
                                <span id="javaVersion" class="font-medium">--</span>
                            </div>
                            <div class="flex justify-between">
                                <span class="text-gray-600">操作系统:</span>
                                <span id="osName" class="font-medium">--</span>
                            </div>
                            <div class="flex justify-between">
                                <span class="text-gray-600">CPU核心:</span>
                                <span id="cpuCores" class="font-medium">--</span>
                            </div>
                            <div class="flex justify-between">
                                <span class="text-gray-600">运行时间:</span>
                                <span id="uptime" class="font-medium">--</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 性能监控 -->
                <div class="bg-white rounded-lg shadow-md p-6">
                    <h3 class="text-lg font-semibold text-gray-900 mb-4">
                        <i class="fas fa-chart-line mr-2 text-blue-600"></i>
                        性能监控
                    </h3>
                    <div class="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
                        <div class="bg-gray-50 rounded-lg p-4">
                            <div class="flex items-center justify-between mb-2">
                                <span class="text-gray-600 text-sm">内存使用</span>
                                <span id="memoryPercent" class="text-blue-600 font-semibold">--%</span>
                            </div>
                            <div class="w-full bg-gray-200 rounded-full h-2 mb-2">
                                <div id="memoryBar" class="bg-blue-500 h-2 rounded-full transition-all duration-300" style="width: 0%"></div>
                            </div>
                            <div class="text-xs text-gray-500">
                                已用: <span id="memoryUsed">--MB</span> / 总计: <span id="memoryTotal">--MB</span>
                            </div>
                        </div>

                        <div class="bg-gray-50 rounded-lg p-4">
                            <div class="flex items-center justify-between mb-2">
                                <span class="text-gray-600 text-sm">CPU使用率</span>
                                <span id="cpuPercent" class="text-green-600 font-semibold">--%</span>
                            </div>
                            <div class="w-full bg-gray-200 rounded-full h-2 mb-2">
                                <div id="cpuBar" class="bg-green-500 h-2 rounded-full transition-all duration-300" style="width: 0%"></div>
                            </div>
                            <div class="text-xs text-gray-500">
                                当前负载: <span id="cpuLoad">--</span>
                            </div>
                        </div>

                        <div class="bg-gray-50 rounded-lg p-4">
                            <div class="flex items-center justify-between mb-2">
                                <span class="text-gray-600 text-sm">活跃线程</span>
                                <span id="threadsCount" class="text-purple-600 font-semibold">--</span>
                            </div>
                            <div class="w-full bg-gray-200 rounded-full h-2 mb-2">
                                <div id="threadsBar" class="bg-purple-500 h-2 rounded-full transition-all duration-300" style="width: 0%"></div>
                            </div>
                            <div class="text-xs text-gray-500">
                                最大: <span id="maxThreads">--</span>
                            </div>
                        </div>

                        <div class="bg-gray-50 rounded-lg p-4">
                            <div class="flex items-center justify-between mb-2">
                                <span class="text-gray-600 text-sm">请求QPS</span>
                                <span id="qpsCount" class="text-orange-600 font-semibold">--</span>
                            </div>
                            <div class="w-full bg-gray-200 rounded-full h-2 mb-2">
                                <div id="qpsBar" class="bg-orange-500 h-2 rounded-full transition-all duration-300" style="width: 0%"></div>
                            </div>
                            <div class="text-xs text-gray-500">
                                总请求: <span id="totalRequests">--</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;

        // 加载初始数据
        await this.loadSystemData();
    },

    /**
     * 加载系统数据
     */
    async loadSystemData() {
        try {
            // 获取系统概览数据
            const overviewResponse = await adminApi.getSystemOverview();
            if (overviewResponse.code === 200) {
                this.updateSystemInfo(overviewResponse.data);
            }

            // 检查健康状态
            await this.checkHealthStatus();

            // 更新性能数据
            this.updatePerformanceMetrics();

        } catch (error) {
            console.error('加载系统数据失败:', error);
            uiManager.showNotification('加载失败', '无法获取系统数据', 'error');
        }
    },

    /**
     * 更新系统信息
     */
    updateSystemInfo(systemData) {
        if (systemData.system) {
            document.getElementById('javaVersion').textContent = systemData.system.javaVersion || '--';
            document.getElementById('osName').textContent = systemData.system.osName || '--';
            document.getElementById('cpuCores').textContent = systemData.system.availableProcessors || '--';
        }

        // 计算运行时间（这里使用模拟数据）
        const uptime = this.calculateUptime();
        document.getElementById('uptime').textContent = uptime;
    },

    /**
     * 检查健康状态
     */
    async checkHealthStatus() {
        const now = new Date().toLocaleTimeString();

        // 检查用户端健康状态
        try {
            const startTime = Date.now();
            const response = await fetch('http://localhost:8082/health/user');
            const responseTime = Date.now() - startTime;

            document.getElementById('userResponseTime').textContent = `${responseTime}ms`;
            document.getElementById('userHealthCheck').textContent = response.ok ? '正常' : '异常';
            document.getElementById('userHealthCheck').className = response.ok ? 'font-medium text-green-600' : 'font-medium text-red-600';
            document.getElementById('userLastCheck').textContent = now;

            const userStatus = document.getElementById('userPortStatus');
            userStatus.className = response.ok ?
                'px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800' :
                'px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800';
            userStatus.innerHTML = response.ok ?
                '<i class="fas fa-circle mr-1"></i>正常' :
                '<i class="fas fa-circle mr-1"></i>异常';

            uiManager.updateConnectionStatus(true);

        } catch (error) {
            document.getElementById('userResponseTime').textContent = '--ms';
            document.getElementById('userHealthCheck').textContent = '连接失败';
            document.getElementById('userHealthCheck').className = 'font-medium text-red-600';
            document.getElementById('userLastCheck').textContent = now;

            const userStatus = document.getElementById('userPortStatus');
            userStatus.className = 'px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800';
            userStatus.innerHTML = '<i class="fas fa-circle mr-1"></i>异常';

            uiManager.updateConnectionStatus(false);
        }

        // 检查管理端健康状态
        try {
            const startTime = Date.now();
            const response = await fetch('http://localhost:8083/health/admin');
            const responseTime = Date.now() - startTime;

            document.getElementById('adminResponseTime').textContent = `${responseTime}ms`;
            document.getElementById('adminHealthCheck').textContent = response.ok ? '正常' : '异常';
            document.getElementById('adminHealthCheck').className = response.ok ? 'font-medium text-green-600' : 'font-medium text-red-600';
            document.getElementById('adminLastCheck').textContent = now;

            const adminStatus = document.getElementById('adminPortStatus');
            adminStatus.className = response.ok ?
                'px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800' :
                'px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800';
            adminStatus.innerHTML = response.ok ?
                '<i class="fas fa-circle mr-1"></i>正常' :
                '<i class="fas fa-circle mr-1"></i>异常';

        } catch (error) {
            document.getElementById('adminResponseTime').textContent = '--ms';
            document.getElementById('adminHealthCheck').textContent = '连接失败';
            document.getElementById('adminHealthCheck').className = 'font-medium text-red-600';
            document.getElementById('adminLastCheck').textContent = now;

            const adminStatus = document.getElementById('adminPortStatus');
            adminStatus.className = 'px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800';
            adminStatus.innerHTML = '<i class="fas fa-circle mr-1"></i>异常';
        }
    },

    /**
     * 更新性能指标
     */
    updatePerformanceMetrics() {
        // 模拟性能数据（实际项目中应该从真实的监控系统获取）
        const memoryUsed = Math.floor(Math.random() * 300) + 200; // 200-500MB
        const memoryTotal = 1024; // 1GB
        const memoryPercent = Math.round((memoryUsed / memoryTotal) * 100);

        const cpuPercent = Math.floor(Math.random() * 40) + 10; // 10-50%
        const threads = Math.floor(Math.random() * 50) + 30; // 30-80
        const maxThreads = 200;
        const qps = Math.floor(Math.random() * 100) + 20; // 20-120

        // 更新内存使用
        document.getElementById('memoryPercent').textContent = `${memoryPercent}%`;
        document.getElementById('memoryUsed').textContent = `${memoryUsed}MB`;
        document.getElementById('memoryTotal').textContent = `${memoryTotal}MB`;
        document.getElementById('memoryBar').style.width = `${memoryPercent}%`;

        // 更新CPU使用率
        document.getElementById('cpuPercent').textContent = `${cpuPercent}%`;
        document.getElementById('cpuLoad').textContent = `${cpuPercent}%`;
        document.getElementById('cpuBar').style.width = `${cpuPercent}%`;

        // 更新线程数
        document.getElementById('threadsCount').textContent = threads;
        document.getElementById('maxThreads').textContent = maxThreads;
        document.getElementById('threadsBar').style.width = `${Math.round((threads / maxThreads) * 100)}%`;

        // 更新QPS
        document.getElementById('qpsCount').textContent = qps;
        document.getElementById('totalRequests').textContent = Math.floor(Math.random() * 10000) + 5000;
        document.getElementById('qpsBar').style.width = `${Math.min(Math.round((qps / 200) * 100), 100)}%`;
    },

    /**
     * 切换自动刷新
     */
    toggleAutoRefresh() {
        const btn = document.getElementById('autoRefreshBtn');
        const text = document.getElementById('autoRefreshText');

        if (this.updateInterval) {
            // 停止自动刷新
            clearInterval(this.updateInterval);
            this.updateInterval = null;
            btn.className = 'bg-green-500 hover:bg-green-600 text-white px-4 py-2 rounded-lg transition';
            text.textContent = '开始自动刷新';
            uiManager.showNotification('提示', '自动刷新已停止', 'info');
        } else {
            // 开始自动刷新
            this.updateInterval = setInterval(() => {
                this.refreshSystemData();
            }, 10000); // 每10秒刷新一次

            btn.className = 'bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded-lg transition';
            text.textContent = '停止自动刷新';
            uiManager.showNotification('提示', '自动刷新已开启 (10秒间隔)', 'info');
        }
    },

    /**
     * 手动刷新系统数据
     */
    async refreshSystemData() {
        await this.loadSystemData();
    },

    /**
     * 计算运行时间
     */
    calculateUptime() {
        // 模拟运行时间（实际项目中应该从服务器获取）
        const now = Date.now();
        const startTime = now - (Math.random() * 86400000 * 7); // 随机1-7天前
        const uptime = now - startTime;

        const days = Math.floor(uptime / (1000 * 60 * 60 * 24));
        const hours = Math.floor((uptime % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((uptime % (1000 * 60 * 60)) / (1000 * 60));

        if (days > 0) {
            return `${days}天 ${hours}小时 ${minutes}分钟`;
        } else if (hours > 0) {
            return `${hours}小时 ${minutes}分钟`;
        } else {
            return `${minutes}分钟`;
        }
    },

    /**
     * 清理资源
     */
    destroy() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
            this.updateInterval = null;
        }
    }
};

// 导出到全局作用域
window.AdminSystemPage = AdminSystemPage;