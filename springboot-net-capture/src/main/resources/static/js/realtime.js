// 实时监控页面功能
class RealtimePage {
    constructor() {
        this.websocket = null;
        this.logCount = 0;
        this.realtimeStats = {
            packets: 0,
            bytes: 0,
            startTime: null
        };
        this.maxLogEntries = 500;
        this.maxTableRows = 50;
        this.recentPackets = [];
        this.init();
    }

    init() {
        this.bindEvents();
        this.connectWebSocket();
    }

    bindEvents() {
        document.getElementById('connectBtn').addEventListener('click', () => this.connectWebSocket());
        document.getElementById('disconnectBtn').addEventListener('click', () => this.disconnectWebSocket());
        document.getElementById('clearBtn').addEventListener('click', () => this.clearLog());
        document.getElementById('applyFilterBtn').addEventListener('click', () => this.applyFilter());
    }

    // 连接WebSocket
    connectWebSocket() {
        if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
            return;
        }

        const wsUrl = `${Config.WS_BASE_URL}//${window.location.host}/ws/packets`;
        this.websocket = new WebSocket(wsUrl);
        this.realtimeStats.startTime = new Date();

        this.websocket.onopen = () => {
            this.updateConnectionStatus(true);
            this.addLogEntry('系统', '已连接到实时数据流');
            this.applyFilter(); // 应用当前过滤器
        };

        this.websocket.onmessage = (event) => {
            try {
                const packet = JSON.parse(event.data);
                this.handlePacketData(packet);
            } catch (error) {
                console.error('解析数据包失败:', error);
            }
        };

        this.websocket.onclose = () => {
            this.updateConnectionStatus(false);
            this.addLogEntry('系统', '连接已断开');
            
            // 5秒后尝试重连
            setTimeout(() => {
                if (!this.websocket || this.websocket.readyState === WebSocket.CLOSED) {
                    this.connectWebSocket();
                }
            }, Config.WEBSOCKET_RECONNECT_DELAY);
        };

