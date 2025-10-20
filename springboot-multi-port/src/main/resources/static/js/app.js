/**
 * 主应用入口
 */

class App {
    constructor() {
        this.initialized = false;
    }

    /**
     * 初始化应用
     */
    async init() {
        if (this.initialized) {
            return;
        }

        try {
            console.log('正在初始化多端口测试平台...');

            // 设置全局错误处理
            this.setupErrorHandling();

            // 初始化主题
            this.initTheme();

            // 检查后端连接
            await this.checkBackendConnection();

            // 订阅状态变化
            this.subscribeToStateChanges();

            // 加载初始页面
            this.loadInitialPage();

            this.initialized = true;
            console.log('应用初始化完成');

        } catch (error) {
            console.error('应用初始化失败:', error);
            uiManager.showNotification('初始化失败', '应用初始化过程中出现错误', 'error');
        }
    }

    /**
     * 设置全局错误处理
     */
    setupErrorHandling() {
        // 捕获未处理的Promise错误
        window.addEventListener('unhandledrejection', (event) => {
            console.error('未处理的Promise错误:', event.reason);
            uiManager.showNotification('系统错误', '发生未预期的错误', 'error');
        });

        // 捕获全局JavaScript错误
        window.addEventListener('error', (event) => {
            console.error('全局JavaScript错误:', event.error);
            uiManager.showNotification('系统错误', '脚本执行出错', 'error');
        });
    }

    /**
     * 初始化主题
     */
    initTheme() {
        const savedTheme = localStorage.getItem('theme') || 'light';
        if (savedTheme === 'dark') {
            document.body.classList.add('dark');
            const themeBtn = document.getElementById('themeToggle');
            if (themeBtn) {
                themeBtn.innerHTML = '<i class="fas fa-sun"></i>';
            }
        }
        appState.updateState('ui.theme', savedTheme);
    }

    /**
     * 检查后端连接
     */
    async checkBackendConnection() {
        try {
            // 检查用户端连接
            const userResponse = await fetch('http://localhost:8082/health/user');
            const userConnected = userResponse.ok;

            // 检查管理端连接
            const adminResponse = await fetch('http://localhost:8083/health/admin');
            const adminConnected = adminResponse.ok;

            if (userConnected && adminConnected) {
                uiManager.showNotification('连接成功', '已连接到后端服务', 'success');
                uiManager.updateConnectionStatus(true);
            } else if (userConnected) {
                uiManager.showNotification('部分连接', '用户端服务可用，管理端服务不可用', 'warning');
                uiManager.updateConnectionStatus(true);
            } else {
                uiManager.showNotification('连接失败', '无法连接到后端服务，请确保服务正在运行', 'error');
                uiManager.updateConnectionStatus(false);
            }

        } catch (error) {
            console.error('检查后端连接失败:', error);
            uiManager.showNotification('连接失败', '无法连接到后端服务', 'error');
            uiManager.updateConnectionStatus(false);
        }
    }

    /**
     * 订阅状态变化
     */
    subscribeToStateChanges() {
        appState.subscribe((state) => {
            // 这里可以响应状态变化
            console.log('状态更新:', state);
        });
    }

    /**
     * 加载初始页面
     */
    loadInitialPage() {
        // 默认加载用户端商品页面
        uiManager.switchUserPage('products');
    }

    /**
     * 重启应用
     */
    restart() {
        this.initialized = false;
        this.init();
    }

    /**
     * 获取应用信息
     */
    getInfo() {
        return {
            name: '双端口应用测试平台',
            version: '1.0.0',
            description: '用于测试多端口Spring Boot应用的前端界面',
            features: [
                '用户端商品浏览和购物车',
                '管理端商品管理和统计',
                '实时系统监控',
                '响应式设计'
            ],
            apis: {
                user: 'http://localhost:8082',
                admin: 'http://localhost:8083'
            }
        };
    }
}

// 创建全局应用实例
const app = new App();

// 页面卸载时清理资源
window.addEventListener('beforeunload', () => {
    if (AdminSystemPage && AdminSystemPage.destroy) {
        AdminSystemPage.destroy();
    }
});

// 导出到全局作用域
window.App = App;       // 导出类
window.app = app;       // 导出实例（小写）
window.AppClass = App;  // 导出类（兼容性）

console.log('App类已导出到全局作用域:', typeof window.App);
console.log('app实例已导出到全局作用域:', typeof window.app);
console.log('app实例是否有init方法:', typeof window.app?.init);