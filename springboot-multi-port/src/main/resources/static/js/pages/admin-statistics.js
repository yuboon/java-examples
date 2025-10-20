/**
 * 管理端统计分析页面
 */

const AdminStatisticsPage = {
    /**
     * 渲染页面
     */
    async render(container) {
        container.innerHTML = `
            <div class="space-y-6">
                <!-- 页面标题 -->
                <div class="flex justify-between items-center">
                    <h1 class="text-2xl font-bold text-gray-900">
                        <i class="fas fa-chart-bar mr-2 text-green-600"></i>
                        统计分析
                    </h1>
                    <div class="flex space-x-2">
                        <button onclick="AdminStatisticsPage.refreshData()"
                                class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg transition">
                            <i class="fas fa-sync-alt mr-2"></i>刷新数据
                        </button>
                    </div>
                </div>

                <!-- 概览卡片 -->
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    <div class="bg-white rounded-lg shadow-md p-6 border-l-4 border-blue-500">
                        <div class="flex items-center justify-between">
                            <div>
                                <p class="text-gray-600 text-sm">商品总数</p>
                                <p id="totalProducts" class="text-3xl font-bold text-gray-900">0</p>
                            </div>
                            <div class="bg-blue-100 rounded-full p-3">
                                <i class="fas fa-box text-blue-600 text-xl"></i>
                            </div>
                        </div>
                    </div>

                    <div class="bg-white rounded-lg shadow-md p-6 border-l-4 border-green-500">
                        <div class="flex items-center justify-between">
                            <div>
                                <p class="text-gray-600 text-sm">上架商品</p>
                                <p id="activeProducts" class="text-3xl font-bold text-gray-900">0</p>
                            </div>
                            <div class="bg-green-100 rounded-full p-3">
                                <i class="fas fa-check-circle text-green-600 text-xl"></i>
                            </div>
                        </div>
                    </div>

                    <div class="bg-white rounded-lg shadow-md p-6 border-l-4 border-yellow-500">
                        <div class="flex items-center justify-between">
                            <div>
                                <p class="text-gray-600 text-sm">总库存</p>
                                <p id="totalStock" class="text-3xl font-bold text-gray-900">0</p>
                            </div>
                            <div class="bg-yellow-100 rounded-full p-3">
                                <i class="fas fa-warehouse text-yellow-600 text-xl"></i>
                            </div>
                        </div>
                    </div>

                    <div class="bg-white rounded-lg shadow-md p-6 border-l-4 border-purple-500">
                        <div class="flex items-center justify-between">
                            <div>
                                <p class="text-gray-600 text-sm">商品分类</p>
                                <p id="totalCategories" class="text-3xl font-bold text-gray-900">0</p>
                            </div>
                            <div class="bg-purple-100 rounded-full p-3">
                                <i class="fas fa-tags text-purple-600 text-xl"></i>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 详细数据表格 -->
                <div class="bg-white rounded-lg shadow-md">
                    <div class="p-4 border-b border-gray-200">
                        <h3 class="text-lg font-semibold text-gray-900">
                            <i class="fas fa-table mr-2 text-purple-600"></i>
                            分类详细信息
                        </h3>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="min-w-full divide-y divide-gray-200">
                            <thead class="bg-gray-50">
                                <tr>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        分类名称
                                    </th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        商品数量
                                    </th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        上架商品
                                    </th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                        总库存
                                    </th>
                                </tr>
                            </thead>
                            <tbody id="categoryTableBody" class="bg-white divide-y divide-gray-200">
                                <!-- 分类数据将在这里动态生成 -->
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- 空状态 -->
                <div id="emptyState" class="hidden">
                    ${uiManager.createEmptyState('暂无统计数据')}
                </div>

                <!-- 错误状态 -->
                <div id="errorState" class="hidden">
                    ${uiManager.createErrorState('加载统计数据失败，请重试', 'AdminStatisticsPage.loadData()')}
                </div>
            </div>
        `;

        // 加载数据
        await this.loadData();
    },

    /**
     * 加载统计数据
     */
    async loadData() {
        try {
            uiManager.showLoading();

            const response = await adminApi.getProductStatistics();

            if (response.code === 200) {
                const statistics = response.data;
                this.renderOverviewCards(statistics);
                this.renderCategoryTable(statistics);
                appState.updateAdminData('statistics', statistics);
            } else {
                throw new Error(response.message);
            }
        } catch (error) {
            console.error('加载统计数据失败:', error);
            this.showError();
            uiManager.showNotification('加载失败', '无法加载统计数据', 'error');
        } finally {
            uiManager.hideLoading();
        }
    },

    /**
     * 渲染概览卡片
     */
    renderOverviewCards(statistics) {
        document.getElementById('totalProducts').textContent = statistics.totalProducts || 0;
        document.getElementById('activeProducts').textContent = statistics.activeProducts || 0;
        document.getElementById('totalStock').textContent = statistics.totalStock || 0;
        document.getElementById('totalCategories').textContent = statistics.categories?.length || 0;
    },

    /**
     * 渲染分类表格
     */
    renderCategoryTable(statistics) {
        const tbody = document.getElementById('categoryTableBody');
        const categories = statistics.categories || [];

        if (categories.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center py-8 text-gray-400">暂无分类数据</td></tr>';
            return;
        }

        tbody.innerHTML = categories.map(category => `
            <tr class="hover:bg-gray-50">
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    ${category.name}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    ${category.count || 0}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    ${category.activeCount || 0}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    ${category.totalStock || 0}
                </td>
            </tr>
        `).join('');
    },

    /**
     * 刷新数据
     */
    async refreshData() {
        await this.loadData();
        uiManager.showNotification('成功', '统计数据已刷新', 'success');
    },

    /**
     * 显示错误状态
     */
    showError() {
        document.getElementById('emptyState').classList.add('hidden');
        document.getElementById('errorState').classList.remove('hidden');
    }
};

// 导出到全局作用域
window.AdminStatisticsPage = AdminStatisticsPage;