        this.websocket.onerror = (error) => {
            console.error('WebSocket错误:', error);
            this.addLogEntry('错误', '连接发生错误');
        };
    }

    // 断开WebSocket连接
    disconnectWebSocket() {
        if (this.websocket) {
            this.websocket.close();
            this.websocket = null;
        }
    }

    // 更新连接状态
    updateConnectionStatus(connected) {
        const statusIndicator = document.getElementById('connectionStatus');
        const connectBtn = document.getElementById('connectBtn');
        const disconnectBtn = document.getElementById('disconnectBtn');

        if (connected) {
            statusIndicator.className = 'status-indicator bg-success';
            connectBtn.disabled = true;
            disconnectBtn.disabled = false;
        } else {
            statusIndicator.className = 'status-indicator bg-danger';
            connectBtn.disabled = false;
            disconnectBtn.disabled = true;
        }
    }

    // 处理数据包数据
    handlePacketData(packet) {
        // 更新统计
        this.realtimeStats.packets++;
        this.realtimeStats.bytes += (packet.packetLength || 0);
        this.updateRealtimeStats();

        // 添加到日志
        const logMessage = this.formatPacketLog(packet);
        this.addLogEntry(packet.protocol, logMessage, packet.protocol);

        // 添加到最新数据包表格
        this.addToRecentPacketsTable(packet);
    }

    // 格式化数据包日志
    formatPacketLog(packet) {
        let message = '';
        
        if (packet.sourceIp && packet.destinationIp) {
            message += `${packet.sourceIp}${packet.sourcePort ? ':' + packet.sourcePort : ''} -> `;
            message += `${packet.destinationIp}${packet.destinationPort ? ':' + packet.destinationPort : ''}`;
        }
        
        if (packet.httpMethod) {
            message += ` [${packet.httpMethod}]`;
        }
        
        if (packet.httpUrl) {
            message += ` ${packet.httpUrl}`;
        }
        
        if (packet.httpStatus) {
            message += ` [${packet.httpStatus}]`;
        }
        
        if (packet.packetLength) {
            message += ` (${packet.packetLength}B)`;
        }

        return message || '数据包详情不可用';
    }

    // 添加日志条目
    addLogEntry(type, message, protocol) {
        const logDiv = document.getElementById('realTimeLog');
        const now = new Date().toLocaleTimeString('zh-CN');
        
        const logEntry = document.createElement('div');
        logEntry.className = 'log-entry';
        
        const protocolClass = protocol ? `log-protocol-${protocol}` : '';
        
        logEntry.innerHTML = `
            <span class="log-time">[${now}]</span> 
            <span class="${protocolClass}">[${type}]</span> 
            <span>${message}</span>
        `;
        
        logDiv.appendChild(logEntry);
        
        // 限制日志条数
        const entries = logDiv.getElementsByClassName('log-entry');
        if (entries.length > this.maxLogEntries) {
            logDiv.removeChild(entries[0]);
        }
        
        // 自动滚动到底部
        logDiv.scrollTop = logDiv.scrollHeight;
        
        this.logCount++;
        document.getElementById('logCount').textContent = this.logCount;
    }

    // 添加到最新数据包表格
    addToRecentPacketsTable(packet) {
        this.recentPackets.unshift(packet);
        if (this.recentPackets.length > this.maxTableRows) {
            this.recentPackets = this.recentPackets.slice(0, this.maxTableRows);
        }
        
        this.updateRecentPacketsTable();
    }

    // 更新最新数据包表格
    updateRecentPacketsTable() {
        const tbody = document.getElementById('recentPacketsTable');
        
        if (this.recentPackets.length === 0) {
            Utils.showEmpty(tbody, '暂无数据', 6);
            return;
        }
        
        tbody.innerHTML = this.recentPackets.map(packet => `
            <tr>
                <td>${Utils.formatTime(packet.captureTime)}</td>
                <td><span class="badge ${Utils.getProtocolBadgeClass(packet.protocol)} badge-sm">${packet.protocol}</span></td>
                <td>${packet.sourceIp || ''}${packet.sourcePort ? ':' + packet.sourcePort : ''}</td>
                <td>${packet.destinationIp || ''}${packet.destinationPort ? ':' + packet.destinationPort : ''}</td>
                <td>${packet.packetLength || 0} B</td>
                <td class="text-truncate" style="max-width: 300px;" title="${this.formatPacketLog(packet)}">${this.formatPacketLog(packet)}</td>
            </tr>
        `).join('');
    }

    // 更新实时统计
    updateRealtimeStats() {
        const now = new Date();
        const elapsed = (now - this.realtimeStats.startTime) / 1000 / 60; // 分钟
        
        if (elapsed > 0) {
            const packetsPerMin = Math.round(this.realtimeStats.packets / elapsed);
            const bytesPerMin = Math.round(this.realtimeStats.bytes / elapsed);
            
            document.getElementById('realtimePackets').textContent = packetsPerMin;
            document.getElementById('realtimeBytes').textContent = Utils.formatBytes(bytesPerMin);
        }
    }

    // 清空日志
    clearLog() {
        document.getElementById('realTimeLog').innerHTML = '';
        this.logCount = 0;
        document.getElementById('logCount').textContent = '0';
        this.recentPackets = [];
        this.updateRecentPacketsTable();
        this.realtimeStats = { packets: 0, bytes: 0, startTime: new Date() };
        this.updateRealtimeStats();
    }

    // 应用过滤器
    applyFilter() {
        if (!this.websocket || this.websocket.readyState !== WebSocket.OPEN) {
            return;
        }
        
        const protocol = document.getElementById('protocolFilter').value;
        const ip = document.getElementById('ipFilter').value;
        const url = document.getElementById('urlFilter').value;
        
        let filterString = '';
        
        if (protocol) filterString += protocol + ' ';
        if (ip) filterString += ip + ' ';
        if (url) filterString += url + ' ';
        
        this.websocket.send('filter:' + filterString.trim());
        
        this.addLogEntry('系统', `已应用过滤器: ${filterString.trim() || '无过滤'}`);
    }

    // 清理资源
    destroy() {
        if (this.websocket) {
            this.websocket.close();
        }
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    window.realtimePage = new RealtimePage();
});

// 页面关闭时断开连接
window.addEventListener('beforeunload', function() {
    if (window.realtimePage) {
        window.realtimePage.destroy();
    }
});