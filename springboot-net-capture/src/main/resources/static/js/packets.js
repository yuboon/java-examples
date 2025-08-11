// 数据包列表页面功能
class PacketsPage {
    constructor() {
        this.currentPage = 1;
        this.pageSize = Config.DEFAULT_PAGE_SIZE;
        this.totalPages = 1;
        this.init();
    }

    init() {
        this.bindEvents();
        this.loadPackets();
    }

    bindEvents() {
        // 搜索按钮事件
        document.getElementById('searchBtn').addEventListener('click', () => {
            this.currentPage = 1;
            this.loadPackets();
        });

        // 回车键搜索
        document.addEventListener('keypress', (event) => {
            if (event.key === 'Enter') {
                this.currentPage = 1;
                this.loadPackets();
            }
        });
    }

    // 加载数据包列表
    async loadPackets() {
        const protocol = document.getElementById('protocolFilter').value;
        const sourceIp = document.getElementById('sourceIpFilter').value;
        const destIp = document.getElementById('destIpFilter').value;
        const sourcePort = document.getElementById('sourcePortFilter').value;
        const destPort = document.getElementById('destPortFilter').value;

        const params = new URLSearchParams({
            pageNo: this.currentPage,
            pageSize: this.pageSize
        });

        if (protocol) params.append('protocol', protocol);
        if (sourceIp) params.append('sourceIp', sourceIp);
        if (destIp) params.append('destinationIp', destIp);
        if (sourcePort) params.append('sourcePort', sourcePort);
        if (destPort) params.append('destinationPort', destPort);

        Utils.showLoading('packetTableBody', '加载数据包...');

        try {
            const data = await Utils.apiRequest(`/api/packets?${params.toString()}`);
            this.displayPackets(data.records);
            this.updatePagination(data.current, data.pages, data.total);
        } catch (error) {
            console.error('加载数据包失败:', error);
            Utils.showError('packetTableBody', '加载失败');
        }
    }

    // 显示数据包列表
    displayPackets(packets) {
        const tbody = document.getElementById('packetTableBody');
        
        if (!packets || packets.length === 0) {
            Utils.showEmpty(tbody, '暂无数据', 6);
            return;
        }

        tbody.innerHTML = packets.map(packet => `
            <tr class="packet-row" onclick="packetsPage.showPacketDetail(${packet.id})">
                <td>${Utils.formatTime(packet.captureTime)}</td>
                <td>
                    <span class="badge ${Utils.getProtocolBadgeClass(packet.protocol)} protocol-badge">
                        ${packet.protocol}
                    </span>
                </td>
                <td>${packet.sourceIp || ''}${packet.sourcePort ? ':' + packet.sourcePort : ''}</td>
                <td>${packet.destinationIp || ''}${packet.destinationPort ? ':' + packet.destinationPort : ''}</td>
                <td>${packet.packetLength || 0} B</td>
                <td class="text-truncate-custom" title="${packet.summary || ''}">${packet.summary || ''}</td>
            </tr>
        `).join('');
    }

