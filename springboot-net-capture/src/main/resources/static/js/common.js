// 公共工具函数
const Utils = {
    // 格式化时间
    formatTime: function(timeString) {
        if (!timeString) return '';
        const date = new Date(timeString);
        return date.toLocaleString('zh-CN');
    },

    // 格式化字节数
    formatBytes: function(bytes) {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    },

    // 获取协议对应的Badge样式类
    getProtocolBadgeClass: function(protocol) {
        switch (protocol) {
            case 'HTTP': return 'bg-success';
            case 'TCP': return 'bg-primary';
            case 'UDP': return 'bg-warning';
            case 'DNS': return 'bg-info';
            case 'DHCP': return 'bg-secondary';
            default: return 'bg-dark';
        }
    },

    // API请求封装
    apiRequest: async function(url, options = {}) {
        try {
            const response = await fetch(url, {
                headers: {
                    'Content-Type': 'application/json',
                    ...options.headers
                },
                ...options
            });
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('API request failed:', error);
            throw error;
        }
    },

    // 显示加载状态
    showLoading: function(element, text = '加载中...') {
        if (typeof element === 'string') {
            element = document.getElementById(element);
        }
        if (element) {
            element.innerHTML = `<div class="text-center"><span class="spinner me-2"></span>${text}</div>`;
        }
    },

    // 显示错误信息
    showError: function(element, message = '加载失败') {
        if (typeof element === 'string') {
            element = document.getElementById(element);
        }
        if (element) {
            element.innerHTML = `<div class="text-center text-danger">${message}</div>`;
        }
    },

    // 显示空数据
    showEmpty: function(element, message = '暂无数据', colspan = 1) {
        if (typeof element === 'string') {
            element = document.getElementById(element);
        }
        if (element) {
            element.innerHTML = `<tr><td colspan="${colspan}" class="text-center text-muted">${message}</td></tr>`;
        }
    },

    // 防抖函数
    debounce: function(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },

    // 节流函数
    throttle: function(func, limit) {
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
};

// 全局配置
const Config = {
    API_BASE_URL: '',
    WS_BASE_URL: location.protocol === 'https:' ? 'wss:' : 'ws:',
    DEFAULT_PAGE_SIZE: 20,
    REFRESH_INTERVAL: 5000,
    WEBSOCKET_RECONNECT_DELAY: 5000
};

// 通知工具
const Notification = {
    success: function(message) {
        this.show(message, 'success');
    },
    
    error: function(message) {
        this.show(message, 'danger');
    },
    
    warning: function(message) {
        this.show(message, 'warning');
    },
    
    info: function(message) {
        this.show(message, 'info');
    },
    
    show: function(message, type = 'info') {
        // 创建通知元素
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
        alertDiv.style.top = '20px';
        alertDiv.style.right = '20px';
        alertDiv.style.zIndex = '9999';
        alertDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.body.appendChild(alertDiv);
        
        // 自动移除
        setTimeout(() => {
            if (alertDiv.parentNode) {
                alertDiv.parentNode.removeChild(alertDiv);
            }
        }, 5000);
    }
};

// 页面公共初始化
document.addEventListener('DOMContentLoaded', function() {
    // 设置活动导航项
    const currentPage = window.location.pathname.split('/').pop() || 'index.html';
    const navLinks = document.querySelectorAll('.navbar-nav .nav-link');
    
    navLinks.forEach(link => {
        const href = link.getAttribute('href');
        if (href === currentPage || (currentPage === '' && href === '/')) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });
});