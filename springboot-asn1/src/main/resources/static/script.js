// 全局变量
let currentResult = null;
let parseStartTime = null;

// DOM元素
const elements = {
    asn1Input: document.getElementById('asn1Input'),
    encodingType: document.getElementById('encodingType'),
    verboseCheckbox: document.getElementById('verbose'),
    parseBtn: document.getElementById('parseBtn'),
    resultContainer: document.getElementById('resultContainer'),
    resultStats: document.getElementById('resultStats'),
    parseTime: document.getElementById('parseTime'),
    dataSize: document.getElementById('dataSize'),
    clearBtn: document.getElementById('clearBtn'),
    pasteBtn: document.getElementById('pasteBtn'),
    expandAllBtn: document.getElementById('expandAllBtn'),
    collapseAllBtn: document.getElementById('collapseAllBtn'),
    copyBtn: document.getElementById('copyBtn'),
    downloadBtn: document.getElementById('downloadBtn')
};

// 示例数据
const sampleData = {
    cert: {
        name: 'CERT',
        data: 'MIICFjCCAbugAwIBAgIJAOWoGwJCnlzJMAoGCCqBHM9VAYN1MGcxCzAJBgNVBAYTAkNOMRAwDgYDVQQIDAdCZWlqaW5nMRAwDgYDVQQHDAdIYWlEaWFuMRMwEQYDVQQKDApHTUNlcnQub3JnMR8wHQYDVQQDDBZHTUNlcnQgR00gUm9vdCBDQSAtIDAxMB4XDTI1MTAxNTE1MzQ0NloXDTI2MTAxNTE1MzQ0NlowKjELMAkGA1UEBhMCQ04xGzAZBgNVBAMMEkNOPVNQUklOR0JPT1QtQVNOMTBZMBMGByqGSM49AgEGCCqBHM9VAYItA0IABELWZpP7zz8BfGaF1KBAXPnz6vLlsQbyTRiQGCutV4gfRCHyWWv3UDfuZnsb23Gpkl9tP3I+vLUEbAgSHlBu8UqjgYwwgYkwDAYDVR0TAQH/BAIwADALBgNVHQ8EBAMCBsAwLAYJYIZIAYb4QgENBB8WHUdNQ2VydC5vcmcgU2lnbmVkIENlcnRpZmljYXRlMB0GA1UdDgQWBBQ9PH/j70J99kkwDlX3iBG1hr8PbDAfBgNVHSMEGDAWgBR/Wl47AIRZKg+YvqEObzmVQxBNBzAKBggqgRzPVQGDdQNJADBGAiEAu8CIkPzNuYYCXCJ1amp+mgPKLIdwqkWfw0bkkols8o8CIQDKuntS24AVDeffU3OFrgQThOOhOuVzm7QXdw2jfFzClg==',
        description: '证书数据'
    },
    integer: {
        name: 'INTEGER',
        data: 'AgEB',
        description: '简单的ASN.1整数，值为1'
    },
    sequence: {
        name: 'SEQUENCE',
        data: 'MAkCAQECAQECAQE=',
        description: '包含三个整数的序列'
    },
    utf8string: {
        name: 'UTF8String',
        data: 'DAVIZWxsbw==',
        description: 'UTF8编码的字符串"Hello"'
    },
    oid: {
        name: 'OID',
        data: 'BgMqAwQ=',
        description: '对象标识符 1.2.3.4'
    },
    boolean: {
        name: 'BOOLEAN',
        data: 'AQH/',
        description: '布尔值 TRUE'
    },
    null: {
        name: 'NULL',
        data: 'BQA=',
        description: '空值 NULL'
    }
};

// 初始化
document.addEventListener('DOMContentLoaded', function() {
    initializeEventListeners();
    loadSampleDataOnLoad();
    setupKeyboardShortcuts();
    initializeTooltips();
});

