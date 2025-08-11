// 统计分析页面功能
class StatisticsPage {
    constructor() {
        this.protocolChart = null;
        this.trendChart = null;
        this.refreshInterval = null;
        this.init();
    }

    init() {
        this.initCharts();
        this.loadStatistics();
        this.startRefreshTimer();
    }

    startRefreshTimer() {
        // 定时刷新统计数据
        this.refreshInterval = setInterval(() => {
            this.loadStatistics();
        }, 30000); // 30秒刷新一次
    }

    // 加载统计数据
    async loadStatistics() {
        await Promise.all([
            this.loadCurrentStatistics(),
            this.loadProtocolStatistics(),
            this.loadTrafficTrend(),
            this.loadTopIps(),
            this.loadHistoryStats()
        ]);
    }

    // 加载当前统计
    async loadCurrentStatistics() {
        try {
            const stats = await Utils.apiRequest('/api/statistics/current');
            document.getElementById('totalPacketsCard').textContent = stats.totalPackets || 0;
            document.getElementById('totalBytesCard').textContent = Utils.formatBytes(stats.totalBytes || 0);
            document.getElementById('httpPacketsCard').textContent = stats.httpPackets || 0;
            document.getElementById('avgPacketSizeCard').textContent = 
                stats.averagePacketSize ? Math.round(stats.averagePacketSize) + 'B' : '0B';
        } catch (error) {
            console.error('加载当前统计失败:', error);
        }
    }

    // 加载协议统计
    async loadProtocolStatistics() {
        try {
            const data = await Utils.apiRequest('/api/statistics/protocols');
            this.updateProtocolChart(data);
        } catch (error) {
            console.error('加载协议统计失败:', error);
        }
    }

    // 加载流量趋势
    async loadTrafficTrend() {
        try {
            const data = await Utils.apiRequest('/api/statistics/trend');
            this.updateTrendChart(data);
        } catch (error) {
            console.error('加载流量趋势失败:', error);
        }
    }

    // 加载Top IP统计
    async loadTopIps() {
        try {
            // 加载Top源IP
            const sourceIps = await Utils.apiRequest('/api/statistics/top-sources');
            this.updateTopSourceIpsTable(sourceIps);

            // 加载Top目标IP
            const destIps = await Utils.apiRequest('/api/statistics/top-destinations');
            this.updateTopDestIpsTable(destIps);
        } catch (error) {
            console.error('加载Top IP统计失败:', error);
        }
    }

    // 加载历史统计
    async loadHistoryStats() {
        try {
            const data = await Utils.apiRequest('/api/statistics/recent?hours=24&limit=20');
            this.updateHistoryStatsTable(data);
        } catch (error) {
            console.error('加载历史统计失败:', error);
        }
    }

    // 初始化图表
    initCharts() {
        // 协议分布饼图
        const protocolCtx = document.getElementById('protocolChart').getContext('2d');
        this.protocolChart = new Chart(protocolCtx, {
            type: 'pie',
            data: {
                labels: [],
                datasets: [{
                    data: [],
                    backgroundColor: [
                        '#FF6384',
                        '#36A2EB',
                        '#FFCE56',
                        '#4BC0C0',
                        '#9966FF',
                        '#FF9F40'
                    ]
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            }
        });

        // 流量趋势折线图
        const trendCtx = document.getElementById('trendChart').getContext('2d');
        this.trendChart = new Chart(trendCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: '数据包数量',
                    data: [],
                    borderColor: '#36A2EB',
                    backgroundColor: 'rgba(54, 162, 235, 0.1)',
                    fill: true,
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }

    // 更新协议分布图
    updateProtocolChart(data) {
        if (!data || data.length === 0) {
            this.protocolChart.data.labels = ['暂无数据'];
            this.protocolChart.data.datasets[0].data = [1];
            this.protocolChart.update();
            return;
        }

        this.protocolChart.data.labels = data.map(item => item.protocol || '未知');
        this.protocolChart.data.datasets[0].data = data.map(item => parseInt(item.count) || 0);
        this.protocolChart.update();
    }

    // 更新流量趋势图
    updateTrendChart(data) {
        if (!data || data.length === 0) {
            this.trendChart.data.labels = ['暂无数据'];
            this.trendChart.data.datasets[0].data = [0];
            this.trendChart.update();
            return;
        }

        // 将时间戳转换为可读格式
        this.trendChart.data.labels = data.map(item => {
            const timeWindow = item.timeWindow || item.time_window;
            if (timeWindow && timeWindow.includes('T')) {
                return new Date(timeWindow).toLocaleTimeString();
            }
            return timeWindow || '未知';
        });
        this.trendChart.data.datasets[0].data = data.map(item => parseInt(item.totalPackets || item.total_packets) || 0);
        this.trendChart.update();
    }

    // 更新Top源IP表格
    updateTopSourceIpsTable(data) {
        const tbody = document.getElementById('topSourceIpsTable');
        
        if (!data || data.length === 0) {
            Utils.showEmpty(tbody, '暂无数据', 3);
            return;
        }
        
        const total = data.reduce((sum, item) => sum + (parseInt(item.count) || 0), 0);
        tbody.innerHTML = data.slice(0, 10).map(item => `
            <tr>
                <td>${item.source_ip || '未知'}</td>
                <td>${item.count || 0}</td>
                <td>${total > 0 ? ((parseInt(item.count) / total) * 100).toFixed(1) : 0}%</td>
            </tr>
        `).join('');
    }

    // 更新Top目标IP表格
    updateTopDestIpsTable(data) {
        const tbody = document.getElementById('topDestIpsTable');
        
        if (!data || data.length === 0) {
            Utils.showEmpty(tbody, '暂无数据', 3);
            return;
        }
        
        const total = data.reduce((sum, item) => sum + (parseInt(item.count) || 0), 0);
        tbody.innerHTML = data.slice(0, 10).map(item => `
            <tr>
                <td>${item.destination_ip || '未知'}</td>
                <td>${item.count || 0}</td>
                <td>${total > 0 ? ((parseInt(item.count) / total) * 100).toFixed(1) : 0}%</td>
            </tr>
        `).join('');
    }

    // 更新历史统计表格
    updateHistoryStatsTable(data) {
        const tbody = document.getElementById('historyStatsTable');
        
        if (!data || data.length === 0) {
            Utils.showEmpty(tbody, '暂无数据', 8);
            return;
        }
        
        tbody.innerHTML = data.map(stat => `
            <tr>
                <td>${Utils.formatTime(stat.statisticsTime)}</td>
                <td>${stat.timeWindow}</td>
                <td>${stat.totalPackets}</td>
                <td>${Utils.formatBytes(stat.totalBytes)}</td>
                <td>${stat.httpPackets}</td>
                <td>${stat.tcpPackets}</td>
                <td>${stat.udpPackets}</td>
                <td>${stat.averagePacketSize ? Math.round(stat.averagePacketSize) + 'B' : '0B'}</td>
            </tr>
        `).join('');
    }

    // 清理资源
    destroy() {
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
        }
        if (this.protocolChart) {
            this.protocolChart.destroy();
        }
        if (this.trendChart) {
            this.trendChart.destroy();
        }
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    window.statisticsPage = new StatisticsPage();
});

// 页面卸载时清理资源
window.addEventListener('beforeunload', function() {
    if (window.statisticsPage) {
        window.statisticsPage.destroy();
    }
});