/**
 * 用户端购物车页面
 */

const UserCartPage = {
    /**
     * 渲染页面
     */
    async render(container) {
        container.innerHTML = `
            <div class="space-y-6">
                <!-- 页面标题 -->
                <div class="flex justify-between items-center">
                    <h1 class="text-2xl font-bold text-gray-900">
                        <i class="fas fa-shopping-cart mr-2 text-green-600"></i>
                        我的购物车
                    </h1>
                    <div class="flex space-x-2">
                        <button onclick="UserCartPage.clearCart()"
                                class="bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded-lg transition">
                            <i class="fas fa-trash mr-2"></i>清空购物车
                        </button>
                        <button onclick="uiManager.switchUserPage('products')"
                                class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg transition">
                            <i class="fas fa-arrow-left mr-2"></i>继续购物
                        </button>
                    </div>
                </div>

                <!-- 购物车内容 -->
                <div class="grid lg:grid-cols-3 gap-6">
                    <!-- 购物车列表 -->
                    <div class="lg:col-span-2">
                        <div class="bg-white rounded-lg shadow-md">
                            <div class="p-4 border-b border-gray-200">
                                <h2 class="text-lg font-semibold text-gray-900">购物车商品</h2>
                            </div>
                            <div id="cartItemsContainer" class="divide-y divide-gray-200">
                                <!-- 购物车商品将在这里动态生成 -->
                            </div>
                        </div>
                    </div>

                    <!-- 订单摘要 -->
                    <div class="lg:col-span-1">
                        <div class="bg-white rounded-lg shadow-md p-6 sticky top-24">
                            <h2 class="text-lg font-semibold text-gray-900 mb-4">订单摘要</h2>

                            <div class="space-y-3 mb-6">
                                <div class="flex justify-between">
                                    <span class="text-gray-600">商品数量:</span>
                                    <span id="totalItems" class="font-medium">0</span>
                                </div>
                                <div class="flex justify-between">
                                    <span class="text-gray-600">商品总价:</span>
                                    <span id="subtotal" class="font-medium">¥0.00</span>
                                </div>
                                <div class="flex justify-between">
                                    <span class="text-gray-600">运费:</span>
                                    <span id="shipping" class="font-medium">¥0.00</span>
                                </div>
                                <div class="border-t pt-3">
                                    <div class="flex justify-between text-lg font-bold">
                                        <span>总计:</span>
                                        <span id="totalAmount" class="text-red-600">¥0.00</span>
                                    </div>
                                </div>
                            </div>

                            <button onclick="UserCartPage.checkout()"
                                    class="w-full bg-green-500 hover:bg-green-600 text-white py-3 px-4 rounded-lg transition font-medium">
                                <i class="fas fa-credit-card mr-2"></i>
                                立即结算
                            </button>

                            <div class="mt-4 text-center">
                                <button onclick="uiManager.switchUserPage('products')"
                                        class="text-blue-600 hover:text-blue-700 text-sm">
                                    <i class="fas fa-arrow-left mr-1"></i>
                                    继续购物
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 空状态 -->
                <div id="emptyState" class="hidden">
                    ${uiManager.createEmptyState('购物车为空，快去添加商品吧！', 'fa-shopping-cart')}
                </div>

                <!-- 错误状态 -->
                <div id="errorState" class="hidden">
                    ${uiManager.createErrorState('加载购物车失败，请重试', 'UserCartPage.loadCart()')}
                </div>
            </div>
        `;

        // 加载购物车数据
        await this.loadCart();
    },

    /**
     * 加载购物车数据
     */
    async loadCart() {
        try {
            uiManager.showLoading();

            const userId = appState.get('currentUser.id');
            const response = await userApi.getCart(userId);

            if (response.code === 200) {
                const cartData = {
                    items: response.data,
                    summary: {
                        totalAmount: 0,
                        itemCount: 0
                    }
                };

                // 获取购物车统计信息
                try {
                    const summaryResponse = await userApi.getCartSummary(userId);
                    if (summaryResponse.code === 200) {
                        cartData.summary = summaryResponse.data;
                    }
                } catch (error) {
                    console.error('获取购物车统计失败:', error);
                }

                appState.updateCart(cartData);
                this.renderCartItems(cartData.items);
                this.updateOrderSummary(cartData.summary);
            } else {
                throw new Error(response.message);
            }
        } catch (error) {
            console.error('加载购物车失败:', error);
            this.showError();
            uiManager.showNotification('加载失败', '无法加载购物车数据', 'error');
        } finally {
            uiManager.hideLoading();
        }
    },

    /**
     * 渲染购物车商品列表
     */
    renderCartItems(items) {
        const container = document.getElementById('cartItemsContainer');
        const emptyState = document.getElementById('emptyState');
        const errorState = document.getElementById('errorState');

        // 隐藏状态提示
        emptyState.classList.add('hidden');
        errorState.classList.add('hidden');

        if (items.length === 0) {
            container.innerHTML = `
                <div class="p-8 text-center">
                    <i class="fas fa-shopping-cart text-gray-300 text-5xl mb-4"></i>
                    <p class="text-gray-500 text-lg mb-4">购物车为空</p>
                    <button onclick="uiManager.switchUserPage('products')"
                            class="bg-blue-500 hover:bg-blue-600 text-white px-6 py-2 rounded-lg transition">
                        <i class="fas fa-shopping-bag mr-2"></i>
                        去购物
                    </button>
                </div>
            `;
            return;
        }

        container.innerHTML = items.map(item => this.createCartItem(item)).join('');

        // 添加事件监听
        this.attachCartItemEvents();
    },

    /**
     * 创建购物车商品项
     */
    createCartItem(item) {
        return `
            <div class="p-4 cart-item" data-id="${item.id}">
                <div class="flex items-center space-x-4">
                    <!-- 商品图片 -->
                    <img src="https://picsum.photos/seed/${item.productId}/100/100.jpg"
                         alt="${item.productName}"
                         class="w-20 h-20 object-cover rounded-lg">

                    <!-- 商品信息 -->
                    <div class="flex-1">
                        <h3 class="font-semibold text-gray-900">${item.productName}</h3>
                        <p class="text-gray-600 text-sm">商品ID: ${item.productId}</p>
                        <p class="text-red-600 font-bold mt-1">${uiManager.formatPrice(item.price)}</p>
                    </div>

                    <!-- 数量控制 -->
                    <div class="flex items-center space-x-2">
                        <button onclick="UserCartPage.updateQuantity(${item.id}, ${item.quantity - 1})"
                                class="w-8 h-8 rounded-full bg-gray-100 hover:bg-gray-200 text-gray-600 transition"
                                ${item.quantity <= 1 ? 'disabled' : ''}>
                            <i class="fas fa-minus text-xs"></i>
                        </button>
                        <span class="w-12 text-center font-medium">${item.quantity}</span>
                        <button onclick="UserCartPage.updateQuantity(${item.id}, ${item.quantity + 1})"
                                class="w-8 h-8 rounded-full bg-gray-100 hover:bg-gray-200 text-gray-600 transition">
                            <i class="fas fa-plus text-xs"></i>
                        </button>
                    </div>

                    <!-- 小计 -->
                    <div class="text-right">
                        <p class="font-bold text-red-600">${uiManager.formatPrice(item.price * item.quantity)}</p>
                        <button onclick="UserCartPage.removeItem(${item.id})"
                                class="text-red-500 hover:text-red-600 text-sm mt-1">
                            <i class="fas fa-trash mr-1"></i>删除
                        </button>
                    </div>
                </div>
            </div>
        `;
    },

    /**
     * 更新订单摘要
     */
    updateOrderSummary(summary) {
        document.getElementById('totalItems').textContent = summary.itemCount || 0;
        document.getElementById('subtotal').textContent = uiManager.formatPrice(summary.totalAmount || 0);

        // 计算运费（满99免运费）
        const shipping = (summary.totalAmount || 0) >= 99 ? 0 : 10;
        document.getElementById('shipping').textContent = uiManager.formatPrice(shipping);

        // 计算总计
        const total = (summary.totalAmount || 0) + shipping;
        document.getElementById('totalAmount').textContent = uiManager.formatPrice(total);
    },

    /**
     * 更新商品数量
     */
    async updateQuantity(cartItemId, newQuantity) {
        if (newQuantity <= 0) {
            this.removeItem(cartItemId);
            return;
        }

        try {
            const userId = appState.get('currentUser.id');
            const response = await userApi.updateCartItem(userId, cartItemId, newQuantity);

            if (response.code === 200) {
                uiManager.showNotification('成功', '数量已更新', 'success');
                await this.loadCart(); // 重新加载购物车
            } else {
                throw new Error(response.message);
            }
        } catch (error) {
            console.error('更新数量失败:', error);
            uiManager.showNotification('失败', '更新数量失败', 'error');
        }
    },

    /**
     * 移除购物车商品
     */
    async removeItem(cartItemId) {
        if (!confirm('确定要删除这个商品吗？')) {
            return;
        }

        try {
            const userId = appState.get('currentUser.id');
            const response = await userApi.removeFromCart(userId, cartItemId);

            if (response.code === 200) {
                uiManager.showNotification('成功', '商品已从购物车移除', 'success');
                await this.loadCart(); // 重新加载购物车
            } else {
                throw new Error(response.message);
            }
        } catch (error) {
            console.error('移除商品失败:', error);
            uiManager.showNotification('失败', '移除商品失败', 'error');
        }
    },

    /**
     * 清空购物车
     */
    async clearCart() {
        if (!confirm('确定要清空购物车吗？此操作不可恢复。')) {
            return;
        }

        try {
            const userId = appState.get('currentUser.id');
            const response = await userApi.clearCart(userId);

            if (response.code === 200) {
                uiManager.showNotification('成功', '购物车已清空', 'success');
                await this.loadCart(); // 重新加载购物车
            } else {
                throw new Error(response.message);
            }
        } catch (error) {
            console.error('清空购物车失败:', error);
            uiManager.showNotification('失败', '清空购物车失败', 'error');
        }
    },

    /**
     * 结算
     */
    checkout() {
        const items = appState.get('cart.items');

        if (items.length === 0) {
            uiManager.showNotification('提示', '购物车为空，无法结算', 'warning');
            return;
        }

        // 模拟结算流程
        this.showCheckoutModal();
    },

    /**
     * 显示结算模态框
     */
    showCheckoutModal() {
        const modal = document.createElement('div');
        modal.className = 'fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4';
        modal.innerHTML = `
            <div class="bg-white rounded-lg max-w-md w-full">
                <div class="p-6">
                    <div class="flex justify-between items-center mb-4">
                        <h2 class="text-xl font-bold text-gray-900">确认订单</h2>
                        <button onclick="this.closest('.fixed').remove()" class="text-gray-500 hover:text-gray-700">
                            <i class="fas fa-times text-xl"></i>
                        </button>
                    </div>

                    <div class="space-y-4">
                        <div class="bg-blue-50 p-4 rounded-lg">
                            <p class="text-blue-800">
                                <i class="fas fa-info-circle mr-2"></i>
                                这是一个演示应用，不会进行真实的支付处理。
                            </p>
                        </div>

                        <div>
                            <h3 class="font-semibold text-gray-900 mb-2">收货信息</h3>
                            <div class="space-y-2">
                                <input type="text" placeholder="收货人姓名" class="w-full border border-gray-300 rounded-lg px-3 py-2">
                                <input type="tel" placeholder="手机号码" class="w-full border border-gray-300 rounded-lg px-3 py-2">
                                <input type="text" placeholder="收货地址" class="w-full border border-gray-300 rounded-lg px-3 py-2">
                            </div>
                        </div>

                        <button onclick="UserCartPage.processCheckout(); this.closest('.fixed').remove();"
                                class="w-full bg-green-500 hover:bg-green-600 text-white py-3 px-4 rounded-lg transition font-medium">
                            <i class="fas fa-check mr-2"></i>
                            确认支付
                        </button>
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
     * 处理结算
     */
    async processCheckout() {
        try {
            uiManager.showLoading();

            // 模拟支付处理
            await new Promise(resolve => setTimeout(resolve, 2000));

            // 清空购物车
            const userId = appState.get('currentUser.id');
            await userApi.clearCart(userId);

            uiManager.showNotification('支付成功', '订单已成功创建！', 'success');

            // 跳转到商品页面
            setTimeout(() => {
                uiManager.switchUserPage('products');
            }, 2000);

        } catch (error) {
            console.error('支付处理失败:', error);
            uiManager.showNotification('支付失败', '支付处理过程中出现错误', 'error');
        } finally {
            uiManager.hideLoading();
        }
    },

    /**
     * 显示错误状态
     */
    showError() {
        document.getElementById('cartItemsContainer').innerHTML = '';
        document.getElementById('emptyState').classList.add('hidden');
        document.getElementById('errorState').classList.remove('hidden');
    },

    /**
     * 添加购物车商品事件监听
     */
    attachCartItemEvents() {
        // 购物车商品事件已通过内联onclick处理
    }
};

// 导出到全局作用域
window.UserCartPage = UserCartPage;