// 初始化事件监听器
function initializeEventListeners() {
    // 解析按钮
    elements.parseBtn.addEventListener('click', parseAsn1);

    // 示例数据按钮
    document.querySelectorAll('.sample-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const sampleType = this.dataset.sample;
            loadSampleData(sampleType);
        });
    });

    // 清空按钮
    elements.clearBtn.addEventListener('click', clearInput);

    // 粘贴按钮
    elements.pasteBtn.addEventListener('click', pasteFromClipboard);

    // 输入框事件
    elements.asn1Input.addEventListener('input', handleInputChange);
    elements.asn1Input.addEventListener('keydown', handleInputKeydown);

    // 编码类型改变
    elements.encodingType.addEventListener('change', handleEncodingChange);

    // 结果操作按钮
    elements.expandAllBtn.addEventListener('click', expandAll);
    elements.collapseAllBtn.addEventListener('click', collapseAll);
    elements.copyBtn.addEventListener('click', copyResult);
    elements.downloadBtn.addEventListener('click', downloadResult);

    // 拖拽文件
    setupDragAndDrop();
}

// 加载示例数据
function loadSampleData(sampleType) {
    const sample = sampleData[sampleType];
    if (sample) {
        elements.asn1Input.value = sample.data;
        elements.asn1Input.focus();

        // 显示提示信息
        showNotification(`已加载${sample.name}示例：${sample.description}`, 'info');

        // 自动解析
        setTimeout(() => parseAsn1(), 100);
    }
}

// 页面加载时显示默认示例
function loadSampleDataOnLoad() {
    // 可以选择在页面加载时显示一个默认示例
    // loadSampleData('integer');
}

