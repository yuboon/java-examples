/**
 * 状态管理模块
 * 管理应用的全局状态
 */

class AppState {
    constructor() {
        this.state = {
            // 当前服务类型
            currentService: 'user',

            // 当前页面
            currentPage: {
                user: 'products',
                admin: 'products'
            },

            // 用户数据
            currentUser: {
                id: 1, // 模拟用户ID
                name: '测试用户'
            },

            // 商品数据
            products: [],
            categories: [],

            // 购物车数据
            cart: {
                items: [],
                summary: {
                    totalAmount: 0,
                    itemCount: 0
                }
            },

            // 管理端数据
            adminData: {
                allProducts: [],
                statistics: {},
                systemInfo: {},
                portStatus: {}
            },

            // UI状态
            ui: {
                loading: false,
                notifications: [],
                theme: 'light'
            },

            // 搜索和过滤
            search: {
                keyword: '',
                category: '',
                sortBy: 'name',
                sortOrder: 'asc'
            }
        };

        // 订阅者列表
        this.subscribers = [];
    }

    /**
     * 获取状态
     */
    getState() {
        return this.state;
    }

    /**
     * 获取特定状态
     */
    get(path) {
        return this.getNestedValue(this.state, path);
    }

    /**
     * 设置状态
     */
    setState(updates) {
        this.state = { ...this.state, ...updates };
        this.notify();
    }

    /**
     * 更新嵌套状态
     */
    updateState(path, value) {
        this.setNestedValue(this.state, path, value);
        this.notify();
    }

    /**
     * 获取嵌套对象的值
     */
    getNestedValue(obj, path) {
        return path.split('.').reduce((current, key) => {
            return current && current[key] !== undefined ? current[key] : null;
        }, obj);
    }

    /**
     * 设置嵌套对象的值
     */
    setNestedValue(obj, path, value) {
        const keys = path.split('.');
        const lastKey = keys.pop();
        const target = keys.reduce((current, key) => {
            if (!current[key]) current[key] = {};
            return current[key];
        }, obj);
        target[lastKey] = value;
    }

    /**
     * 订阅状态变化
     */
    subscribe(callback) {
        this.subscribers.push(callback);
        return () => {
            this.subscribers = this.subscribers.filter(sub => sub !== callback);
        };
    }

    /**
     * 通知所有订阅者
     */
    notify() {
        this.subscribers.forEach(callback => callback(this.state));
    }

    /**
     * 重置状态
     */
    reset() {
        this.state = {
            currentService: 'user',
            currentPage: {
                user: 'products',
                admin: 'products'
            },
            currentUser: {
                id: 1,
                name: '测试用户'
            },
            products: [],
            categories: [],
            cart: {
                items: [],
                summary: {
                    totalAmount: 0,
                    itemCount: 0
                }
            },
            adminData: {
                allProducts: [],
                statistics: {},
                systemInfo: {},
                portStatus: {}
            },
            ui: {
                loading: false,
                notifications: [],
                theme: 'light'
            },
            search: {
                keyword: '',
                category: '',
                sortBy: 'name',
                sortOrder: 'asc'
            }
        };
        this.notify();
    }

    /**
     * 切换服务类型
     */
    switchService(service) {
        this.updateState('currentService', service);
    }

    /**
     * 切换页面
     */
    switchPage(service, page) {
        this.updateState(`currentPage.${service}`, page);
    }

    /**
     * 设置加载状态
     */
    setLoading(loading) {
        this.updateState('ui.loading', loading);
    }

    /**
     * 添加通知
     */
    addNotification(notification) {
        const notifications = [...this.state.ui.notifications, {
            id: Date.now(),
            timestamp: new Date(),
            ...notification
        }];
        this.updateState('ui.notifications', notifications);

        // 自动移除通知
        setTimeout(() => {
            this.removeNotification(notification.id);
        }, 5000);
    }

    /**
     * 移除通知
     */
    removeNotification(id) {
        const notifications = this.state.ui.notifications.filter(n => n.id !== id);
        this.updateState('ui.notifications', notifications);
    }

    /**
     * 更新商品列表
     */
    updateProducts(products) {
        this.updateState('products', products);

        // 提取分类
        const categories = [...new Set(products.map(p => p.category).filter(Boolean))];
        this.updateState('categories', categories);
    }

    /**
     * 更新购物车
     */
    updateCart(cartData) {
        this.updateState('cart', cartData);
    }

    /**
     * 更新管理端数据
     */
    updateAdminData(key, data) {
        this.updateState(`adminData.${key}`, data);
    }

    /**
     * 更新搜索参数
     */
    updateSearchParams(params) {
        this.updateState('search', { ...this.state.search, ...params });
    }
}

// 创建全局状态实例
const appState = new AppState();

// 导出状态实例
window.appState = appState;