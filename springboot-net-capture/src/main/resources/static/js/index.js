// 主页面功能
class IndexPage {
    constructor() {
        this.refreshInterval = null;
        this.init();
    }

    init() {
        this.bindEvents();
        this.loadInitialData();
        this.startRefreshTimer();
    }

    bindEvents() {
        // 绑定抓包控制按钮事件
        document.getElementById('startCaptureBtn').addEventListener('click', () => this.startCapture());
        document.getElementById('stopCaptureBtn').addEventListener('click', () => this.stopCapture());
    }

    loadInitialData() {
        this.loadNetworkInterfaces();
        this.updateCaptureStatus();
        this.updateStatistics();
    }

    startRefreshTimer() {
        // 定时更新状态和统计
        this.refreshInterval = setInterval(() => {
            this.updateCaptureStatus();
            this.updateStatistics();
        }, Config.REFRESH_INTERVAL);
    }

    // 加载网络接口列表
    async loadNetworkInterfaces() {
        try {
            const result = await Utils.apiRequest('/api/capture/interfaces');
            const select = document.getElementById('networkInterface');
            select.innerHTML = '<option value="">选择网络接口...</option>';
            
            if (result.success && result.interfaces) {
                result.interfaces.forEach(iface => {
                    const option = document.createElement('option');
                    option.value = iface.name;
                    option.textContent = `${iface.name} (${iface.description || '无描述'})`;
                    select.appendChild(option);
                });
            } else {
                // 显示错误信息
                const option = document.createElement('option');
                option.value = "";
                option.textContent = result.message || "无可用网络接口";
                option.disabled = true;
                select.appendChild(option);
                
                // 显示安装提示
                if (result.downloadUrl) {
                    Notification.error(`${result.error}<br>请<a href="${result.downloadUrl}" target="_blank">点击这里下载 Npcap</a>`);
                }
            }
        } catch (error) {
            console.error('加载网络接口失败:', error);
            Notification.error('加载网络接口失败，请检查是否已安装 Npcap');
        }
    }

    // 更新抓包状态
    async updateCaptureStatus() {
        try {
            const status = await Utils.apiRequest('/api/capture/status');
            const indicator = document.querySelector('.status-indicator');
            const statusText = document.getElementById('statusText');
            const startBtn = document.getElementById('startCaptureBtn');
            const stopBtn = document.getElementById('stopCaptureBtn');
            
            if (status.isCapturing) {
                indicator.className = 'status-indicator status-running';
                statusText.textContent = `运行中 (已抓包 ${status.capturedPackets} 个)`;
                startBtn.disabled = true;
                stopBtn.disabled = false;
            } else {
                indicator.className = 'status-indicator status-stopped';
                statusText.textContent = '已停止';
                startBtn.disabled = false;
                stopBtn.disabled = true;
            }
        } catch (error) {
            console.error('获取抓包状态失败:', error);
        }
    }

    // 更新统计信息
    async updateStatistics() {
        try {
            const stats = await Utils.apiRequest('/api/statistics/current');
            document.getElementById('totalPackets').textContent = stats.totalPackets || 0;
            document.getElementById('totalBytes').textContent = Utils.formatBytes(stats.totalBytes || 0);
            document.getElementById('httpPackets').textContent = stats.httpPackets || 0;
            document.getElementById('tcpPackets').textContent = stats.tcpPackets || 0;
            document.getElementById('udpPackets').textContent = stats.udpPackets || 0;
        } catch (error) {
            console.error('获取统计信息失败:', error);
        }
    }

    // 开始抓包
    async startCapture() {
        const networkInterface = document.getElementById('networkInterface').value;
        const filter = document.getElementById('captureFilter').value;
        
        const payload = {
            action: 'start',
            networkInterface: networkInterface,
            filter: filter
        };
        
        try {
            const result = await Utils.apiRequest('/api/capture/control', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            
            if (result.success) {
                Notification.success('抓包已启动');
                this.updateCaptureStatus();
            } else {
                Notification.error('启动失败: ' + result.message);
            }
        } catch (error) {
            console.error('启动抓包失败:', error);
            Notification.error('启动抓包失败');
        }
    }

    // 停止抓包
    async stopCapture() {
        const payload = { action: 'stop' };
        
        try {
            const result = await Utils.apiRequest('/api/capture/control', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            
            if (result.success) {
                Notification.success('抓包已停止');
                this.updateCaptureStatus();
            } else {
                Notification.error('停止失败: ' + result.message);
            }
        } catch (error) {
            console.error('停止抓包失败:', error);
            Notification.error('停止抓包失败');
        }
    }

    // 清理资源
    destroy() {
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
        }
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    window.indexPage = new IndexPage();
});

// 页面卸载时清理资源
window.addEventListener('beforeunload', function() {
    if (window.indexPage) {
        window.indexPage.destroy();
    }
});