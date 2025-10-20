/**
 * 管理端商品管理页面
 */

const AdminProductsPage = {
    currentPage: 1,
    pageSize: 10,
    totalItems: 0,
    allProducts: [],

    /**
     * 渲染页面
     */
    async render(container) {
        container.innerHTML = `
            <div class="space-y-6">
                <!-- 页面标题和操作 -->
                <div class="flex justify-between items-center">
                    <h1 class="text-2xl font-bold text-gray-900">
                        <i class="fas fa-box mr-2 text-blue-600"></i>
                        商品管理
                    </h1>
                    <div class="flex space-x-2">
                        <button onclick="AdminProductsPage.showCreateModal()"
                                class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg transition">
                            <i class="fas fa-plus mr-2"></i>添加商品
                        </button>
                    </div>
                </div>

                <!-- 搜索和过滤器 -->
                <div class="bg-white rounded-lg shadow-md p-4">
                    <div class="grid md:grid-cols-4 gap-4">
                        <div>
                            <input type="text" id="searchInput" placeholder="搜索商品名称..."
                                   class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500">
                        </div>
                        <div>
                            <select id="categoryFilter" class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500">
                                <option value="">所有分类</option>
                            </select>
                        </div>
                        <div>
                            <select id="statusFilter" class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500">
                                <option value="">所有状态</option>
                                <option value="true">上架</option>
                                <option value="false">下架</option>
                            </select>
                        </div>
                        <div>
                            <button onclick="AdminProductsPage.filterProducts()"
                                    class="w-full bg-blue-500 hover:bg-blue-600 text-white py-2 px-4 rounded-lg transition">
                                <i class="fas fa-search mr-2"></i>搜索
                            </button>
                        </div>
                    </div>
                </div>

                <!-- 商品表格 -->
                <div class="bg-white rounded-lg shadow-md overflow-hidden">
                    <div class="overflow-x-auto">
                        <table class="min-w-full divide-y divide-gray-200">
                            <thead class="bg-gray-50">
                                <tr>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        商品信息
                                    </th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        分类
                                    </th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        价格
                                    </th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        库存
                                    </th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        状态
                                    </th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        操作
                                    </th>
                                </tr>
                            </thead>
                            <tbody id="productsTableBody" class="bg-white divide-y divide-gray-200">
                                <!-- 商品数据将在这里动态生成 -->
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- 空状态 -->
                <div id="emptyState" class="hidden">
                    ${uiManager.createEmptyState('暂无商品数据')}
                </div>

                <!-- 错误状态 -->
                <div id="errorState" class="hidden">
                    ${uiManager.createErrorState('加载商品数据失败，请重试', 'AdminProductsPage.loadProducts()')}
                </div>
            </div>
        `;

        // 加载数据
        await this.loadProducts();
        this.setupEventListeners();
    },

    /**
     * 设置事件监听器
     */
    setupEventListeners() {
        // 搜索框回车事件
        document.getElementById('searchInput').addEventListener('keyup', (e) => {
            if (e.key === 'Enter') {
                this.filterProducts();
            }
        });

        // 防抖搜索
        document.getElementById('searchInput').addEventListener('input',
            uiManager.debounce(() => this.filterProducts(), 500)
        );
    },

    /**
     * 加载商品数据
     */
    async loadProducts() {
        try {
            uiManager.showLoading();

            const response = await adminApi.getAllProducts();

            if (response.code === 200) {
                this.allProducts = response.data;
                this.totalItems = this.allProducts.length;
                this.renderProducts(this.allProducts);
                this.updateCategories(this.allProducts);
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
     * 渲染商品表格
     */
    renderProducts(products) {
        const tbody = document.getElementById('productsTableBody');
        const emptyState = document.getElementById('emptyState');
        const errorState = document.getElementById('errorState');

        // 隐藏状态提示
        emptyState.classList.add('hidden');
        errorState.classList.add('hidden');

        if (products.length === 0) {
            tbody.innerHTML = '';
            emptyState.classList.remove('hidden');
            return;
        }

        tbody.innerHTML = products.map(product => this.createProductRow(product)).join('');
    },

    /**
     * 创建商品行
     */
    createProductRow(product) {
        const statusBadge = product.status ?
            '<span class="bg-green-100 text-green-800 text-xs px-2 py-1 rounded">上架</span>' :
            '<span class="bg-red-100 text-red-800 text-xs px-2 py-1 rounded">下架</span>';

        const stockStatus = product.stock > 20 ? 'text-green-600' :
                           product.stock > 0 ? 'text-yellow-600' : 'text-red-600';

        return `
            <tr class="hover:bg-gray-50">
                <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex items-center">
                        <img src="https://picsum.photos/seed/${product.id}/50/50.jpg"
                             alt="${product.name}"
                             class="w-10 h-10 rounded-lg mr-3">
                        <div>
                            <div class="text-sm font-medium text-gray-900">${product.name}</div>
                            <div class="text-sm text-gray-500">ID: ${product.id}</div>
                        </div>
                    </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    ${product.category || '-'}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    <span class="font-semibold text-red-600">${uiManager.formatPrice(product.price)}</span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm">
                    <span class="${stockStatus} font-medium">${product.stock}</span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${statusBadge}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <div class="flex space-x-2">
                        <button onclick="AdminProductsPage.viewProduct(${product.id})"
                                class="text-blue-600 hover:text-blue-900">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button onclick="AdminProductsPage.editProduct(${product.id})"
                                class="text-green-600 hover:text-green-900">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button onclick="AdminProductsPage.toggleStatus(${product.id})"
                                class="text-yellow-600 hover:text-yellow-900">
                            <i class="fas fa-power-off"></i>
                        </button>
                        <button onclick="AdminProductsPage.deleteProduct(${product.id})"
                                class="text-red-600 hover:text-red-900">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    },

    /**
     * 更新分类过滤器
     */
    updateCategories(products) {
        const categories = [...new Set(products.map(p => p.category).filter(Boolean))];
        const select = document.getElementById('categoryFilter');

        select.innerHTML = `
            <option value="">所有分类</option>
            ${categories.map(category => `
                <option value="${category}">${category}</option>
            `).join('')}
        `;
    },

    /**
     * 过滤商品
     */
    filterProducts() {
        const keyword = document.getElementById('searchInput').value.toLowerCase().trim();
        const category = document.getElementById('categoryFilter').value;
        const status = document.getElementById('statusFilter').value;

        let filtered = this.allProducts;

        if (keyword) {
            filtered = filtered.filter(p =>
                p.name.toLowerCase().includes(keyword) ||
                p.description.toLowerCase().includes(keyword)
            );
        }

        if (category) {
            filtered = filtered.filter(p => p.category === category);
        }

        if (status !== '') {
            filtered = filtered.filter(p => p.status.toString() === status);
        }

        this.renderProducts(filtered);
    },

    /**
     * 查看商品详情
     */
    async viewProduct(id) {
        try {
            const response = await adminApi.getProduct(id);

            if (response.code === 200) {
                this.showProductModal(response.data, 'view');
            } else {
                throw new Error(response.message);
            }
        } catch (error) {
            console.error('获取商品详情失败:', error);
            uiManager.showNotification('失败', '无法获取商品详情', 'error');
        }
    },

    /**
     * 编辑商品
     */
    async editProduct(id) {
        try {
            const response = await adminApi.getProduct(id);

            if (response.code === 200) {
                this.showProductModal(response.data, 'edit');
            } else {
                throw new Error(response.message);
            }
        } catch (error) {
            console.error('获取商品信息失败:', error);
            uiManager.showNotification('失败', '无法获取商品信息', 'error');
        }
    },

    /**
     * 切换商品状态
     */
    async toggleStatus(id) {
        const product = this.allProducts.find(p => p.id === id);
        if (!product) return;

        const action = product.status ? '下架' : '上架';
        if (!confirm(`确定要${action}商品 "${product.name}" 吗？`)) {
            return;
        }

        try {
            const response = await adminApi.updateProductStatus(id, !product.status);

            if (response.code === 200) {
                uiManager.showNotification('成功', `商品已${action}`, 'success');
                await this.loadProducts();
            } else {
                throw new Error(response.message);
            }
        } catch (error) {
            console.error('更新状态失败:', error);
            uiManager.showNotification('失败', '更新状态失败', 'error');
        }
    },

    /**
     * 删除商品
     */
    async deleteProduct(id) {
        const product = this.allProducts.find(p => p.id === id);
        if (!product) return;

        if (!confirm(`确定要删除商品 "${product.name}" 吗？此操作不可恢复！`)) {
            return;
        }

        try {
            const response = await adminApi.deleteProduct(id);

            if (response.code === 200) {
                uiManager.showNotification('成功', '商品已删除', 'success');
                await this.loadProducts();
            } else {
                throw new Error(response.message);
            }
        } catch (error) {
            console.error('删除商品失败:', error);
            uiManager.showNotification('失败', '删除商品失败', 'error');
        }
    },

    /**
     * 显示创建商品模态框
     */
    showCreateModal() {
        this.showProductModal(null, 'create');
    },

    /**
     * 显示商品模态框
     */
    showProductModal(product, mode) {
        const isCreate = mode === 'create';
        const isEdit = mode === 'edit';
        const isView = mode === 'view';

        const modal = document.createElement('div');
        modal.className = 'fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4';

        const title = isCreate ? '添加商品' : isEdit ? '编辑商品' : '商品详情';
        const disabled = isView ? 'disabled' : '';

        modal.innerHTML = `
            <div class="bg-white rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
                <div class="p-6">
                    <div class="flex justify-between items-center mb-6">
                        <h2 class="text-2xl font-bold text-gray-900">${title}</h2>
                        <button onclick="this.closest('.fixed').remove()" class="text-gray-500 hover:text-gray-700">
                            <i class="fas fa-times text-xl"></i>
                        </button>
                    </div>

                    <form id="productForm" class="space-y-4">
                        <div class="grid md:grid-cols-2 gap-4">
                            <div>
                                <label class="block text-sm font-medium text-gray-700 mb-1">商品名称</label>
                                <input type="text" id="productName" value="${product?.name || ''}"
                                       class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                       ${disabled} required>
                            </div>
                            <div>
                                <label class="block text-sm font-medium text-gray-700 mb-1">商品分类</label>
                                <input type="text" id="productCategory" value="${product?.category || ''}"
                                       class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                       ${disabled}>
                            </div>
                        </div>

                        <div>
                            <label class="block text-sm font-medium text-gray-700 mb-1">商品描述</label>
                            <textarea id="productDescription" rows="3"
                                      class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                      ${disabled}>${product?.description || ''}</textarea>
                        </div>

                        <div class="grid md:grid-cols-2 gap-4">
                            <div>
                                <label class="block text-sm font-medium text-gray-700 mb-1">价格 (元)</label>
                                <input type="number" id="productPrice" value="${product?.price || ''}" step="0.01" min="0"
                                       class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                       ${disabled} required>
                            </div>
                            <div>
                                <label class="block text-sm font-medium text-gray-700 mb-1">库存数量</label>
                                <input type="number" id="productStock" value="${product?.stock || ''}" min="0"
                                       class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                       ${disabled} required>
                            </div>
                        </div>

                        <div>
                            <label class="flex items-center">
                                <input type="checkbox" id="productStatus" ${product?.status ? 'checked' : ''}
                                       class="rounded border-gray-300 text-blue-600 focus:ring-blue-500" ${disabled}>
                                <span class="ml-2 text-sm text-gray-700">商品上架</span>
                            </label>
                        </div>

                        ${!isView ? `
                            <div class="flex justify-end space-x-3 pt-4 border-t">
                                <button type="button" onclick="this.closest('.fixed').remove()"
                                        class="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50">
                                    取消
                                </button>
                                <button type="submit"
                                        class="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg">
                                    ${isCreate ? '创建' : '保存'}
                                </button>
                            </div>
                        ` : ''}
                    </form>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        // 表单提交处理
        if (!isView) {
            const form = document.getElementById('productForm');
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                this.saveProduct(product?.id, mode);
            });
        }

        // 点击背景关闭模态框
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.remove();
            }
        });
    },

    /**
     * 保存商品
     */
    async saveProduct(id, mode) {
        const formData = {
            name: document.getElementById('productName').value,
            category: document.getElementById('productCategory').value,
            description: document.getElementById('productDescription').value,
            price: parseFloat(document.getElementById('productPrice').value),
            stock: parseInt(document.getElementById('productStock').value),
            status: document.getElementById('productStatus').checked
        };

        // 基本验证
        if (!formData.name || formData.price <= 0 || formData.stock < 0) {
            uiManager.showNotification('验证失败', '请填写完整的商品信息', 'error');
            return;
        }

        try {
            let response;
            if (mode === 'create') {
                response = await adminApi.createProduct(formData);
            } else {
                response = await adminApi.updateProduct(id, formData);
            }

            if (response.code === 200) {
                const action = mode === 'create' ? '创建' : '更新';
                uiManager.showNotification('成功', `商品${action}成功`, 'success');
                document.querySelector('.fixed').remove();
                await this.loadProducts();
            } else {
                throw new Error(response.message);
            }
        } catch (error) {
            console.error('保存商品失败:', error);
            uiManager.showNotification('失败', '保存商品失败', 'error');
        }
    },

    /**
     * 显示错误状态
     */
    showError() {
        document.getElementById('productsTableBody').innerHTML = '';
        document.getElementById('emptyState').classList.add('hidden');
        document.getElementById('errorState').classList.remove('hidden');
    }
};

// 导出到全局作用域
window.AdminProductsPage = AdminProductsPage;