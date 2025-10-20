/**
 * 用户端商品搜索页面
 */

const UserSearchPage = {
    /**
     * 渲染页面
     */
    async render(container) {
        container.innerHTML = `
            <div class="space-y-6">
                <!-- 页面标题 -->
                <div class="flex justify-between items-center">
                    <h1 class="text-2xl font-bold text-gray-900">
                        <i class="fas fa-search mr-2 text-purple-600"></i>
                        商品搜索
                    </h1>
                    <button onclick="uiManager.switchUserPage('products')"
                            class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg transition">
                        <i class="fas fa-arrow-left mr-2"></i>
                        返回商品列表
                    </button>
                </div>

                <!-- 搜索框 -->
                <div class="bg-white rounded-lg shadow-md p-6">
                    <div class="flex space-x-4">
                        <div class="flex-1">
                            <div class="relative">
                                <input type="text"
                                       id="searchInput"
                                       placeholder="搜索商品名称或描述..."
                                       class="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                       onkeyup="UserSearchPage.handleSearchKeyup(event)">
                                <i class="fas fa-search absolute left-3 top-4 text-gray-400"></i>
                            </div>
                        </div>
                        <button onclick="UserSearchPage.performSearch()"
                                class="bg-blue-500 hover:bg-blue-600 text-white px-6 py-3 rounded-lg transition">
                            <i class="fas fa-search mr-2"></i>
                            搜索
                        </button>
                    </div>
                </div>

                <!-- 搜索结果 -->
                <div id="searchResultsContainer">
                    <div class="text-center py-12">
                        <i class="fas fa-search text-gray-300 text-6xl mb-4"></i>
                        <p class="text-gray-500 text-lg">请输入关键词搜索商品</p>
                    </div>
                </div>
            </div>
        `;

        this.setupSearchInput();
    },

    /**
     * 设置搜索输入框
     */
    setupSearchInput() {
        const searchInput = document.getElementById('searchInput');
        searchInput.focus();
    },

    /**
     * 处理搜索按键事件
     */
    handleSearchKeyup(event) {
        if (event.key === 'Enter') {
            this.performSearch();
        }
    },

    /**
     * 执行搜索
     */
    async performSearch() {
        const searchInput = document.getElementById('searchInput');
        const keyword = searchInput.value.trim();

        if (!keyword) {
            uiManager.showNotification('提示', '请输入搜索关键词', 'warning');
            return;
        }

        await this.searchTerm(keyword);
    },

    /**
     * 搜索指定关键词
     */
    async searchTerm(keyword) {
        try {
            uiManager.showLoading();

            // 更新搜索框
            document.getElementById('searchInput').value = keyword;

            // 执行API搜索
            const response = await userApi.searchProducts(keyword);

            if (response.code === 200) {
                const products = response.data;
                this.renderSearchResults(products, keyword);

                if (products.length === 0) {
                    this.showNoResults(keyword);
                }
            } else {
                throw new Error(response.message);
            }
        } catch (error) {
            console.error('搜索失败:', error);
            this.showSearchError();
            uiManager.showNotification('搜索失败', '无法完成搜索，请重试', 'error');
        } finally {
            uiManager.hideLoading();
        }
    },

    /**
     * 渲染搜索结果
     */
    renderSearchResults(products, keyword) {
        const container = document.getElementById('searchResultsContainer');

        container.innerHTML = `
            <div class="bg-white rounded-lg shadow-md">
                <div class="p-4 border-b border-gray-200">
                    <div class="flex items-center justify-between">
                        <h2 class="text-lg font-semibold text-gray-900">
                            搜索结果: "<span class="text-blue-600">${keyword}</span>"
                        </h2>
                        <span class="text-gray-600">找到 ${products.length} 个商品</span>
                    </div>
                </div>
                <div class="p-4">
                    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        ${products.map(product => this.createSearchResultCard(product)).join('')}
                    </div>
                </div>
            </div>
        `;
    },

    /**
     * 创建搜索结果卡片
     */
    createSearchResultCard(product) {
        const isOutOfStock = product.stock <= 0;
        const statusBadge = product.status ?
            '<span class="bg-green-100 text-green-800 text-xs px-2 py-1 rounded">在售</span>' :
            '<span class="bg-red-100 text-red-800 text-xs px-2 py-1 rounded">下架</span>';

        return `
            <div class="border border-gray-200 rounded-lg p-4 hover:shadow-lg transition-shadow ${isOutOfStock ? 'opacity-60' : ''}">
                <div class="flex space-x-4">
                    <img src="https://picsum.photos/seed/${product.id}/100/100.jpg"
                         alt="${product.name}"
                         class="w-20 h-20 object-cover rounded-lg">
                    <div class="flex-1">
                        <h3 class="font-semibold text-gray-900 mb-1">${product.name}</h3>
                        <p class="text-gray-600 text-sm mb-2">${product.description}</p>
                        <div class="flex items-center justify-between">
                            <span class="text-red-600 font-bold">${uiManager.formatPrice(product.price)}</span>
                            <div class="flex items-center space-x-2">
                                ${statusBadge}
                                <span class="text-sm text-gray-500">库存: ${product.stock}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    },

    /**
     * 显示无结果状态
     */
    showNoResults(keyword) {
        document.getElementById('searchResultsContainer').innerHTML = `
            <div class="text-center py-12">
                <i class="fas fa-search text-gray-300 text-6xl mb-4"></i>
                <p class="text-gray-500 text-lg mb-2">未找到与 "<span class="font-semibold">${keyword}</span>" 相关的商品</p>
                <p class="text-gray-400 text-sm">建议尝试其他关键词</p>
            </div>
        `;
    },

    /**
     * 显示搜索错误
     */
    showSearchError() {
        document.getElementById('searchResultsContainer').innerHTML = `
            ${uiManager.createErrorState('搜索失败，请重试', 'UserSearchPage.performSearch()')}
        `;
    }
};

// 导出到全局作用域
window.UserSearchPage = UserSearchPage;