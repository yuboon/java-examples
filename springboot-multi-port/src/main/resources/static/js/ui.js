/**
 * UI 交互模块
 * 处理用户界面的交互和显示
 */

class UIManager {
    constructor() {
        this.initEventListeners();
        this.connectButtonEvents();
    }

    /**
     * 初始化事件监听器
     */
    initEventListeners() {
        // 服务切换标签
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.switchService(e.target.dataset.service);
            });
        });

        // 用户端页面标签
        document.querySelectorAll('.user-tab').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.switchUserPage(e.target.dataset.page);
            });
        });

        // 管理端页面标签
        document.querySelectorAll('.admin-tab').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.switchAdminPage(e.target.dataset.page);
            });
        });

        // 主题切换
        document.getElementById('themeToggle').addEventListener('click', () => {
            this.toggleTheme();
        });
    }

    /**
     * 连接按钮事件到页面模块
     */
    connectButtonEvents() {
        // 这里会由各个页面模块自己处理按钮事件
    }

    /**
     * 切换服务类型
     */
    switchService(service) {
        // 更新标签状态
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.remove('tab-active');
            if (btn.dataset.service === service) {
                btn.classList.add('tab-active');
            }
        });

        // 切换内容显示
        document.getElementById('userContent').classList.toggle('hidden', service !== 'user');
        document.getElementById('adminContent').classList.toggle('hidden', service !== 'admin');

        // 更新状态
        appState.switchService(service);

        // 加载对应页面
        if (service === 'user') {
            const currentPage = appState.get('currentPage.user');
            this.switchUserPage(currentPage);
        } else {
            const currentPage = appState.get('currentPage.admin');
            this.switchAdminPage(currentPage);
        }
    }

    /**
     * 切换用户端页面
     */
    switchUserPage(page) {
        // 更新标签状态
        document.querySelectorAll('.user-tab').forEach(btn => {
            btn.classList.remove('tab-active');
            if (btn.dataset.page === page) {
                btn.classList.add('tab-active');
            }
        });

        // 更新状态
        appState.switchPage('user', page);

        // 加载页面内容
        const container = document.getElementById('userPageContainer');
        this.loadPage('user', page, container);
    }

    /**
     * 切换管理端页面
     */
    switchAdminPage(page) {
        // 更新标签状态
        document.querySelectorAll('.admin-tab').forEach(btn => {
            btn.classList.remove('tab-active');
            if (btn.dataset.page === page) {
                btn.classList.add('tab-active');
            }
        });

        // 更新状态
        appState.switchPage('admin', page);

        // 加载页面内容
        const container = document.getElementById('adminPageContainer');
        this.loadPage('admin', page, container);
    }

    /**
     * 加载页面内容
     */
    loadPage(service, page, container) {
        // 显示加载状态
        this.showLoading();

        // 调用对应的页面模块
        if (service === 'user') {
            switch (page) {
                case 'products':
                    UserProductsPage.render(container);
                    break;
                case 'cart':
                    UserCartPage.render(container);
                    break;
                case 'search':
                    UserSearchPage.render(container);
                    break;
            }
        } else if (service === 'admin') {
            switch (page) {
                case 'products':
                    AdminProductsPage.render(container);
                    break;
                case 'statistics':
                    AdminStatisticsPage.render(container);
                    break;
                case 'system':
                    AdminSystemPage.render(container);
                    break;
            }
        }

        // 隐藏加载状态
        this.hideLoading();
    }

    /**
     * 显示加载状态
     */
    showLoading() {
        document.getElementById('loadingOverlay').classList.remove('hidden');
        appState.setLoading(true);
    }

    /**
     * 隐藏加载状态
     */
    hideLoading() {
        document.getElementById('loadingOverlay').classList.add('hidden');
        appState.setLoading(false);
    }

    /**
     * 显示通知
     */
    showNotification(title, message, type = 'info') {
        const notification = {
            title,
            message,
            type
        };

        appState.addNotification(notification);

        // 显示通知UI
        const notificationEl = document.getElementById('notification');
        const iconEl = document.getElementById('notificationIcon');
        const titleEl = document.getElementById('notificationTitle');
        const messageEl = document.getElementById('notificationMessage');

        // 设置图标
        const icons = {
            success: '<i class="fas fa-check-circle text-green-500 text-xl"></i>',
            error: '<i class="fas fa-exclamation-circle text-red-500 text-xl"></i>',
            warning: '<i class="fas fa-exclamation-triangle text-yellow-500 text-xl"></i>',
            info: '<i class="fas fa-info-circle text-blue-500 text-xl"></i>'
        };

        iconEl.innerHTML = icons[type] || icons.info;
        titleEl.textContent = title;
        messageEl.textContent = message;

        // 显示通知
        notificationEl.classList.remove('hidden');
        notificationEl.classList.add('fade-in');

        // 自动隐藏
        setTimeout(() => {
            notificationEl.classList.add('hidden');
        }, 5000);
    }

    /**
     * 更新连接状态
     */
    updateConnectionStatus(connected) {
        const statusEl = document.getElementById('connectionStatus');

        if (connected) {
            statusEl.className = 'text-sm px-3 py-1 rounded-full bg-green-100 text-green-800';
            statusEl.innerHTML = '<i class="fas fa-circle text-green-500 mr-1"></i>已连接';
        } else {
            statusEl.className = 'text-sm px-3 py-1 rounded-full bg-red-100 text-red-800';
            statusEl.innerHTML = '<i class="fas fa-circle text-red-500 mr-1"></i>连接断开';
        }
    }

    /**
     * 切换主题
     */
    toggleTheme() {
        const currentTheme = appState.get('ui.theme');
        const newTheme = currentTheme === 'light' ? 'dark' : 'light';

        // 更新主题类
        document.body.classList.toggle('dark');

        // 更新主题图标
        const themeBtn = document.getElementById('themeToggle');
        themeBtn.innerHTML = newTheme === 'light' ?
            '<i class="fas fa-moon"></i>' :
            '<i class="fas fa-sun"></i>';

        // 更新状态
        appState.updateState('ui.theme', newTheme);

        // 保存到本地存储
        localStorage.setItem('theme', newTheme);
    }

    /**
     * 格式化价格
     */
    formatPrice(price) {
        return new Intl.NumberFormat('zh-CN', {
            style: 'currency',
            currency: 'CNY'
        }).format(price || 0);
    }

    /**
     * 格式化日期时间
     */
    formatDateTime(dateString) {
        if (!dateString) return '-';

        const date = new Date(dateString);
        return new Intl.DateTimeFormat('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        }).format(date);
    }

    /**
     * 创建空状态提示
     */
    createEmptyState(message, icon = 'fa-inbox') {
        return `
            <div class="text-center py-12">
                <i class="fas ${icon} text-gray-300 text-6xl mb-4"></i>
                <p class="text-gray-500 text-lg">${message}</p>
            </div>
        `;
    }

    /**
     * 创建错误状态提示
     */
    createErrorState(message, onRetry) {
        return `
            <div class="text-center py-12">
                <i class="fas fa-exclamation-triangle text-red-300 text-6xl mb-4"></i>
                <p class="text-red-500 text-lg mb-4">${message}</p>
                ${onRetry ? `
                    <button onclick="${onRetry}" class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg">
                        <i class="fas fa-redo mr-2"></i>重试
                    </button>
                ` : ''}
            </div>
        `;
    }

    /**
     * 防抖函数
     */
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    /**
     * 节流函数
     */
    throttle(func, limit) {
        let inThrottle;
        return function() {
            const args = arguments;
            const context = this;
            if (!inThrottle) {
                func.apply(context, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    }
}

// 创建全局UI管理器实例
const uiManager = new UIManager();

// 导出UI管理器
window.uiManager = uiManager;