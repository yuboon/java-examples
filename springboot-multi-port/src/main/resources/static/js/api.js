/**
 * API 调用模块
 * 处理与后端的所有HTTP请求
 */

class ApiClient {
    constructor() {
        this.baseUrls = {
            user: 'http://localhost:8082',
            admin: 'http://localhost:8083'
        };
        this.defaultHeaders = {
            'Content-Type': 'application/json'
        };
    }

    /**
     * 通用HTTP请求方法
     */
    async request(service, endpoint, options = {}) {
        const url = `${this.baseUrls[service]}${endpoint}`;
        const config = {
            headers: { ...this.defaultHeaders, ...options.headers },
            ...options
        };

        try {
            const response = await fetch(url, config);

            // 处理HTTP错误状态
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            // 尝试解析JSON响应
            const data = await response.json();
            return data;
        } catch (error) {
            console.error('API请求失败:', error);
            throw error;
        }
    }

    /**
     * GET请求
     */
    async get(service, endpoint, params = {}) {
        const queryString = new URLSearchParams(params).toString();
        const url = queryString ? `${endpoint}?${queryString}` : endpoint;
        return this.request(service, url);
    }

    /**
     * POST请求
     */
    async post(service, endpoint, data = {}) {
        return this.request(service, endpoint, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    /**
     * PUT请求
     */
    async put(service, endpoint, data = {}) {
        return this.request(service, endpoint, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    /**
     * DELETE请求
     */
    async delete(service, endpoint) {
        return this.request(service, endpoint, {
            method: 'DELETE'
        });
    }

    /**
     * PATCH请求
     */
    async patch(service, endpoint, data = {}) {
        return this.request(service, endpoint, {
            method: 'PATCH',
            body: JSON.stringify(data)
        });
    }
}

// 用户端API
class UserApi extends ApiClient {
    /**
     * 获取所有商品
     */
    async getProducts() {
        return this.get('user', '/api/user/products');
    }

    /**
     * 根据ID获取商品
     */
    async getProduct(id) {
        return this.get('user', `/api/user/products/${id}`);
    }

    /**
     * 根据分类获取商品
     */
    async getProductsByCategory(category) {
        return this.get('user', `/api/user/products/category/${category}`);
    }

    /**
     * 搜索商品
     */
    async searchProducts(keyword) {
        return this.get('user', '/api/user/products/search', { keyword });
    }

    /**
     * 获取用户购物车
     */
    async getCart(userId) {
        return this.get('user', `/api/user/cart/${userId}`);
    }

    /**
     * 添加商品到购物车
     */
    async addToCart(userId, productId, quantity) {
        return this.post('user', `/api/user/cart/${userId}/items`, {
            productId,
            quantity
        });
    }

    /**
     * 更新购物车商品数量
     */
    async updateCartItem(userId, cartItemId, quantity) {
        return this.put('user', `/api/user/cart/${userId}/items/${cartItemId}`, {
            quantity
        });
    }

    /**
     * 从购物车移除商品
     */
    async removeFromCart(userId, cartItemId) {
        return this.delete('user', `/api/user/cart/${userId}/items/${cartItemId}`);
    }

    /**
     * 清空购物车
     */
    async clearCart(userId) {
        return this.delete('user', `/api/user/cart/${userId}`);
    }

    /**
     * 获取购物车统计
     */
    async getCartSummary(userId) {
        return this.get('user', `/api/user/cart/${userId}/summary`);
    }

    /**
     * 健康检查
     */
    async healthCheck() {
        return this.get('user', '/health/user');
    }
}

// 管理端API
class AdminApi extends ApiClient {
    /**
     * 获取所有商品（包括下架的）
     */
    async getAllProducts() {
        return this.get('admin', '/api/admin/products');
    }

    /**
     * 根据ID获取商品
     */
    async getProduct(id) {
        return this.get('admin', `/api/admin/products/${id}`);
    }

    /**
     * 创建商品
     */
    async createProduct(product) {
        return this.post('admin', '/api/admin/products', product);
    }

    /**
     * 更新商品
     */
    async updateProduct(id, product) {
        return this.put('admin', `/api/admin/products/${id}`, product);
    }

    /**
     * 删除商品
     */
    async deleteProduct(id) {
        return this.delete('admin', `/api/admin/products/${id}`);
    }

    /**
     * 更新商品状态
     */
    async updateProductStatus(id, status) {
        return this.patch('admin', `/api/admin/products/${id}/status`, { status });
    }

    /**
     * 批量更新商品状态
     */
    async batchUpdateStatus(ids, status) {
        return this.patch('admin', '/api/admin/products/batch/status', {
            ids,
            status
        });
    }

    /**
     * 获取商品统计
     */
    async getProductStatistics() {
        return this.get('admin', '/api/admin/statistics/products');
    }

    /**
     * 获取系统概览
     */
    async getSystemOverview() {
        return this.get('admin', '/api/admin/statistics/overview');
    }

    /**
     * 获取端口状态
     */
    async getPortStatus() {
        return this.get('admin', '/api/admin/statistics/ports');
    }

    /**
     * 健康检查
     */
    async healthCheck() {
        return this.get('admin', '/health/admin');
    }
}

// 创建全局API实例
const userApi = new UserApi();
const adminApi = new AdminApi();

// 导出API实例
window.userApi = userApi;
window.adminApi = adminApi;