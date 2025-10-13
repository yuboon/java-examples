// JWT 密钥轮换演示系统 - 前端脚本

class JwtDemoApp {
    constructor() {
        this.apiBase = '';
        this.currentToken = localStorage.getItem('jwtToken') || null;
        this.currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');

        this.init();
    }

    init() {
        this.bindEvents();
        this.loadSystemInfo();
        this.updateLoginStatus();
        this.setupTabNavigation();

        // 定期刷新系统状态
        setInterval(() => this.loadSystemInfo(), 30000);
    }

    setupTabNavigation() {
        const tabBtns = document.querySelectorAll('.tab-btn');
        const tabContents = document.querySelectorAll('.tab-content');

        tabBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const targetTab = btn.dataset.tab;

                // 更新按钮样式
                tabBtns.forEach(b => {
                    b.classList.remove('text-blue-600', 'border-b-2', 'border-blue-600');
                    b.classList.add('text-gray-600');
                });
                btn.classList.remove('text-gray-600');
                btn.classList.add('text-blue-600', 'border-b-2', 'border-blue-600');

                // 切换内容显示
                tabContents.forEach(content => {
                    content.classList.remove('active');
                });
                document.getElementById(targetTab).classList.add('active');
            });
        });
    }

    bindEvents() {
        // 登录表单
        document.getElementById('loginForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.login();
        });

        // 受保护资源访问
        document.getElementById('accessProtected').addEventListener('click', () => {
            this.accessProtectedResource();
        });

        // 密钥信息刷新
        document.getElementById('refreshKeyInfo').addEventListener('click', () => {
            this.loadKeyInfo();
        });

        // Token解析
        document.getElementById('parseToken').addEventListener('click', () => {
            this.parseToken();
        });

        // 管理功能
        document.getElementById('rotateKeys').addEventListener('click', () => {
            this.rotateKeys();
        });

        document.getElementById('cleanupKeys').addEventListener('click', () => {
            this.cleanupKeys();
        });

        // 测试工具
        document.getElementById('generateTestToken').addEventListener('click', () => {
            this.generateTestToken();
        });

        // 状态刷新
        document.getElementById('refreshStatus').addEventListener('click', () => {
            this.loadSystemInfo();
            this.loadKeyInfo();
        });

        // Token输入框回车解析
        document.getElementById('tokenInput').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.parseToken();
            }
        });
    }

    async apiCall(url, options = {}) {
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
            },
        };

        if (this.currentToken) {
            defaultOptions.headers.Authorization = `Bearer ${this.currentToken}`;
        }

        const finalOptions = { ...defaultOptions, ...options };

        try {
            const response = await fetch(this.apiBase + url, finalOptions);
            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || `HTTP ${response.status}`);
            }

            return data;
        } catch (error) {
            console.error('API调用失败:', error);
            throw error;
        }
    }

    async login() {
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;

        if (!username || !password) {
            this.showMessage('请输入用户名和密码', 'error');
            return;
        }

        const loginBtn = document.querySelector('#loginForm button[type="submit"]');
        loginBtn.disabled = true;
        loginBtn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>登录中...';

        try {
            const response = await this.apiCall('/api/auth/login', {
                method: 'POST',
                body: JSON.stringify({ username, password }),
            });

            if (response.success) {
                this.currentToken = response.data.token;
                this.currentUser = {
                    username: response.data.username,
                    expiresIn: response.data.expiresIn,
                    tokenType: response.data.tokenType
                };

                localStorage.setItem('jwtToken', this.currentToken);
                localStorage.setItem('currentUser', JSON.stringify(this.currentUser));

                this.updateLoginStatus();
                this.showMessage('登录成功！', 'success');

                // 清空表单
                document.getElementById('username').value = '';
                document.getElementById('password').value = '';
            } else {
                this.showMessage(response.message || '登录失败', 'error');
            }
        } catch (error) {
            this.showMessage(error.message || '登录失败', 'error');
        } finally {
            loginBtn.disabled = false;
            loginBtn.innerHTML = '<i class="fas fa-sign-in-alt mr-2"></i>登录';
        }
    }

    logout() {
        this.currentToken = null;
        this.currentUser = null;
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('currentUser');
        this.updateLoginStatus();
        this.showMessage('已退出登录', 'info');
    }

    updateLoginStatus() {
        const statusDiv = document.getElementById('loginStatus');

        if (this.currentToken && this.currentUser) {
            const keyId = this.extractKeyIdFromToken(this.currentToken);
            statusDiv.innerHTML = `
                <div class="bg-green-50 border border-green-200 rounded-lg p-4">
                    <div class="flex items-center justify-between mb-3">
                        <h3 class="font-semibold text-green-800">
                            <i class="fas fa-user-check mr-2"></i>已登录
                        </h3>
                        <button onclick="app.logout()" class="text-red-600 hover:text-red-800 text-sm">
                            <i class="fas fa-sign-out-alt mr-1"></i>退出
                        </button>
                    </div>
                    <div class="space-y-2 text-sm">
                        <div><span class="font-medium">用户名:</span> ${this.currentUser.username}</div>
                        <div><span class="font-medium">密钥ID:</span> <code class="bg-gray-100 px-1 rounded">${keyId}</code></div>
                        <div><span class="font-medium">Token类型:</span> ${this.currentUser.tokenType}</div>
                        <div><span class="font-medium">Token预览:</span></div>
                        <div class="bg-gray-100 p-2 rounded text-xs font-mono break-all max-h-32 overflow-y-auto">
                            ${this.currentToken}
                        </div>
                        <div class="text-xs text-gray-500 mt-1">
                            长度: ${this.currentToken.length} 字符
                        </div>
                    </div>
                </div>
            `;
        } else {
            statusDiv.innerHTML = `
                <div class="text-gray-500 text-center py-8">
                    <i class="fas fa-user-slash text-4xl mb-3"></i>
                    <p>尚未登录</p>
                </div>
            `;
        }
    }

    extractKeyIdFromToken(token) {
        try {
            const parts = token.split('.');
            if (parts.length !== 3) return 'N/A';

            const header = JSON.parse(atob(parts[0]));
            return header.kid || 'N/A';
        } catch (error) {
            return 'Parse Error';
        }
    }

    async accessProtectedResource() {
        if (!this.currentToken) {
            this.showMessage('请先登录', 'warning');
            return;
        }

        const btn = document.getElementById('accessProtected');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>访问中...';

        try {
            const response = await this.apiCall('/api/demo/protected');
            document.getElementById('protectedResult').textContent = JSON.stringify(response.data, null, 2);
        } catch (error) {
            document.getElementById('protectedResult').textContent = `错误: ${error.message}`;
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-lock-open mr-2"></i>访问受保护资源';
        }
    }

    async loadKeyInfo() {
        const btn = document.getElementById('refreshKeyInfo');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>刷新中...';

        try {
            const response = await this.apiCall('/api/demo/key-stats');
            document.getElementById('keyInfoResult').textContent = JSON.stringify(response.data, null, 2);
        } catch (error) {
            document.getElementById('keyInfoResult').textContent = `错误: ${error.message}`;
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-sync-alt mr-2"></i>刷新密钥信息';
        }
    }

    async parseToken() {
        let tokenInput = document.getElementById('tokenInput').value.trim();

        if (!tokenInput) {
            this.showMessage('请输入Token', 'warning');
            return;
        }

        // 如果输入的是完整的Authorization header，提取token部分
        if (tokenInput.startsWith('Bearer ')) {
            tokenInput = tokenInput.substring(7);
        }

        const btn = document.getElementById('parseToken');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>解析中...';

        try {
            const response = await this.apiCall('/api/demo/parse-token', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${tokenInput}`,
                }
            });
            document.getElementById('tokenResult').textContent = JSON.stringify(response.data, null, 2);
        } catch (error) {
            document.getElementById('tokenResult').textContent = `错误: ${error.message}`;
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-search mr-2"></i>解析';
        }
    }

    async rotateKeys() {
        if (!confirm('确定要手动轮换密钥吗？这将创建新的密钥对。')) {
            return;
        }

        const btn = document.getElementById('rotateKeys');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>轮换中...';

        try {
            const response = await this.apiCall('/api/auth/admin/rotate-keys', {
                method: 'POST'
            });
            document.getElementById('adminResult').textContent = JSON.stringify(response.data, null, 2);
            this.showMessage('密钥轮换成功', 'success');

            // 刷新密钥信息
            this.loadKeyInfo();
        } catch (error) {
            document.getElementById('adminResult').textContent = `错误: ${error.message}`;
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-redo mr-2"></i>手动轮换密钥';
        }
    }

    async cleanupKeys() {
        if (!confirm('确定要清理过期密钥吗？')) {
            return;
        }

        const btn = document.getElementById('cleanupKeys');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>清理中...';

        try {
            const response = await this.apiCall('/api/auth/admin/cleanup-keys', {
                method: 'POST'
            });
            document.getElementById('adminResult').textContent = JSON.stringify(response.data, null, 2);
            this.showMessage('密钥清理完成', 'success');

            // 刷新密钥信息
            this.loadKeyInfo();
        } catch (error) {
            document.getElementById('adminResult').textContent = `错误: ${error.message}`;
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-broom mr-2"></i>清理过期密钥';
        }
    }

    async generateTestToken() {
        const username = document.getElementById('testUsername').value.trim() || 'demoUser';

        const btn = document.getElementById('generateTestToken');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>生成中...';

        try {
            const response = await this.apiCall(`/api/demo/generate-test-token?username=${encodeURIComponent(username)}`, {
                method: 'POST'
            });
            document.getElementById('testResult').textContent = JSON.stringify(response.data, null, 2);

            // 自动填充到Token输入框
            document.getElementById('tokenInput').value = response.data.token;

            this.showMessage('测试Token生成成功', 'success');
        } catch (error) {
            document.getElementById('testResult').textContent = `错误: ${error.message}`;
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-plus-circle mr-2"></i>生成测试Token';
        }
    }

    async loadSystemInfo() {
        try {
            const response = await this.apiCall('/api/demo/system-info');
            this.displaySystemInfo(response.data);

            // 更新连接状态
            const statusEl = document.getElementById('connectionStatus');
            statusEl.innerHTML = `
                <span class="w-2 h-2 bg-green-400 rounded-full mr-2"></span>
                <span class="text-sm">已连接</span>
            `;
        } catch (error) {
            // 更新连接状态
            const statusEl = document.getElementById('connectionStatus');
            statusEl.innerHTML = `
                <span class="w-2 h-2 bg-red-400 rounded-full mr-2"></span>
                <span class="text-sm">连接失败</span>
            `;

            document.getElementById('systemInfo').innerHTML = `
                <div class="col-span-3 text-center py-4 text-red-500">
                    <i class="fas fa-exclamation-triangle"></i>
                    <p class="mt-2">无法获取系统信息: ${error.message}</p>
                </div>
            `;
        }
    }

    displaySystemInfo(info) {
        const systemInfoDiv = document.getElementById('systemInfo');
        systemInfoDiv.innerHTML = `
            <div class="bg-blue-50 rounded-lg p-4">
                <div class="flex items-center mb-2">
                    <i class="fas fa-info-circle text-blue-600 mr-2"></i>
                    <h3 class="font-semibold text-blue-800">应用信息</h3>
                </div>
                <div class="text-sm space-y-1">
                    <div><strong>名称:</strong> ${info.application}</div>
                    <div><strong>版本:</strong> ${info.version}</div>
                    <div><strong>运行时间:</strong> ${info.uptime}</div>
                </div>
            </div>

            <div class="bg-green-50 rounded-lg p-4">
                <div class="flex items-center mb-2">
                    <i class="fas fa-clock text-green-600 mr-2"></i>
                    <h3 class="font-semibold text-green-800">时间信息</h3>
                </div>
                <div class="text-sm">
                    <div><strong>服务器时间:</strong></div>
                    <div class="font-mono">${info.serverTime}</div>
                </div>
            </div>

            <div class="bg-purple-50 rounded-lg p-4">
                <div class="flex items-center mb-2">
                    <i class="fas fa-memory text-purple-600 mr-2"></i>
                    <h3 class="font-semibold text-purple-800">内存使用</h3>
                </div>
                <div class="text-sm space-y-1">
                    <div><strong>总计:</strong> ${info.memory.total}</div>
                    <div><strong>已用:</strong> ${info.memory.used}</div>
                    <div><strong>空闲:</strong> ${info.memory.free}</div>
                    <div><strong>最大:</strong> ${info.memory.max}</div>
                </div>
            </div>
        `;
    }

    showMessage(message, type = 'info') {
        // 创建消息元素
        const messageDiv = document.createElement('div');
        messageDiv.className = `fixed top-4 right-4 px-4 py-3 rounded-lg shadow-lg z-50 transition-all duration-300 transform translate-x-0`;

        // 根据类型设置样式
        const styles = {
            success: 'bg-green-500 text-white',
            error: 'bg-red-500 text-white',
            warning: 'bg-yellow-500 text-white',
            info: 'bg-blue-500 text-white'
        };

        messageDiv.className += ' ' + (styles[type] || styles.info);

        // 设置图标
        const icons = {
            success: 'fas fa-check-circle',
            error: 'fas fa-exclamation-circle',
            warning: 'fas fa-exclamation-triangle',
            info: 'fas fa-info-circle'
        };

        messageDiv.innerHTML = `
            <div class="flex items-center">
                <i class="${icons[type] || icons.info} mr-2"></i>
                <span>${message}</span>
            </div>
        `;

        // 添加到页面
        document.body.appendChild(messageDiv);

        // 3秒后自动移除
        setTimeout(() => {
            messageDiv.style.transform = 'translateX(400px)';
            setTimeout(() => {
                if (messageDiv.parentNode) {
                    messageDiv.parentNode.removeChild(messageDiv);
                }
            }, 300);
        }, 3000);
    }
}

// 初始化应用
const app = new JwtDemoApp();