// 解析ASN.1数据
async function parseAsn1() {
    const data = elements.asn1Input.value.trim();

    if (!data) {
        showNotification('请输入ASN.1数据', 'warning');
        elements.asn1Input.focus();
        return;
    }

    // 显示加载状态
    setLoading(true);
    parseStartTime = performance.now();

    try {
        const response = await fetch('/api/asn1/parse', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                asn1Data: data,
                encodingType: elements.encodingType.value,
                verbose: elements.verboseCheckbox.checked
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const result = await response.json();
        currentResult = result;

        if (result.success) {
            showSuccess(result);
            updateStats(result, data.length);
        } else {
            showError(result.message);
        }

    } catch (error) {
        console.error('解析错误:', error);
        showError(`网络错误或服务器异常: ${error.message}`);
    } finally {
        setLoading(false);
    }
}

// 显示加载状态
function setLoading(loading) {
    if (loading) {
        elements.parseBtn.innerHTML = '<span class="loading"></span><span>解析中...</span>';
        elements.parseBtn.disabled = true;
    } else {
        elements.parseBtn.innerHTML = '<i class="fas fa-search"></i><span>解析ASN.1</span>';
        elements.parseBtn.disabled = false;
    }
}

// 显示成功结果
function showSuccess(result) {
    let html = `<div class="success-message fade-in">
        <i class="fas fa-check-circle"></i>
        <span>${result.message}</span>
    </div>`;

    if (result.rootStructure) {
        html += renderStructure(result.rootStructure);
    }

    if (result.metadata) {
        html += renderMetadata(result.metadata);
    }

    if (result.warnings && result.warnings.length > 0) {
        html += renderWarnings(result.warnings);
    }

    elements.resultContainer.innerHTML = html;
    elements.resultContainer.classList.add('fade-in');

    // 添加交互功能
    addStructureInteraction();
}

// 显示错误信息
function showError(message) {
    elements.resultContainer.innerHTML = `
        <div class="error-message fade-in">
            <i class="fas fa-exclamation-triangle"></i>
            <span>${message}</span>
        </div>
    `;
    elements.resultStats.style.display = 'none';
}

// 显示通知
function showNotification(message, type = 'info') {
    // 创建通知元素
    const notification = document.createElement('div');
    notification.className = `${type}-message fade-in`;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        max-width: 400px;
        z-index: 9999;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    `;

    // 添加图标
    const icon = getIconForType(type);
    notification.innerHTML = `<i class="fas ${icon}"></i><span>${message}</span>`;

    // 添加到页面
    document.body.appendChild(notification);

    // 自动移除
    setTimeout(() => {
        notification.style.opacity = '0';
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 300);
    }, 3000);
}

// 获取类型对应的图标
function getIconForType(type) {
    const icons = {
        'success': 'fa-check-circle',
        'error': 'fa-exclamation-triangle',
        'warning': 'fa-exclamation-circle',
        'info': 'fa-info-circle'
    };
    return icons[type] || icons['info'];
}

// 渲染ASN.1结构
function renderStructure(structure, level = 0) {
    const tagClass = getTagClass(structure.tagClass);
    const hasChildren = structure.children && structure.children.length > 0;

    // 使用表格布局，避免折行
    let html = `
        <div class="structure-row" data-level="${level}">
            <div class="structure-indent" style="width: ${level * 20}px;"></div>
            <div class="structure-toggle">
                ${hasChildren ? '<span class="collapsible compact">▼</span>' : '<span class="no-children"></span>'}
            </div>
            <div class="structure-tag-info">
                <span class="tag-mini tag-${tagClass}">${structure.tagClass}</span>
                <span class="tag-name">${structure.tag}</span>
                ${structure.tagNumber >= 0 ? `<span class="tag-num">[${structure.tagNumber}]</span>` : ''}
            </div>
            <div class="structure-type">
                <span class="type-name">${structure.type}</span>
            </div>
            <div class="structure-value">
                <span class="value-text">${formatValue(structure.value, structure.type)}</span>
            </div>
            <div class="structure-meta">
                ${structure.length ? `<span class="len-info">L:${structure.length}</span>` : ''}
                ${structure.offset ? `<span class="off-info">@${structure.offset.toString(16).toUpperCase()}</span>` : ''}
            </div>
        </div>
    `;

    // 详细属性（仅在详细模式下显示）
    if (structure.properties && Object.keys(structure.properties).length > 0) {
        html += '<div class="structure-properties-row">';
        html += '<div class="structure-indent" style="width: ' + ((level + 1) * 20) + 'px;"></div>';
        html += '<div class="properties-content">';
        for (const [key, value] of Object.entries(structure.properties)) {
            html += `<span class="prop-item"><em>${key}:</em> <code>${value}</code></span>`;
        }
        html += '</div>';
        html += '</div>';
    }

    // 添加子结构
    if (hasChildren) {
        html += '<div class="structure-children-wrapper">';
        structure.children.forEach(child => {
            html += renderStructure(child, level + 1);
        });
        html += '</div>';
    }

    return html;
}

// 格式化值显示
function formatValue(value, type) {
    if (!value) return '';

    // 对于不同类型的值进行特殊格式化
    switch (type) {
        case 'OCTET STRING':
        case 'BIT STRING':
            // 对于长数据，提供展开/折叠功能，但保留完整值用于复制
            if (value.length > 50) {
                return `
                    <div class="value-container">
                        <span class="long-value" data-full-value="${value}" title="${value}">${value.substring(0, 47)}...</span>
                        <span class="copy-btn" data-value="${value}" title="点击复制完整值">
                            <i class="fas fa-copy"></i>
                        </span>
                    </div>
                `;
            }
            return `
                <div class="value-container">
                    <span data-full-value="${value}"><code>${value}</code></span>
                    <span class="copy-btn" data-value="${value}" title="点击复制完整值">
                        <i class="fas fa-copy"></i>
                    </span>
                </div>
            `;

        case 'INTEGER':
            return `
                <div class="value-container">
                    <span data-full-value="${value}"><code>${value}</code></span>
                    <span class="copy-btn" data-value="${value}" title="点击复制完整值">
                        <i class="fas fa-copy"></i>
                    </span>
                </div>
            `;

        case 'OBJECT IDENTIFIER':
            return `
                <div class="value-container">
                    <span data-full-value="${value}"><code>${value}</code></span>
                    <span class="copy-btn" data-value="${value}" title="点击复制完整值">
                        <i class="fas fa-copy"></i>
                    </span>
                </div>
            `;

        case 'BOOLEAN':
            return `
                <div class="value-container">
                    <span class="boolean-value ${value.toLowerCase()}" data-full-value="${value}">${value}</span>
                    <span class="copy-btn" data-value="${value}" title="点击复制完整值">
                        <i class="fas fa-copy"></i>
                    </span>
                </div>
            `;

        case 'NULL':
            return `
                <div class="value-container">
                    <span class="null-value" data-full-value="${value}">${value}</span>
                    <span class="copy-btn" data-value="${value}" title="点击复制完整值">
                        <i class="fas fa-copy"></i>
                    </span>
                </div>
            `;

        default:
            return `
                <div class="value-container">
                    <span data-full-value="${value}">${escapeHtml(value)}</span>
                    <span class="copy-btn" data-value="${value}" title="点击复制完整值">
                        <i class="fas fa-copy"></i>
                    </span>
                </div>
            `;
    }
}

// 渲染属性
function renderProperties(properties) {
    let html = '<div class="structure-properties">';
    for (const [key, value] of Object.entries(properties)) {
        html += `<div><em>${key}:</em> <code>${value}</code></div>`;
    }
    html += '</div>';
    return html;
}

// 渲染元数据
function renderMetadata(metadata) {
    let html = '<div style="margin-top: 20px;"><h3><i class="fas fa-info-circle"></i> 元数据</h3><div class="structure-details">';
    for (const [key, value] of Object.entries(metadata)) {
        html += `<div><strong>${formatMetadataKey(key)}:</strong> ${formatMetadataValue(key, value)}</div>`;
    }
    html += '</div></div>';
    return html;
}

// 格式化元数据键
function formatMetadataKey(key) {
    const keyMap = {
        'originalLength': '原始长度',
        'encodingType': '编码类型',
        'encodingTimestamp': '编码时间戳',
        'probableEncoding': '可能编码规则'
    };
    return keyMap[key] || key;
}

// 格式化元数据值
function formatMetadataValue(key, value) {
    switch (key) {
        case 'encodingTimestamp':
            return new Date(value).toLocaleString();
        case 'originalLength':
            return `${value} 字节`;
        default:
            return value;
    }
}

// 渲染警告信息
function renderWarnings(warnings) {
    let html = '<div style="margin-top: 15px;"><h3><i class="fas fa-exclamation-triangle"></i> 警告</h3>';
    warnings.forEach(warning => {
        html += `<div class="warning-message"><i class="fas fa-exclamation-circle"></i> ${warning}</div>`;
    });
    html += '</div>';
    return html;
}

// 获取标签类样式
function getTagClass(tagClass) {
    const classMap = {
        'UNIVERSAL': 'universal',
        'APPLICATION': 'application',
        'CONTEXT_SPECIFIC': 'context',
        'PRIVATE': 'private'
    };
    return classMap[tagClass] || 'unknown';
}

// 添加结构交互功能
function addStructureInteraction() {
    // 添加展开/折叠功能
    document.querySelectorAll('.collapsible').forEach(element => {
        element.addEventListener('click', function() {
            const structureRow = this.closest('.structure-row');
            let childrenWrapper = structureRow.nextElementSibling;

            // 查找子元素容器
            while (childrenWrapper && !childrenWrapper.classList.contains('structure-children-wrapper')) {
                childrenWrapper = childrenWrapper.nextElementSibling;
            }

            if (childrenWrapper && childrenWrapper.classList.contains('structure-children-wrapper')) {
                const isHidden = childrenWrapper.style.display === 'none';
                childrenWrapper.style.display = isHidden ? 'block' : 'none';
                this.textContent = isHidden ? '▼' : '▶';
                this.classList.toggle('collapsed');
            }
        });
    });

    // 添加长值展开/折叠功能（只针对长值）
    document.querySelectorAll('.long-value').forEach(element => {
        element.style.cursor = 'pointer';
        element.title = '点击展开/折叠完整值';
        element.addEventListener('click', function(e) {
            e.stopPropagation();
            toggleLongValue(this);
        });
    });

    // 添加复制按钮功能
    document.querySelectorAll('.copy-btn').forEach(element => {
        element.style.cursor = 'pointer';
        element.addEventListener('click', function(e) {
            e.stopPropagation();
            const valueToCopy = this.getAttribute('data-value');
            copyToClipboard(valueToCopy);

            // 显示复制成功的反馈
            const originalIcon = this.innerHTML;
            this.innerHTML = '<i class="fas fa-check"></i>';
            this.style.color = '#27ae60';

            setTimeout(() => {
                this.innerHTML = originalIcon;
                this.style.color = '';
            }, 2000);

            showNotification('已复制到剪贴板', 'success');
        });
    });

    // 为类型名添加复制功能
    document.querySelectorAll('.type-name').forEach(element => {
        element.style.cursor = 'pointer';
        element.addEventListener('click', function(e) {
            e.stopPropagation();
            copyToClipboard(this.textContent);
            showNotification('已复制到剪贴板', 'success');
        });
    });

  
    // 添加双击展开/折叠所有子节点
    document.querySelectorAll('.structure-row').forEach(row => {
        row.addEventListener('dblclick', function(e) {
            if (e.target.classList.contains('collapsible')) return;

            const collapsible = this.querySelector('.collapsible');
            if (collapsible) {
                collapsible.click();
            }
        });
    });
}

// 更新统计信息
function updateStats(result, inputSize) {
    if (parseStartTime) {
        const parseTime = (performance.now() - parseStartTime).toFixed(2);
        elements.parseTime.textContent = `解析时间: ${parseTime}ms`;
        elements.dataSize.textContent = `数据大小: ${inputSize} 字节`;
        elements.resultStats.style.display = 'flex';
    }
}

// 清空输入
function clearInput() {
    elements.asn1Input.value = '';
    elements.resultContainer.innerHTML = '<div class="placeholder"><i class="fas fa-info-circle"></i><p>解析结果将在这里显示...</p><p>请在左侧输入ASN.1数据并点击解析按钮</p></div>';
    elements.resultStats.style.display = 'none';
    currentResult = null;
    elements.asn1Input.focus();
}

// 从剪贴板粘贴
async function pasteFromClipboard() {
    try {
        const text = await navigator.clipboard.readText();
        elements.asn1Input.value = text;
        showNotification('已从剪贴板粘贴', 'success');
        elements.asn1Input.focus();
    } catch (error) {
        showNotification('无法访问剪贴板', 'error');
    }
}

// 复制到剪贴板
async function copyToClipboard(text) {
    try {
        await navigator.clipboard.writeText(text);
    } catch (error) {
        // 降级方案
        const textarea = document.createElement('textarea');
        textarea.value = text;
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
    }
}

// 展开所有
function expandAll() {
    document.querySelectorAll('.structure-children-wrapper').forEach(element => {
        element.style.display = 'block';
    });
    document.querySelectorAll('.collapsible').forEach(element => {
        element.classList.remove('collapsed');
        element.textContent = '▼';
    });
}

// 折叠所有
function collapseAll() {
    document.querySelectorAll('.structure-children-wrapper').forEach(element => {
        element.style.display = 'none';
    });
    document.querySelectorAll('.collapsible').forEach(element => {
        element.classList.add('collapsed');
        element.textContent = '▶';
    });
}

// 复制结果
function copyResult() {
    if (!currentResult) {
        showNotification('没有可复制的结果', 'warning');
        return;
    }

    const resultText = formatResultForCopy(currentResult);
    copyToClipboard(resultText);
    showNotification('结果已复制到剪贴板', 'success');
}

// 格式化结果用于复制
function formatResultForCopy(result) {
    let text = `ASN.1解析结果\n`;
    text += `状态: ${result.success ? '成功' : '失败'}\n`;
    text += `消息: ${result.message}\n\n`;

    if (result.rootStructure) {
        text += formatStructureForCopy(result.rootStructure, 0);
    }

    if (result.metadata) {
        text += '\n元数据:\n';
        for (const [key, value] of Object.entries(result.metadata)) {
            text += `  ${key}: ${value}\n`;
        }
    }

    return text;
}

// 格式化结构用于复制
function formatStructureForCopy(structure, level) {
    const indent = '  '.repeat(level);
    let text = `${indent}${structure.tag} [${structure.tagNumber}] (${structure.tagClass})\n`;
    text += `${indent}  类型: ${structure.type}\n`;
    text += `${indent}  值: ${structure.value}\n`;

    if (structure.children) {
        structure.children.forEach(child => {
            text += formatStructureForCopy(child, level + 1);
        });
    }

    return text;
}

// 下载结果
function downloadResult() {
    if (!currentResult) {
        showNotification('没有可下载的结果', 'warning');
        return;
    }

    const resultText = formatResultForCopy(currentResult);
    const blob = new Blob([resultText], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = `asn1_parse_result_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    showNotification('结果已下载', 'success');
}

// 处理输入变化
function handleInputChange() {
    // 可以在这里添加实时验证或格式化
    const data = elements.asn1Input.value;

    // 简单的十六进制验证
    if (elements.encodingType.value === 'HEX' && data) {
        const hexPattern = /^[0-9a-fA-F\s]*$/;
        if (!hexPattern.test(data)) {
            showNotification('输入包含非十六进制字符', 'warning');
        }
    }
}

// 处理输入键盘事件
function handleInputKeydown(e) {
    if (e.key === 'Enter' && e.ctrlKey) {
        e.preventDefault();
        parseAsn1();
    } else if (e.key === 'Escape') {
        clearInput();
    }
}

// 处理编码类型变化
function handleEncodingChange() {
    // 可以在这里添加编码类型变化时的处理逻辑
    showNotification(`已切换到 ${elements.encodingType.value} 编码`, 'info');
}

// 设置键盘快捷键
function setupKeyboardShortcuts() {
    document.addEventListener('keydown', function(e) {
        // Ctrl+Enter: 解析
        if (e.ctrlKey && e.key === 'Enter') {
            e.preventDefault();
            parseAsn1();
        }

        // Ctrl+L: 清空
        if (e.ctrlKey && e.key === 'l') {
            e.preventDefault();
            clearInput();
        }

        // Ctrl+V: 粘贴（当输入框没有焦点时）
        if (e.ctrlKey && e.key === 'v' && document.activeElement !== elements.asn1Input) {
            e.preventDefault();
            pasteFromClipboard();
        }
    });
}

// 设置拖拽上传
function setupDragAndDrop() {
    const dropZone = elements.asn1Input;

    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        dropZone.addEventListener(eventName, preventDefaults, false);
        document.body.addEventListener(eventName, preventDefaults, false);
    });

    ['dragenter', 'dragover'].forEach(eventName => {
        dropZone.addEventListener(eventName, highlight, false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
        dropZone.addEventListener(eventName, unhighlight, false);
    });

    dropZone.addEventListener('drop', handleDrop, false);
}