    // 更新分页
    updatePagination(current, pages, total) {
        this.currentPage = current;
        this.totalPages = pages;
        
        const pagination = document.getElementById('pagination');
        let html = '';

        // 上一页
        html += `<li class="page-item ${current <= 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="${current > 1 ? 'packetsPage.goToPage(' + (current - 1) + ')' : 'return false;'}">上一页</a>
        </li>`;

        // 页码
        const startPage = Math.max(1, current - 2);
        const endPage = Math.min(pages, current + 2);

        if (startPage > 1) {
            html += `<li class="page-item"><a class="page-link" href="#" onclick="packetsPage.goToPage(1)">1</a></li>`;
            if (startPage > 2) {
                html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
        }

        for (let i = startPage; i <= endPage; i++) {
            html += `<li class="page-item ${i === current ? 'active' : ''}">
                <a class="page-link" href="#" onclick="packetsPage.goToPage(${i})">${i}</a>
            </li>`;
        }

        if (endPage < pages) {
            if (endPage < pages - 1) {
                html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
            html += `<li class="page-item"><a class="page-link" href="#" onclick="packetsPage.goToPage(${pages})">${pages}</a></li>`;
        }

        // 下一页
        html += `<li class="page-item ${current >= pages ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="${current < pages ? 'packetsPage.goToPage(' + (current + 1) + ')' : 'return false;'}">下一页</a>
        </li>`;

        pagination.innerHTML = html;
    }

    // 跳转到指定页面
    goToPage(page) {
        this.currentPage = page;
        this.loadPackets();
    }

    // 显示数据包详情
    async showPacketDetail(packetId) {
        try {
            const packet = await Utils.apiRequest(`/api/packets/${packetId}`);
            const content = document.getElementById('packetDetailContent');
            content.innerHTML = this.generatePacketDetailHtml(packet);
            
            new bootstrap.Modal(document.getElementById('packetDetailModal')).show();
        } catch (error) {
            console.error('获取数据包详情失败:', error);
            Notification.error('获取数据包详情失败');
        }
    }

    // 生成数据包详情HTML
    generatePacketDetailHtml(packet) {
        return `
            <div class="row">
                <div class="col-md-6">
                    <h6>基本信息</h6>
                    <table class="table table-sm">
                        <tr><td><strong>ID:</strong></td><td>${packet.id}</td></tr>
                        <tr><td><strong>捕获时间:</strong></td><td>${Utils.formatTime(packet.captureTime)}</td></tr>
                        <tr><td><strong>协议:</strong></td><td>${packet.protocol}</td></tr>
                        <tr><td><strong>数据包长度:</strong></td><td>${packet.packetLength} 字节</td></tr>
                        <tr><td><strong>网络接口:</strong></td><td>${packet.networkInterface || ''}</td></tr>
                    </table>
                </div>
                <div class="col-md-6">
                    <h6>网络信息</h6>
                    <table class="table table-sm">
                        <tr><td><strong>源IP:</strong></td><td>${packet.sourceIp || ''}</td></tr>
                        <tr><td><strong>源端口:</strong></td><td>${packet.sourcePort || ''}</td></tr>
                        <tr><td><strong>目标IP:</strong></td><td>${packet.destinationIp || ''}</td></tr>
                        <tr><td><strong>目标端口:</strong></td><td>${packet.destinationPort || ''}</td></tr>
                    </table>
                </div>
            </div>
            ${packet.httpMethod || packet.httpUrl || packet.httpStatus ? `
            <div class="row mt-3">
                <div class="col-12">
                    <h6>HTTP 信息</h6>
                    <table class="table table-sm">
                        ${packet.httpMethod ? `<tr><td><strong>方法:</strong></td><td>${packet.httpMethod}</td></tr>` : ''}
                        ${packet.httpUrl ? `<tr><td><strong>URL:</strong></td><td>${packet.httpUrl}</td></tr>` : ''}
                        ${packet.httpStatus ? `<tr><td><strong>状态码:</strong></td><td>${packet.httpStatus}</td></tr>` : ''}
                    </table>
                </div>
            </div>
            ` : ''}
            ${packet.httpHeaders ? `
            <div class="row mt-3">
                <div class="col-12">
                    <h6>HTTP 头部</h6>
                    <pre class="bg-light p-2 rounded">${packet.httpHeaders}</pre>
                </div>
            </div>
            ` : ''}
            ${packet.payload ? `
            <div class="row mt-3">
                <div class="col-12">
                    <h6>载荷数据</h6>
                    <pre class="bg-light p-2 rounded" style="max-height: 200px; overflow-y: auto;">${packet.payload}</pre>
                </div>
            </div>
            ` : ''}
        `;
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    window.packetsPage = new PacketsPage();
});