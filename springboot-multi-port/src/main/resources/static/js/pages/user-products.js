/**
 * 用户端商品页面
 */

const UserProductsPage = {
    /**
     * 渲染页面
     */
    async render(container) {
        container.innerHTML = `
            <div class="space-y-6">
                <!-- 页面标题 -->
                <div class="flex justify-between items-center">
                    <h1 class="text-2xl font-bold text-gray-900">
                        <i class="fas fa-shopping-bag mr-2 text-blue-600"></i>
                        商品浏览
                    </h1>
                    <div class="flex space-x-2">
                        <button onclick="UserProductsPage.refreshProducts()"
                                class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg transition">
                            <i class="fas fa-sync-alt mr-2"></i>刷新
                        </button>
                        <button onclick="UserProductsPage.switchToCart()"
                                class="bg-green-500 hover:bg-green-600 text-white px-4 py-2 rounded-lg transition relative">
                            <i class="fas fa-shopping-cart mr-2"></i>
                            购物车
                            <span id="cartBadge" class="absolute -top-2 -right-2 bg-red-500 text-white text-xs rounded-full w-6 h-6 flex items-center justify-center hidden">0</span>
                        </button>
                    </div>
                </div>

                <!-- 分类过滤器 -->
                <div class="bg-white rounded-lg shadow-md p-4">
                    <div class="flex flex-wrap items-center gap-4">
                        <span class="text-gray-700 font-medium">分类筛选:</span>
                        <div id="categoryFilters" class="flex flex-wrap gap-2">
                            <button onclick="UserProductsPage.filterByCategory('')"
                                    class="category-btn px-3 py-1 rounded-full bg-blue-500 text-white text-sm">
                                全部
                            </button>
                        </div>
                    </div>
                </div>

                <!-- 排序选项 -->
                <div class="bg-white rounded-lg shadow-md p-4">
                    <div class="flex items-center justify-between">
                        <div class="flex items-center gap-4">
                            <span class="text-gray-700 font-medium">排序:</span>
                            <select id="sortSelect" onchange="UserProductsPage.sortProducts()"
                                    class="border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500">
                                <option value="name-asc">名称 A-Z</option>
                                <option value="name-desc">名称 Z-A</option>
                                <option value="price-asc">价格从低到高</option>
                                <option value="price-desc">价格从高到低</option>
                                <option value="time-desc">最新上架</option>
                            </select>
                        </div>
                        <div class="text-gray-600">
                            共 <span id="productCount" class="font-semibold text-blue-600">0</span> 件商品
                        </div>
                    </div>
                </div>

                <!-- 商品列表 -->
                <div id="productsContainer" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                    <!-- 商品卡片将在这里动态生成 -->
                </div>

                <!-- 空状态 -->
                <div id="emptyState" class="hidden">
                    ${uiManager.createEmptyState('暂无商品')}
                </div>

                <!-- 错误状态 -->
                <div id="errorState" class="hidden">
                    ${uiManager.createErrorState('加载商品失败，请重试', 'UserProductsPage.loadProducts()')}
                </div>
            </div>
        `;

        // 加载商品数据
        await this.loadProducts();
        this.updateCartBadge();
    },

    /**
     * 加载商品数据
     */
    async loadProducts() {
        try {
            uiManager.showLoading();

            const response = await userApi.getProducts();

            if (response.code === 200) {
                appState.updateProducts(response.data);
                this.renderProducts(response.data);
                this.renderCategories(response.data);
            } else {
                throw new Error(response.message);
            }
        } catch (error) {
            console.error('加载商品失败:', error);
            this.showError();
            uiManager.showNotification('加载失败', '无法加载商品数据', 'error');
        } finally {
            uiManager.hideLoading();
        }
    },

    /**
     * 渲染商品列表
     */
    renderProducts(products) {
        const container = document.getElementById('productsContainer');
        const emptyState = document.getElementById('emptyState');
        const errorState = document.getElementById('errorState');
        const productCount = document.getElementById('productCount');

        // 隐藏状态提示
        emptyState.classList.add('hidden');
        errorState.classList.add('hidden');

        if (products.length === 0) {
            container.innerHTML = '';
            emptyState.classList.remove('hidden');
            productCount.textContent = '0';
            return;
        }

        productCount.textContent = products.length;

        container.innerHTML = products.map(product => this.createProductCard(product)).join('');

        // 添加商品卡片事件监听
        this.attachProductEvents();
    },

    /**
     * 创建商品卡片
     */
    createProductCard(product) {
        const isOutOfStock = product.stock <= 0;
        const statusBadge = product.status ?
            '<span class="bg-green-100 text-green-800 text-xs px-2 py-1 rounded">在售</span>' :
            '<span class="bg-red-100 text-red-800 text-xs px-2 py-1 rounded">下架</span>';

        return `
            <div class="bg-white rounded-lg shadow-md card-hover overflow-hidden ${isOutOfStock ? 'opacity-60' : ''}">
                <div class="relative">
                    <img src="https://picsum.photos/seed/${product.id}/300/200.jpg"
                         alt="${product.name}"
                         class="w-full h-48 object-cover">
                    <div class="absolute top-2 right-2">
                        ${statusBadge}
                    </div>
                    ${isOutOfStock ? '<div class="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center"><span class="text-white font-bold">已售罄</span></div>' : ''}
                </div>
                <div class="p-4">
                    <h3 class="font-semibold text-gray-900 mb-2 line-clamp-2">${product.name}</h3>
                    <p class="text-gray-600 text-sm mb-3 line-clamp-2">${product.description}</p>
                    <div class="flex items-center justify-between mb-3">
                        <span class="text-2xl font-bold text-red-600">${uiManager.formatPrice(product.price)}</span>
                        <span class="text-sm text-gray-500">库存: ${product.stock}</span>
                    </div>
                    <div class="flex space-x-2">
                        <button onclick="UserProductsPage.addToCart(${product.id})"
                                class="flex-1 bg-blue-500 hover:bg-blue-600 disabled:bg-gray-300 text-white py-2 px-4 rounded-lg transition ${isOutOfStock || !product.status ? 'disabled cursor-not-allowed' : ''}"
                                ${isOutOfStock || !product.status ? 'disabled' : ''}>
                            <i class="fas fa-cart-plus mr-2"></i>
                            ${isOutOfStock ? '已售罄' : '加入购物车'}
                        </button>
                        <button onclick="UserProductsPage.viewProduct(${product.id})"
                                class="bg-gray-100 hover:bg-gray-200 text-gray-700 py-2 px-4 rounded-lg transition">
                            <i class="fas fa-eye"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
    },

    /**
     * 渲染分类过滤器
     */
    renderCategories(products) {
        const container = document.getElementById('categoryFilters');
        const categories = [...new Set(products.map(p => p.category).filter(Boolean))];

        container.innerHTML = `
            <button onclick="UserProductsPage.filterByCategory('')"
                    class="category-btn px-3 py-1 rounded-full bg-blue-500 text-white text-sm">
                全部
            </button>
            ${categories.map(category => `
                <button onclick="UserProductsPage.filterByCategory('${category}')"
                        class="category-btn px-3 py-1 rounded-full bg-gray-200 text-gray-700 hover:bg-gray-300 text-sm transition">
                    ${category}
                </button>
            `).join('')}
        `;
    },

    /**
     * 按分类筛选
     */
    filterByCategory(category) {
        // 更新按钮状态
        document.querySelectorAll('.category-btn').forEach(btn => {
            if (btn.textContent.trim() === category || (category === '' && btn.textContent.trim() === '全部')) {
                btn.className = 'category-btn px-3 py-1 rounded-full bg-blue-500 text-white text-sm';
            } else {
                btn.className = 'category-btn px-3 py-1 rounded-full bg-gray-200 text-gray-700 hover:bg-gray-300 text-sm transition';
            }
        });

        // 筛选商品
        const allProducts = appState.get('products');
        const filtered = category ? allProducts.filter(p => p.category === category) : allProducts;

        this.renderProducts(filtered);
    },

    /**
     * 排序商品
     */
    sortProducts() {
        const sortValue = document.getElementById('sortSelect').value;
        const [sortBy, sortOrder] = sortValue.split('-');

        const allProducts = appState.get('products');
        const sorted = [...allProducts].sort((a, b) => {
            let aVal, bVal;

            switch (sortBy) {
                case 'name':
                    aVal = a.name || '';
                    bVal = b.name || '';
                    break;
                case 'price':
                    aVal = a.price || 0;
                    bVal = b.price || 0;
                    break;
                case 'time':
                    aVal = new Date(a.createTime || 0);
                    bVal = new Date(b.createTime || 0);
                    break;
                default:
                    return 0;
            }

            if (sortOrder === 'asc') {
                return aVal > bVal ? 1 : -1;
            } else {
                return aVal < bVal ? 1 : -1;
            }
        });

        this.renderProducts(sorted);
    },

    /**
     * 添加到购物车
     */
    async addToCart(productId) {
        try {
            const product = appState.get('products').find(p => p.id === productId);
            if (!product) {
                uiManager.showNotification('错误', '商品不存在', 'error');
                return;
            }

            if (product.stock <= 0 || !product.status) {
                uiManager.showNotification('提示', '商品已售罄或已下架', 'warning');
                return;
            }

            const userId = appState.get('currentUser.id');
            await userApi.addToCart(userId, productId, 1);

            uiManager.showNotification('成功', '商品已添加到购物车', 'success');
            this.updateCartBadge();

        } catch (error) {
            console.error('添加到购物车失败:', error);
            uiManager.showNotification('失败', '添加到购物车失败', 'error');
        }
    },

    /**
     * 查看商品详情
     */
    async viewProduct(productId) {
        try {
            const response = await userApi.getProduct(productId);

            if (response.code === 200) {
                const product = response.data;
                this.showProductModal(product);
            } else {
                throw new Error(response.message);
            }
        } catch (error) {
            console.error('获取商品详情失败:', error);
            uiManager.showNotification('失败', '无法获取商品详情', 'error');
        }
    },

    /**
     * 显示商品详情模态框
     */
    showProductModal(product) {
        const modal = document.createElement('div');
        modal.className = 'fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4';
        modal.innerHTML = `
            <div class="bg-white rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
                <div class="p-6">
                    <div class="flex justify-between items-start mb-4">
                        <h2 class="text-2xl font-bold text-gray-900">${product.name}</h2>
                        <button onclick="this.closest('.fixed').remove()" class="text-gray-500 hover:text-gray-700">
                            <i class="fas fa-times text-xl"></i>
                        </button>
                    </div>

                    <div class="grid md:grid-cols-2 gap-6">
                        <div>
                            <img src="https://picsum.photos/seed/${product.id}/400/300.jpg"
                                 alt="${product.name}"
                                 class="w-full rounded-lg">
                        </div>

                        <div>
                            <div class="mb-4">
                                <span class="text-3xl font-bold text-red-600">${uiManager.formatPrice(product.price)}</span>
                            </div>

                            <div class="space-y-2 mb-4">
                                <div class="flex justify-between">
                                    <span class="text-gray-600">库存:</span>
                                    <span class="font-medium">${product.stock}</span>
                                </div>
                                <div class="flex justify-between">
                                    <span class="text-gray-600">分类:</span>
                                    <span class="font-medium">${product.category}</span>
                                </div>
                                <div class="flex justify-between">
                                    <span class="text-gray-600">状态:</span>
                                    <span class="font-medium ${product.status ? 'text-green-600' : 'text-red-600'}">
                                        ${product.status ? '在售' : '下架'}
                                    </span>
                                </div>
                            </div>

                            <div class="mb-4">
                                <h3 class="font-semibold text-gray-900 mb-2">商品描述</h3>
                                <p class="text-gray-600">${product.description}</p>
                            </div>

                            <button onclick="UserProductsPage.addToCart(${product.id}); this.closest('.fixed').remove();"
                                    class="w-full bg-blue-500 hover:bg-blue-600 disabled:bg-gray-300 text-white py-3 px-4 rounded-lg transition ${product.stock <= 0 || !product.status ? 'disabled cursor-not-allowed' : ''}"
                                    ${product.stock <= 0 || !product.status ? 'disabled' : ''}>
                                <i class="fas fa-cart-plus mr-2"></i>
                                ${product.stock <= 0 || !product.status ? '商品不可用' : '加入购物车'}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        // 点击背景关闭模态框
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.remove();
            }
        });
    },

    /**
     * 刷新商品列表
     */
    refreshProducts() {
        this.loadProducts();
        uiManager.showNotification('提示', '商品列表已刷新', 'info');
    },

    /**
     * 切换到购物车页面
     */
    switchToCart() {
        uiManager.switchUserPage('cart');
    },

    /**
     * 更新购物车徽章
     */
    async updateCartBadge() {
        try {
            const userId = appState.get('currentUser.id');
            const response = await userApi.getCartSummary(userId);

            if (response.code === 200) {
                const itemCount = response.data.itemCount || 0;
                const badge = document.getElementById('cartBadge');

                if (itemCount > 0) {
                    badge.textContent = itemCount > 99 ? '99+' : itemCount;
                    badge.classList.remove('hidden');
                } else {
                    badge.classList.add('hidden');
                }
            }
        } catch (error) {
            console.error('获取购物车信息失败:', error);
        }
    },

    /**
     * 显示错误状态
     */
    showError() {
        document.getElementById('productsContainer').innerHTML = '';
        document.getElementById('emptyState').classList.add('hidden');
        document.getElementById('errorState').classList.remove('hidden');
        document.getElementById('productCount').textContent = '0';
    },

    /**
     * 添加商品卡片事件监听
     */
    attachProductEvents() {
        // 商品卡片事件已通过内联onclick处理，这里可以添加其他事件
    }
};

// 导出到全局作用域
window.UserProductsPage = UserProductsPage;