function preventDefaults(e) {
    e.preventDefault();
    e.stopPropagation();
}

function highlight(e) {
    elements.asn1Input.style.borderColor = 'var(--primary-color)';
    elements.asn1Input.style.backgroundColor = '#f0f8ff';
}

function unhighlight(e) {
    elements.asn1Input.style.borderColor = '';
    elements.asn1Input.style.backgroundColor = '';
}

async function handleDrop(e) {
    const files = e.dataTransfer.files;
    if (files.length > 0) {
        const file = files[0];
        try {
            const text = await file.text();
            elements.asn1Input.value = text;
            showNotification(`已加载文件: ${file.name}`, 'success');
        } catch (error) {
            showNotification('文件读取失败', 'error');
        }
    }
}

// 展开/折叠长值
function toggleLongValue(element) {
    const fullValue = element.getAttribute('data-full-value');
    const isExpanded = element.classList.contains('expanded');

    if (isExpanded) {
        // 折叠
        if (fullValue.length > 50) {
            element.innerHTML = fullValue.substring(0, 47) + '...';
        }
        element.classList.remove('expanded');
        element.title = '点击展开完整值';
    } else {
        // 展开
        element.innerHTML = `<code>${fullValue}</code>`;
        element.classList.add('expanded');
        element.title = '点击折叠值';
    }
}

// 初始化工具提示
function initializeTooltips() {
    // 为按钮添加工具提示
    const tooltips = {
        clearBtn: '清空输入 (Ctrl+L)',
        pasteBtn: '从剪贴板粘贴 (Ctrl+V)',
        expandAllBtn: '展开所有节点',
        collapseAllBtn: '折叠所有节点',
        copyBtn: '复制结果到剪贴板',
        downloadBtn: '下载结果为文件'
    };

    Object.entries(tooltips).forEach(([id, tooltip]) => {
        const element = elements[id];
        if (element) {
            element.title = tooltip;
            element.classList.add('tooltip');
        }
    });
}

// HTML转义
function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}

// API调用示例（用于开发者）
window.Asn1ParserAPI = {
    parse: parseAsn1,
    loadSample: loadSampleData,
    clear: clearInput,
    getResult: () => currentResult,
    getVersion: () => '1.0.0'
};