class BIReportPlatform {
    constructor() {
        this.API_BASE = 'http://localhost:8080/report';
        this.currentDatabase = '';
        this.currentTable = '';
        this.chartType = 'table';
        this.chartInstance = null;
        this.currentData = [];
        this.currentLimit = 100;
        this.xAxisDimension = '';
        this.groupDimensions = [];
        this.enableTableGrouping = false;

        this.init();
    }

    init() {
        this.setupEventListeners();
        this.setupDragAndDrop();
        this.loadDatabases();
        this.updateConnectionStatus();

        // 默认选中表格图表类型
        setTimeout(() => {
            this.selectChartType('table');
        }, 100);
    }

    setupEventListeners() {
        // Database selection
        document.getElementById('databaseSelect').addEventListener('change', (e) => {
            this.currentDatabase = e.target.value;
            if (this.currentDatabase) {
                this.loadTables(this.currentDatabase);
            } else {
                this.clearTables();
            }
        });

        // Table selection
        document.getElementById('tableSelect').addEventListener('change', (e) => {
            this.currentTable = e.target.value;
            if (this.currentTable) {
                this.loadColumns(this.currentDatabase, this.currentTable);
            } else {
                this.clearColumns();
            }
        });

        // Chart type buttons
        document.querySelectorAll('.chart-type-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.selectChartType(e.target.dataset.type);
            });
        });

        // Query button
        document.getElementById('queryBtn').addEventListener('click', () => {
            this.executeQuery();
        });
    }

    setupDragAndDrop() {
        const availableFields = document.getElementById('availableFields');
        const dimensions = document.getElementById('dimensions');
        const metrics = document.getElementById('metrics');

        // Make containers sortable
        [availableFields, dimensions, metrics].forEach(container => {
            new Sortable(container, {
                group: 'fields',
                animation: 150,
                ghostClass: 'sortable-ghost',
                chosenClass: 'sortable-chosen',
                onEnd: (evt) => {
                    this.handleFieldDrop(evt);
                }
            });
        });
    }

    handleFieldDrop(evt) {
        const field = evt.item;
        const fromContainer = evt.from;
        const toContainer = evt.to;

        // 添加拖拽动画效果
        field.classList.add('slide-in');

        // Remove placeholder text if this is the first item
        if (toContainer.children.length === 1) {
            const placeholder = toContainer.querySelector('.text-gray-400, .text-gray-500');
            if (placeholder) {
                placeholder.remove();
            }
        }

        // Add placeholder back if container is empty
        if (fromContainer.children.length === 0) {
            const placeholderText = fromContainer.id === 'dimensions' ?
                '拖拽维度字段到此处' : '拖拽指标字段到此处';
            const placeholder = document.createElement('p');
            placeholder.className = 'text-gray-400 text-sm text-center';
            placeholder.textContent = placeholderText;
            fromContainer.appendChild(placeholder);
        }

        // Style field items with enhanced design
        if (toContainer.id !== 'availableFields') {
            // 根据容器类型设置不同的样式
            const isDimension = toContainer.id === 'dimensions';
            const colorClass = isDimension ? 'blue' : 'green';

            field.className = `field-tag bg-${colorClass}-100 border border-${colorClass}-300 rounded-lg px-3 py-2 text-sm cursor-move flex justify-between items-center hover:shadow-md`;

            // Add remove button with better styling
            if (!field.querySelector('.remove-btn')) {
                const removeBtn = document.createElement('button');
                removeBtn.className = `remove-btn ml-2 w-5 h-5 rounded-full bg-${colorClass}-500 text-white hover:bg-${colorClass}-600 flex items-center justify-center transition-colors`;
                removeBtn.innerHTML = `
                    <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                `;
                removeBtn.title = '移除字段';
                removeBtn.onclick = (e) => {
                    e.stopPropagation();
                    this.removeFieldWithAnimation(field, fromContainer);
                };
                field.appendChild(removeBtn);
            }
        } else {
            // 返回到可用字段区域的样式
            field.className = 'field-tag bg-gray-100 border border-gray-300 rounded-lg px-3 py-2 text-sm cursor-move hover:bg-gray-200';
            const removeBtn = field.querySelector('.remove-btn');
            if (removeBtn) {
                removeBtn.remove();
            }
        }

        // 移除动画类
        setTimeout(() => {
            field.classList.remove('slide-in');
        }, 300);
    }

    checkEmptyContainers() {
        ['dimensions', 'metrics'].forEach(containerId => {
            const container = document.getElementById(containerId);
            // 排除占位符元素
            const fields = Array.from(container.children).filter(child =>
                !child.classList.contains('text-gray-400') && !child.classList.contains('text-gray-500')
            );

            if (fields.length === 0) {
                const placeholderText = containerId === 'dimensions' ?
                    '拖拽维度字段到此处' : '拖拽指标字段到此处';
                const placeholder = document.createElement('p');
                placeholder.className = 'text-gray-400 text-sm text-center';
                placeholder.textContent = placeholderText;
                container.appendChild(placeholder);
            }
        });
    }

    removeFieldWithAnimation(field, targetContainer) {
        // 添加移除动画
        field.style.transform = 'scale(0.8)';
        field.style.opacity = '0';
        field.style.transition = 'all 0.2s ease-out';

        setTimeout(() => {
            field.remove();
            this.checkEmptyContainers();
        }, 200);
    }

    async loadDatabases() {
        try {
            const response = await fetch(`${this.API_BASE}/databases`);
            const databases = await response.json();

            const select = document.getElementById('databaseSelect');
            select.innerHTML = '<option value="">选择数据库...</option>';

            databases.forEach(db => {
                const option = document.createElement('option');
                option.value = db;
                option.textContent = db;
                select.appendChild(option);
            });
        } catch (error) {
            console.error('Error loading databases:', error);
            this.showError('加载数据库列表失败');
        }
    }

    async loadTables(database) {
        try {
            this.showLoading(true);
            const response = await fetch(`${this.API_BASE}/tables/${database}`);
            const tables = await response.json();

            const select = document.getElementById('tableSelect');
            select.innerHTML = '<option value="">选择数据表...</option>';
            select.disabled = false;

            tables.forEach(table => {
                const option = document.createElement('option');
                option.value = table.tableName;
                option.textContent = `${table.tableName} (${table.columnCount}列)`;
                select.appendChild(option);
            });
        } catch (error) {
            console.error('Error loading tables:', error);
            this.showError('加载数据表列表失败');
        } finally {
            this.showLoading(false);
        }
    }

    async loadColumns(database, table) {
        try {
            this.showLoading(true);
            const response = await fetch(`${this.API_BASE}/columns/${database}/${table}`);
            const columns = await response.json();

            const container = document.getElementById('availableFields');
            container.innerHTML = '';

            columns.forEach((column, index) => {
                const field = document.createElement('div');
                field.className = 'field-tag bg-white border border-gray-200 rounded-lg px-3 py-2 text-sm cursor-move hover:shadow-md hover:border-blue-300';
                field.draggable = true;
                field.dataset.field = column.columnName;
                field.dataset.type = column.dataType;

                // 根据数据类型设置图标
                const typeIcon = this.getTypeIcon(column.dataType);
                const typeColor = this.getTypeColor(column.dataType);

                field.innerHTML = `
                    <div class="flex justify-between items-center">
                        <div class="flex items-center flex-1 min-w-0">
                            <span class="text-${typeColor}-500 mr-2">${typeIcon}</span>
                            <span class="font-medium text-gray-800 truncate">${column.columnName}</span>
                        </div>
                        <span class="text-xs text-${typeColor}-600 bg-${typeColor}-50 px-2 py-1 rounded-full ml-2">${column.dataType}</span>
                    </div>
                    ${column.columnComment ? `<div class="text-xs text-gray-500 mt-1 ml-6 truncate" title="${column.columnComment}">${column.columnComment}</div>` : ''}
                `;

                // 添加动画延迟
                field.style.animationDelay = `${index * 50}ms`;
                field.classList.add('slide-in');

                container.appendChild(field);
            });
        } catch (error) {
            console.error('Error loading columns:', error);
            this.showError('加载字段列表失败');
        } finally {
            this.showLoading(false);
        }
    }

    clearTables() {
        const tableSelect = document.getElementById('tableSelect');
        tableSelect.innerHTML = '<option value="">选择数据表...</option>';
        tableSelect.disabled = true;
        this.clearColumns();
    }

    clearColumns() {
        const container = document.getElementById('availableFields');
        container.innerHTML = `
            <div class="flex items-center justify-center h-full">
                <div class="text-center">
                    <svg class="w-12 h-12 mx-auto mb-2 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
                    </svg>
                    <p class="text-gray-400 text-center">请先选择数据表</p>
                </div>
            </div>
        `;
        this.clearQueryFields();
    }

    clearQueryFields() {
        ['dimensions', 'metrics'].forEach(containerId => {
            const container = document.getElementById(containerId);
            const placeholderText = containerId === 'dimensions' ?
                '拖拽维度字段到此处' : '拖拽指标字段到此处';
            container.innerHTML = `<p class="text-gray-400 text-sm text-center">${placeholderText}</p>`;
        });
    }

    selectChartType(type) {
        this.chartType = type;

        // Update button styles - 修复文字颜色问题
        document.querySelectorAll('.chart-type-btn').forEach(btn => {
            btn.classList.remove('bg-blue-600', 'text-white', 'border-blue-600');
            btn.classList.add('border-gray-200', 'text-gray-700', 'bg-white');
        });

        const selectedBtn = document.querySelector(`[data-type="${type}"]`);
        if (selectedBtn) {
            selectedBtn.classList.remove('border-gray-200', 'text-gray-700', 'bg-white');
            selectedBtn.classList.add('bg-blue-600', 'text-white', 'border-blue-600');
        }

        // Show/hide dimension control panel and table controls
        const dimensionControl = document.getElementById('dimensionControl');
        const tableControls = document.getElementById('tableControls');

        if (type === 'bar' || type === 'line' || type === 'pie') {
            dimensionControl.classList.remove('hidden');
            tableControls.classList.add('hidden');
            this.updateDimensionControl();
        } else if (type === 'table') {
            dimensionControl.classList.add('hidden');
            tableControls.classList.remove('hidden');
            // Setup table controls after the element is shown
            setTimeout(() => this.setupTableControls(), 0);
        }
    }

    updateDimensionControl() {
        const dimensions = this.getSelectedFields('dimensions');
        const xAxisSelect = document.getElementById('xAxisDimension');

        // Update X-axis dimension options
        xAxisSelect.innerHTML = '';
        dimensions.forEach(dim => {
            const option = document.createElement('option');
            option.value = dim;
            option.textContent = dim;
            xAxisSelect.appendChild(option);
        });

        // If dimensions changed, reset selections
        if (dimensions.length > 0) {
            if (!dimensions.includes(this.xAxisDimension)) {
                this.xAxisDimension = dimensions[0];
                xAxisSelect.value = this.xAxisDimension;
                this.updateGroupDimensions();
            }
        } else {
            this.xAxisDimension = '';
            this.groupDimensions = [];
            document.getElementById('groupDimensions').innerHTML = '<p class="text-gray-500 text-sm">请先选择X轴维度</p>';
        }

        // Bind X-axis selection event
        xAxisSelect.onchange = (e) => {
            this.xAxisDimension = e.target.value;
            this.updateGroupDimensions();
            if (this.currentData.length > 0) {
                this.renderChart(this.currentData);
            }
        };
    }

    updateGroupDimensions() {
        const dimensions = this.getSelectedFields('dimensions');
        const groupContainer = document.getElementById('groupDimensions');
        this.groupDimensions = dimensions.filter(d => d !== this.xAxisDimension);

        groupContainer.innerHTML = '';
        if (this.groupDimensions.length === 0) {
            groupContainer.innerHTML = '<p class="text-gray-500 text-sm">无其他维度</p>';
            return;
        }

        this.groupDimensions.forEach(dim => {
            const label = document.createElement('label');
            label.className = 'flex items-center text-sm text-gray-700';
            label.innerHTML = `
                <input type="checkbox" value="${dim}" class="mr-2" checked>
                ${dim}
            `;
            groupContainer.appendChild(label);

            // Bind change event
            const checkbox = label.querySelector('input');
            checkbox.onchange = () => {
                const checkedDims = Array.from(groupContainer.querySelectorAll('input:checked'))
                    .map(cb => cb.value);
                this.groupDimensions = checkedDims;
                if (this.currentData.length > 0) {
                    this.renderChart(this.currentData);
                }
            };
        });
    }

    async executeQuery() {
        if (!this.currentDatabase || !this.currentTable) {
            this.showError('请先选择数据库和数据表');
            return;
        }

        const dimensions = this.getSelectedFields('dimensions');
        const metrics = this.getSelectedFields('metrics');

        if (dimensions.length === 0 && metrics.length === 0) {
            this.showError('请至少选择一个维度或指标字段');
            return;
        }

        const queryRequest = {
            tableName: this.currentTable,
            dimensions: dimensions,
            metrics: metrics,
            filters: [],
            limit: this.currentLimit
        };

        try {
            this.showLoading(true);
            const response = await fetch(`${this.API_BASE}/query/${this.currentDatabase}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(queryRequest)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            this.currentData = data;

            // Update dimension control when data changes
            this.updateDimensionControl();

            this.renderChart(data);

            // 显示成功提示
            if (data && data.length > 0) {
                this.showSuccess(`查询成功，返回 ${data.length} 条数据`);
            } else {
                this.showSuccess('查询成功，但无数据返回');
            }
        } catch (error) {
            console.error('Error executing query:', error);
            this.showError('查询执行失败: ' + error.message);
        } finally {
            this.showLoading(false);
        }
    }

    getSelectedFields(containerId) {
        const container = document.getElementById(containerId);
        const fields = Array.from(container.querySelectorAll('[data-field]'));
        return fields.map(field => field.dataset.field);
    }

    renderChart(data) {
        const container = document.getElementById('chartContainer');

        // 清空容器内容
        container.innerHTML = '';
        container.className = 'w-full';

        if (this.chartType === 'table') {
            this.renderTable(container, data);
        } else {
            this.renderEChart(container, data);
        }
    }

    renderTable(container, data) {
        if (data.length === 0) {
            container.innerHTML = `
                <div class="flex items-center justify-center h-96">
                    <div class="text-center">
                        <svg class="w-16 h-16 mx-auto mb-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"></path>
                        </svg>
                        <p class="text-gray-500 text-lg">查询结果为空</p>
                        <p class="text-gray-400 text-sm mt-1">请调整查询条件后重试</p>
                    </div>
                </div>
            `;
            return;
        }

        const rowCount = data.length;
        const isLimited = this.currentLimit <= 500 && rowCount === this.currentLimit;
        const dimensions = this.getSelectedFields('dimensions');
        const metrics = this.getSelectedFields('metrics');

        // Prepare grouped data if enabled
        let displayData = data;
        let grouped = false;

        if (this.enableTableGrouping && dimensions.length >= 1) {
            grouped = true;
            displayData = this.groupDataForTable(data, dimensions, metrics);

            // If grouping resulted in empty data, fall back to normal view
            if (displayData.length === 0) {
                grouped = false;
                displayData = data;
            }
        }

        // If still empty after grouping, show empty message
        if (!displayData || displayData.length === 0) {
            container.innerHTML = `
                <div class="flex items-center justify-center h-96">
                    <div class="text-center">
                        <p class="text-gray-500 text-lg">查询结果为空</p>
                        <p class="text-gray-400 text-sm mt-1">请调整查询条件后重试</p>
                    </div>
                </div>
            `;
            return;
        }

        // Determine columns based on data structure
        let columns = [];
        if (grouped) {
            // For grouped view, show all original columns
            // We'll handle grouping in the rendering logic
            if (displayData.length > 0 && displayData[0].details.length > 0) {
                columns = Object.keys(displayData[0].details[0]);
            }
        } else {
            // For normal view, show all columns from first row
            columns = Object.keys(displayData[0]);
        }

        // Create main result container
        const mainDiv = document.createElement('div');
        mainDiv.className = 'space-y-4';

        // Create table container with proper height
        const tableContainer = document.createElement('div');
        tableContainer.className = 'border border-gray-200 rounded-lg overflow-hidden bg-white';
        tableContainer.style.maxHeight = '500px';

        const table = document.createElement('table');
        table.className = 'min-w-full divide-y divide-gray-200';

        // Create header
        const thead = document.createElement('thead');
        thead.className = 'bg-gray-50 sticky top-0 z-10';
        const headerRow = document.createElement('tr');

        columns.forEach(key => {
            const th = document.createElement('th');
            th.className = 'px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider border-b border-gray-200';
            th.textContent = key;
            headerRow.appendChild(th);
        });

        thead.appendChild(headerRow);
        table.appendChild(thead);

        // Create body
        const tbody = document.createElement('tbody');
        tbody.className = 'bg-white divide-y divide-gray-200';

        if (grouped) {
            // Render merged cell table
            this.renderMergedCellTable(tbody, displayData, dimensions, metrics, columns);
        } else {
            // Render normal rows
            displayData.forEach((row, index) => {
                const rowClass = index % 2 === 0 ? 'bg-white' : 'bg-gray-50';
                const tr = document.createElement('tr');
                tr.className = `${rowClass} hover:bg-blue-50 transition-colors`;

                columns.forEach(key => {
                    const td = document.createElement('td');
                    td.className = 'px-6 py-3 whitespace-nowrap text-sm text-gray-900 border-b border-gray-100';
                    const value = row[key];
                    td.textContent = value !== null && value !== undefined ? value : '';
                    tr.appendChild(td);
                });

                tbody.appendChild(tr);
            });
        }

        table.appendChild(tbody);
        tableContainer.appendChild(table);
        mainDiv.appendChild(tableContainer);

        // Create control bar
        const controlBar = document.createElement('div');
        controlBar.className = 'flex justify-between items-center pt-4 border-t border-gray-200';
        const detailCount = grouped ? this.countAllProcessedRows(displayData) : rowCount;

        let groupInfo = '';
        if (grouped) {
            const primaryGroups = displayData.length;
            const secondaryGroups = this.countAllSubGroups(displayData);

            if (dimensions.length === 1) {
                groupInfo = `<span class="text-blue-500 ml-2">按${dimensions[0]}分组合并，共${primaryGroups}个分组</span>`;
            } else if (dimensions.length === 2) {
                groupInfo = `<span class="text-blue-500 ml-2">按${dimensions[0]}、${dimensions[1]}多级分组合并，共${primaryGroups}个${dimensions[0]}分组，${secondaryGroups}个${dimensions[1]}分组</span>`;
            } else {
                groupInfo = `<span class="text-blue-500 ml-2">按${dimensions.join('、')}多级分组合并，共${primaryGroups}个顶级分组</span>`;
            }
        }

        controlBar.innerHTML = `
            <div class="text-sm text-gray-600">
                ${grouped ?
                    `<span class="text-blue-600">
                        <svg class="w-4 h-4 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
                        </svg>
                        多级单元格合并模式
                    </span>
                    ${groupInfo}
                    <span class="text-gray-500 ml-2">总计${detailCount.toLocaleString()}条明细数据</span>` :
                    `<span>显示 <span class="font-semibold text-gray-900">${rowCount.toLocaleString()}</span> 条数据</span>`
                }
                ${isLimited ? `<span class="text-orange-500 ml-2">⚠️ 可能还有更多数据</span>` : ''}
            </div>
            <div class="flex gap-2 items-center">
                <select id="limitSelect" class="text-sm border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500">
                    <option value="50" ${this.currentLimit === 50 ? 'selected' : ''}>50条</option>
                    <option value="100" ${this.currentLimit === 100 ? 'selected' : ''}>100条</option>
                    <option value="200" ${this.currentLimit === 200 ? 'selected' : ''}>200条</option>
                    <option value="500" ${this.currentLimit === 500 ? 'selected' : ''}>500条</option>
                    <option value="1000" ${this.currentLimit === 1000 ? 'selected' : ''}>1000条</option>
                </select>
                ${isLimited ? `<button id="loadMoreBtn" class="text-sm bg-blue-500 text-white px-4 py-2 rounded-lg hover:bg-blue-600 transition-colors flex items-center">
                    <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                    </svg>
                    加载更多
                </button>` : ''}
            </div>
        `;

        mainDiv.appendChild(controlBar);
        container.appendChild(mainDiv);

        // 绑定事件监听器
        this.setupTableControls();

        // 对于合并单元格模式，不需要展开/折叠功能
        // 所有数据都通过合并单元格可见
    }

    groupDataForTable(data, dimensions, metrics) {
        if (!data || data.length === 0) {
            return [];
        }

        // Debug: Log input data
        console.log('Input data for grouping:', data.length, 'rows');
        console.log('Dimensions:', dimensions);
        console.log('Metrics:', metrics);

        // Create hierarchical grouping structure
        const hierarchicalData = this.createHierarchicalGroups(data, dimensions, metrics);

        // Debug: Log grouped data
        console.log('Grouped data structure:', hierarchicalData);
        const totalProcessed = this.countAllProcessedRows(hierarchicalData);
        console.log('Total processed rows:', totalProcessed, '(should match input rows)');

        return hierarchicalData;
    }

    countAllProcessedRows(groups) {
        return groups.reduce((count, group) => {
            if (group.subGroups && group.subGroups.length > 0) {
                return count + this.countAllProcessedRows(group.subGroups);
            }
            return count + (group.details ? group.details.length : 0);
        }, 0);
    }

    createHierarchicalGroups(data, dimensions, metrics) {
        if (dimensions.length === 0) {
            // No dimensions, return flat data
            return data.map(row => ({
                level: 0,
                groupValues: {},
                details: [row],
                metrics: metrics.reduce((acc, metric) => {
                    acc[metric] = parseFloat(row[metric]) || 0;
                    return acc;
                }, {}),
                subGroups: []
            }));
        }

        // Create nested groups for each dimension level
        const createNestedGroups = (data, dimIndex) => {
            if (dimIndex >= dimensions.length) {
                // Last level: create leaf groups for each unique row
                const leafMap = new Map();
                data.forEach(row => {
                    // Create a unique key for this row based on all dimension values
                    const rowKey = dimensions.map(dim => row[dim] || '').join('|');
                    if (!leafMap.has(rowKey)) {
                        leafMap.set(rowKey, {
                            level: dimIndex,
                            groupValues: dimensions.reduce((acc, dim) => {
                                acc[dim] = row[dim] || '';
                                return acc;
                            }, {}),
                            details: [],
                            metrics: metrics.reduce((acc, metric) => {
                                acc[metric] = 0;
                                return acc;
                            }, {}),
                            subGroups: []
                        });
                    }
                    const leafGroup = leafMap.get(rowKey);
                    leafGroup.details.push(row);

                    // Add metrics for this row
                    metrics.forEach(metric => {
                        leafGroup.metrics[metric] += parseFloat(row[metric]) || 0;
                    });
                });
                return Array.from(leafMap.values());
            }

            const currentDim = dimensions[dimIndex];
            const groupedMap = new Map();

            data.forEach(row => {
                const dimValue = row[currentDim] || '未知';
                if (!groupedMap.has(dimValue)) {
                    groupedMap.set(dimValue, []);
                }
                groupedMap.get(dimValue).push(row);
            });

            // Convert map to array with nested structure
            return Array.from(groupedMap.entries()).map(([dimValue, groupData]) => {
                const subGroups = createNestedGroups(groupData, dimIndex + 1);

                // Calculate metrics for this group by summing all sub-group metrics
                const groupMetrics = {};
                metrics.forEach(metric => {
                    groupMetrics[metric] = subGroups.reduce((sum, group) => {
                        return sum + (group.metrics[metric] || 0);
                    }, 0);
                });

                return {
                    level: dimIndex,
                    groupValues: {
                        [currentDim]: dimValue
                    },
                    groupKey: dimValue,
                    details: groupData,
                    metrics: groupMetrics,
                    subGroups: subGroups
                };
            });
        };

        return createNestedGroups(data, 0);
    }

    calculateGroupMetrics(details, metrics) {
        const result = {};
        if (!metrics || metrics.length === 0) {
            return result;
        }

        // Calculate SUM for each metric
        metrics.forEach(metric => {
            const sum = details.reduce((acc, row) => {
                const val = parseFloat(row[metric]) || 0;
                return acc + val;
            }, 0);
            result[metric] = sum;
        });

        return result;
    }

    renderMergedCellTable(tbody, groupedData, dimensions, metrics, columns) {
        // Calculate other columns (non-dimension, non-metric)
        const otherColumns = columns.filter(col =>
            !dimensions.includes(col) && !metrics.includes(col)
        );

        // Flatten grouped data for simple rendering
        const flatData = this.flattenGroupedData(groupedData, dimensions, metrics);

        // Render simple merged cell table
        this.renderSimpleMergedTable(tbody, flatData, dimensions, metrics, otherColumns);

        // Add summary row at the end
        this.addSummaryRow(tbody, dimensions, metrics, otherColumns, groupedData);
    }

    flattenGroupedData(groupedData, dimensions, metrics) {
        const flatRows = [];

        const processGroup = (group, dimIndex, parentValues = {}) => {
            const currentValues = { ...parentValues, ...group.groupValues };

            if (group.subGroups && group.subGroups.length > 0) {
                // This is a parent group
                group.subGroups.forEach(subGroup => {
                    processGroup(subGroup, dimIndex + 1, currentValues);
                });
            } else {
                // This is a leaf group, add each detail row
                if (group.details) {
                    group.details.forEach(detailRow => {
                        const flatRow = {
                            ...detailRow,
                            _groupValues: currentValues,
                            _groupMetrics: group.metrics,
                            _isGroupHeader: false
                        };

                        // Add a group header row if needed
                        if (dimensions.length > 1 && dimIndex < dimensions.length - 1) {
                            const headerRow = {
                                ...detailRow,
                                _groupValues: currentValues,
                                _groupMetrics: group.metrics,
                                _isGroupHeader: true,
                                _level: dimIndex
                            };
                            flatRows.push(headerRow);
                        }

                        flatRows.push(flatRow);
                    });
                }
            }
        };

        groupedData.forEach(group => processGroup(group, 0));

        return flatRows;
    }

    renderSimpleMergedTable(tbody, flatData, dimensions, metrics, otherColumns) {
        if (flatData.length === 0) return;

        // Track rowspan for each dimension
        const dimensionSpans = {};
        const dimensionValues = {};

        dimensions.forEach(dim => {
            dimensionSpans[dim] = {};
            dimensionValues[dim] = null;
        });

        let currentRow = 0;

        flatData.forEach((row, rowIndex) => {
            const tr = document.createElement('tr');
            tr.className = currentRow % 2 === 0 ? 'bg-white hover:bg-gray-50' : 'bg-gray-50 hover:bg-gray-100';
            tr.className += ' transition-colors';

            // Add dimension cells with rowspan logic
            dimensions.forEach((dim, dimIndex) => {
                const td = document.createElement('td');
                const currentValue = row._groupValues[dim] || row[dim] || '';

                // Check if this value should have a rowspan
                let spanCount = 1;
                if (rowIndex === 0 || currentValue !== dimensionValues[dim]) {
                    // Calculate rowspan for this value
                    spanCount = this.calculateRowSpanForValue(flatData, rowIndex, dim);
                    dimensionSpans[dim][currentValue] = spanCount - 1; // Remaining rows after current one

                    // Set rowspan
                    if (spanCount > 1) {
                        td.rowSpan = spanCount;
                        td.style.verticalAlign = 'middle';
                    }

                    // Style the cell
                    const level = Object.keys(row._groupValues).indexOf(dim);
                    td.className = `px-6 py-3 text-sm font-medium border-b border-gray-200 ${this.getLevelBackgroundColor(level)}`;

                    const icon = this.getLevelIcon(level);
                    td.innerHTML = `
                        <div class="flex items-center">
                            <svg class="w-4 h-4 mr-2 ${this.getLevelIconColor(level)}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                ${icon}
                            </svg>
                            ${currentValue}
                        </div>
                    `;
                } else if (dimensionSpans[dim][currentValue] > 0) {
                    // This cell is covered by a rowspan from above
                    dimensionSpans[dim][currentValue]--;
                    return; // Don't add this cell
                }

                dimensionValues[dim] = currentValue;
                tr.appendChild(td);
            });

            // Add metric cells
            metrics.forEach(metric => {
                const td = document.createElement('td');
                td.className = 'px-6 py-3 text-sm text-gray-900 border-b border-gray-200 text-right';

                if (row._isGroupHeader) {
                    // Show group aggregated value for headers
                    const value = row._groupMetrics[metric] || 0;
                    td.innerHTML = `<span class="font-semibold">${typeof value === 'number' ? value.toLocaleString() : value}</span>`;
                } else {
                    // Show actual value for detail rows
                    const value = row[metric];
                    td.textContent = value !== null && value !== undefined ? value : '0';
                }

                tr.appendChild(td);
            });

            // Add other columns
            otherColumns.forEach(col => {
                const td = document.createElement('td');
                td.className = 'px-6 py-3 text-sm text-gray-600 border-b border-gray-200';

                if (row._isGroupHeader) {
                    td.textContent = '—';
                } else {
                    const value = row[col];
                    td.textContent = value !== null && value !== undefined ? value : '';
                }

                tr.appendChild(td);
            });

            tbody.appendChild(tr);
            currentRow++;
        });
    }

    calculateRowSpanForValue(data, startIndex, dimension) {
        if (startIndex >= data.length) return 1;

        const startValue = data[startIndex]._groupValues[dimension] || data[startIndex][dimension] || '';
        let spanCount = 1;

        for (let i = startIndex + 1; i < data.length; i++) {
            const currentValue = data[i]._groupValues[dimension] || data[i][dimension] || '';
            if (currentValue === startValue) {
                spanCount++;
            } else {
                break;
            }
        }

        return spanCount;
    }

    
    calculateTotalRows(group) {
        if (!group.subGroups || group.subGroups.length === 0) {
            // Leaf node: count actual data rows
            return (group.details && group.details.length > 0) ? group.details.length : 1;
        }
        // Parent node: sum of all sub-group rows
        return group.subGroups.reduce((total, subGroup) => total + this.calculateTotalRows(subGroup), 0);
    }

    getLevelBackgroundColor(level) {
        const colors = ['bg-blue-50', 'bg-green-50', 'bg-yellow-50', 'bg-purple-50'];
        return colors[level % colors.length];
    }

    getLevelIconColor(level) {
        const colors = ['text-blue-500', 'text-green-500', 'text-yellow-600', 'text-purple-500'];
        return colors[level % colors.length];
    }

    getLevelTextColor(level) {
        const colors = ['text-blue-600', 'text-green-600', 'text-yellow-700', 'text-purple-600'];
        return colors[level % colors.length];
    }

    getLevelIcon(level) {
        const icons = [
            '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>', // 文件夹
            '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path>', // 文件
            '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path>', // 分类
            '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path>' // 数据
        ];
        return icons[level % icons.length];
    }

    addSummaryRow(tbody, dimensions, metrics, otherColumns, groupedData) {
        const summaryRow = document.createElement('tr');
        summaryRow.className = 'bg-blue-100 font-semibold';

        // Create summary cells for each dimension
        dimensions.forEach((dim, index) => {
            const td = document.createElement('td');
            td.className = 'px-6 py-3 text-sm text-blue-900 font-bold border-b border-blue-200';
            td.textContent = index === 0 ? '总计' : '—';
            summaryRow.appendChild(td);
        });

        // Calculate and add total metrics
        metrics.forEach(metric => {
            const total = this.calculateTotalMetric(groupedData, metric);
            const td = document.createElement('td');
            td.className = 'px-6 py-3 text-sm text-blue-900 font-bold border-b border-blue-200 text-right';
            td.textContent = typeof total === 'number' ? total.toLocaleString() : total;
            summaryRow.appendChild(td);
        });

        // Add empty cells for other columns
        otherColumns.forEach(col => {
            const td = document.createElement('td');
            td.className = 'px-6 py-3 text-sm text-blue-900 border-b border-blue-200';
            td.textContent = '—';
            summaryRow.appendChild(td);
        });

        tbody.appendChild(summaryRow);
    }

    calculateTotalMetric(groups, metric) {
        return groups.reduce((total, group) => {
            return total + (group.metrics[metric] || 0);
        }, 0);
    }

    countAllSubGroups(groups) {
        return groups.reduce((count, group) => {
            if (group.subGroups && group.subGroups.length > 0) {
                return count + group.subGroups.length + this.countAllSubGroups(group.subGroups);
            }
            return count;
        }, 0);
    }

    setupGroupToggle() {
        // For merged cells, we don't need expand/collapse functionality
        // The data is all visible with merged cells showing groupings
    }

    setupTableControls() {
        // 限制数量选择
        const limitSelect = document.getElementById('limitSelect');
        if (limitSelect) {
            limitSelect.addEventListener('change', (e) => {
                this.currentLimit = parseInt(e.target.value);
                this.executeQuery();
            });
        }

        // 加载更多按钮
        const loadMoreBtn = document.getElementById('loadMoreBtn');
        if (loadMoreBtn) {
            loadMoreBtn.addEventListener('click', () => {
                this.currentLimit = Math.min(this.currentLimit * 2, 1000);
                this.executeQuery();
            });
        }

        // 绑定表格分组开关事件
        const enableGrouping = document.getElementById('enableGrouping');
        if (enableGrouping) {
            enableGrouping.onchange = (e) => {
                this.enableTableGrouping = e.target.checked;
                if (this.currentData.length > 0 && this.chartType === 'table') {
                    this.renderChart(this.currentData);
                }
            };
        }
    }

    renderEChart(container, data) {
        if (data.length === 0) {
            container.innerHTML = `
                <div class="flex items-center justify-center h-96">
                    <div class="text-center">
                        <svg class="w-16 h-16 mx-auto mb-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 3.055A9.001 9.001 0 1020.945 13H11V3.055z"></path>
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.488 9H15V3.512A9.025 9.025 0 0120.488 9z"></path>
                        </svg>
                        <p class="text-gray-500 text-lg">查询结果为空</p>
                        <p class="text-gray-400 text-sm mt-1">请调整查询条件后重试</p>
                    </div>
                </div>
            `;
            return;
        }

        const rowCount = data.length;
        const isLimited = this.currentLimit <= 500 && rowCount === this.currentLimit;

        // Create main container
        const mainDiv = document.createElement('div');
        mainDiv.className = 'space-y-4';

        // Create chart container
        const chartContainer = document.createElement('div');
        chartContainer.className = 'border border-gray-200 rounded-lg bg-white';
        chartContainer.style.height = '450px';

        const echartDiv = document.createElement('div');
        echartDiv.id = 'echartContainer';
        echartDiv.className = 'w-full h-full';
        chartContainer.appendChild(echartDiv);
        mainDiv.appendChild(chartContainer);

        // Create control bar
        const controlBar = document.createElement('div');
        controlBar.className = 'flex justify-between items-center pt-4 border-t border-gray-200';
        controlBar.innerHTML = `
            <div class="text-sm text-gray-600">
                显示 <span class="font-semibold text-gray-900">${rowCount.toLocaleString()}</span> 条数据
                ${isLimited ? `<span class="text-orange-500 ml-2">⚠️ 可能还有更多数据</span>` : ''}
            </div>
            <div class="flex gap-2 items-center">
                <select id="chartLimitSelect" class="text-sm border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500">
                    <option value="50" ${this.currentLimit === 50 ? 'selected' : ''}>50条</option>
                    <option value="100" ${this.currentLimit === 100 ? 'selected' : ''}>100条</option>
                    <option value="200" ${this.currentLimit === 200 ? 'selected' : ''}>200条</option>
                    <option value="500" ${this.currentLimit === 500 ? 'selected' : ''}>500条</option>
                    <option value="1000" ${this.currentLimit === 1000 ? 'selected' : ''}>1000条</option>
                </select>
                ${isLimited ? `<button id="chartLoadMoreBtn" class="text-sm bg-blue-500 text-white px-4 py-2 rounded-lg hover:bg-blue-600 transition-colors flex items-center">
                    <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                    </svg>
                    加载更多
                </button>` : ''}
            </div>
        `;

        mainDiv.appendChild(controlBar);
        container.appendChild(mainDiv);

        // Dispose existing chart
        if (this.chartInstance) {
            this.chartInstance.dispose();
        }

        // Create new chart in the dedicated container
        this.chartInstance = echarts.init(echartDiv);

        const option = this.generateEChartOption(data);
        this.chartInstance.setOption(option);

        // Handle window resize
        window.addEventListener('resize', () => {
            this.chartInstance && this.chartInstance.resize();
        });

        // 绑定图表控制事件
        this.setupChartControls();
    }

    setupChartControls() {
        // 限制数量选择
        const limitSelect = document.getElementById('chartLimitSelect');
        if (limitSelect) {
            limitSelect.addEventListener('change', (e) => {
                this.currentLimit = parseInt(e.target.value);
                this.executeQuery();
            });
        }

        // 加载更多按钮
        const loadMoreBtn = document.getElementById('chartLoadMoreBtn');
        if (loadMoreBtn) {
            loadMoreBtn.addEventListener('click', () => {
                this.currentLimit = Math.min(this.currentLimit * 2, 1000);
                this.executeQuery();
            });
        }
    }

    generateEChartOption(data) {
        const dimensions = this.getSelectedFields('dimensions');
        const metrics = this.getSelectedFields('metrics');

        // Ensure we have an X-axis dimension selected
        if (!this.xAxisDimension && dimensions.length > 0) {
            this.xAxisDimension = dimensions[0];
        }

        if (this.chartType === 'pie' && dimensions.length >= 1 && metrics.length >= 1) {
            // Pie chart - support multiple dimensions by combining them
            let pieData;

            if (this.groupDimensions.length > 0) {
                // Multiple dimensions: group by selected dimensions
                const groupByDims = [this.xAxisDimension, ...this.groupDimensions];
                const groupedMap = new Map();

                data.forEach(row => {
                    const key = groupByDims.map(d => row[d]).join(' / ');
                    const value = parseFloat(row[metrics[0]]) || 0;
                    if (!groupedMap.has(key)) {
                        groupedMap.set(key, 0);
                    }
                    groupedMap.set(key, groupedMap.get(key) + value);
                });

                pieData = Array.from(groupedMap.entries()).map(([name, value]) => ({ name, value }));
            } else {
                // Single dimension
                pieData = data.map(row => ({
                    name: row[this.xAxisDimension] || '',
                    value: parseFloat(row[metrics[0]]) || 0
                }));
            }

            return {
                tooltip: {
                    trigger: 'item',
                    formatter: '{a} <br/>{b}: {c} ({d}%)'
                },
                legend: {
                    orient: 'vertical',
                    left: 'left',
                    type: 'scroll'
                },
                series: [{
                    name: metrics[0],
                    type: 'pie',
                    radius: ['40%', '70%'],
                    avoidLabelOverlap: false,
                    data: pieData,
                    emphasis: {
                        itemStyle: {
                            shadowBlur: 10,
                            shadowOffsetX: 0,
                            shadowColor: 'rgba(0, 0, 0, 0.5)'
                        }
                    },
                    label: {
                        show: false,
                        position: 'center'
                    },
                    labelLine: {
                        show: false
                    }
                }]
            };
        } else {
            // Bar or Line chart - support multiple dimensions as series
            let series = [];

            if (this.groupDimensions.length > 0) {
                // Multiple dimensions: create series for each group
                const groupByDims = this.groupDimensions;
                const uniqueGroups = [...new Set(data.map(row => groupByDims.map(d => row[d]).join(' / ')))];

                uniqueGroups.forEach(group => {
                    const groupValues = group.split(' / ');
                    const filteredData = data.filter(row => {
                        const rowValues = groupByDims.map(d => row[d]);
                        return rowValues.every((v, i) => v === groupValues[i]);
                    });

                    const dataPoints = filteredData.map(row => ({
                        name: row[this.xAxisDimension],
                        value: parseFloat(row[metrics[0]]) || 0
                    }));

                    series.push({
                        name: group,
                        type: this.chartType,
                        data: dataPoints.map(d => d.value),
                        emphasis: {
                            focus: 'series'
                        }
                    });
                });
            } else {
                // Single dimension or no grouping
                const xAxisData = [...new Set(data.map(row => row[this.xAxisDimension] || ''))];
                series = metrics.map(metric => ({
                    name: metric,
                    type: this.chartType,
                    data: data.map(row => parseFloat(row[metric]) || 0),
                    emphasis: {
                        focus: 'series'
                    }
                }));
            }

            const xAxisData = [...new Set(data.map(row => row[this.xAxisDimension] || ''))];

            return {
                tooltip: {
                    trigger: 'axis',
                    axisPointer: {
                        type: 'shadow'
                    }
                },
                legend: {
                    data: series.map(s => s.name),
                    type: 'scroll',
                    top: 10
                },
                grid: {
                    left: '2%',
                    right: '2%',
                    bottom: '8%',
                    top: '15%',
                    containLabel: true
                },
                xAxis: {
                    type: 'category',
                    data: xAxisData,
                    axisLabel: {
                        rotate: xAxisData.length > 10 ? 45 : 0,
                        interval: xAxisData.length > 50 ? 'auto' : 0
                    }
                },
                yAxis: {
                    type: 'value'
                },
                series: series,
                dataZoom: xAxisData.length > 20 ? [
                    {
                        type: 'inside'
                    },
                    {
                        type: 'slider',
                        height: 20,
                        bottom: 30
                    }
                ] : undefined
            };
        }
    }

    showLoading(show) {
        const overlay = document.getElementById('loadingOverlay');
        if (show) {
            overlay.classList.remove('hidden');
        } else {
            overlay.classList.add('hidden');
        }
    }

  
    getTypeIcon(dataType) {
        if (!dataType) return '📝';
        const type = dataType.toLowerCase();
        if (type.includes('int') || type.includes('decimal') || type.includes('float') || type.includes('double')) {
            return '🔢';
        } else if (type.includes('varchar') || type.includes('text') || type.includes('char')) {
            return '📝';
        } else if (type.includes('date') || type.includes('time')) {
            return '📅';
        } else if (type.includes('bool')) {
            return '✅';
        }
        return '📝';
    }

    getTypeColor(dataType) {
        if (!dataType) return 'gray';
        const type = dataType.toLowerCase();
        if (type.includes('int') || type.includes('decimal') || type.includes('float') || type.includes('double')) {
            return 'blue';
        } else if (type.includes('varchar') || type.includes('text') || type.includes('char')) {
            return 'green';
        } else if (type.includes('date') || type.includes('time')) {
            return 'purple';
        } else if (type.includes('bool')) {
            return 'orange';
        }
        return 'gray';
    }

    updateConnectionStatus() {
        const status = document.getElementById('connectionStatus');
        // 在HTML中已经设置为已连接状态，这里可以添加实际的连接检查逻辑
        console.log('Connection status updated');
    }

    showError(message) {
        // 创建更好的错误提示
        const toast = document.createElement('div');
        toast.className = 'fixed top-4 right-4 bg-red-500 text-white px-6 py-3 rounded-lg shadow-lg z-50 flex items-center space-x-2 slide-in';
        toast.innerHTML = `
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            <span>${message}</span>
        `;

        document.body.appendChild(toast);

        // 3秒后自动移除
        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(100%)';
            toast.style.transition = 'all 0.3s ease-out';
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }

    showSuccess(message) {
        // 创建成功提示
        const toast = document.createElement('div');
        toast.className = 'fixed top-4 right-4 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg z-50 flex items-center space-x-2 slide-in';
        toast.innerHTML = `
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
            </svg>
            <span>${message}</span>
        `;

        document.body.appendChild(toast);

        // 2秒后自动移除
        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(100%)';
            toast.style.transition = 'all 0.3s ease-out';
            setTimeout(() => toast.remove(), 300);
        }, 2000);
    }
}

// Initialize the application
document.addEventListener('DOMContentLoaded', () => {
    new BIReportPlatform();
});