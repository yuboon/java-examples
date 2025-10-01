// API 基础配置
const API_BASE = 'http://localhost:8080/api';

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    loadHardwareInfo();
    checkKeyStatus();
    setDefaultExpireDate();
});

// 设置默认到期时间（一年后）
function setDefaultExpireDate() {
    const tomorrow = new Date();
    tomorrow.setFullYear(tomorrow.getFullYear() + 1);
    document.getElementById('expireAt').value = tomorrow.toISOString().split('T')[0];
}

// 显示消息提示
function showToast(title, message, type = 'info') {
    const toast = document.getElementById('toast');
    const icon = document.getElementById('toastIcon');
    const titleEl = document.getElementById('toastTitle');
    const messageEl = document.getElementById('toastMessage');

    // 设置图标和颜色
    const types = {
        success: { icon: '✅', color: 'text-green-600' },
        error: { icon: '❌', color: 'text-red-600' },
        warning: { icon: '⚠️', color: 'text-yellow-600' },
        info: { icon: 'ℹ️', color: 'text-blue-600' }
    };

    const config = types[type] || types.info;
    icon.textContent = config.icon;
    titleEl.textContent = title;
    titleEl.className = `font-medium ${config.color}`;
    messageEl.textContent = message;

    // 显示动画
    toast.classList.remove('translate-x-full');
    toast.classList.add('translate-x-0');

    // 3秒后自动隐藏
    setTimeout(() => {
        toast.classList.remove('translate-x-0');
        toast.classList.add('translate-x-full');
    }, 3000);
}

// 复制到剪贴板
function copyToClipboard(elementId) {
    const element = document.getElementById(elementId);
    element.select();
    document.execCommand('copy');
    showToast('复制成功', '内容已复制到剪贴板', 'success');
}

// 加载硬件信息
async function loadHardwareInfo() {
    try {
        const response = await fetch(`${API_BASE}/hardware/info`);
        const result = await response.json();

        if (result.success) {
            document.getElementById('motherboardSerial').textContent = result.data.motherboardSerial;
            document.getElementById('systemInfo').textContent = result.data.systemInfo;
        } else {
            showToast('错误', '获取硬件信息失败', 'error');
        }
    } catch (error) {
        console.error('获取硬件信息失败:', error);
        showToast('错误', '无法连接到服务器', 'error');
    }
}

// 检查密钥状态
async function checkKeyStatus() {
    try {
        const response = await fetch(`${API_BASE}/keys/status`);
        const result = await response.json();

        const statusEl = document.getElementById('keyStatus');
        if (result.keysLoaded) {
            statusEl.textContent = '✅ 密钥已加载';
            statusEl.className = 'text-sm text-green-600';
        } else {
            statusEl.textContent = '❌ 密钥未加载';
            statusEl.className = 'text-sm text-red-600';
        }
    } catch (error) {
        console.error('检查密钥状态失败:', error);
    }
}

// 生成密钥对
async function generateKeys() {
    try {
        showToast('处理中', '正在生成密钥对...', 'info');

        const response = await fetch(`${API_BASE}/keys/generate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();

        if (result.success) {
            document.getElementById('privateKey').value = result.data.privateKey;
            document.getElementById('publicKey').value = result.data.publicKey;
            showToast('成功', '密钥对生成成功', 'success');
            checkKeyStatus();
        } else {
            showToast('错误', result.message, 'error');
        }
    } catch (error) {
        console.error('生成密钥失败:', error);
        showToast('错误', '生成密钥失败', 'error');
    }
}

// 加载密钥
async function loadKeys() {
    try {
        const privateKey = document.getElementById('privateKey').value.trim();
        const publicKey = document.getElementById('publicKey').value.trim();

        if (!privateKey && !publicKey) {
            showToast('警告', '请输入至少一个密钥', 'warning');
            return;
        }

        const response = await fetch(`${API_BASE}/keys/load`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                privateKey: privateKey,
                publicKey: publicKey
            })
        });

        const result = await response.json();

        if (result.success) {
            showToast('成功', result.message, 'success');
            checkKeyStatus();
        } else {
            showToast('错误', result.message, 'error');
        }
    } catch (error) {
        console.error('加载密钥失败:', error);
        showToast('错误', '加载密钥失败', 'error');
    }
}

// 生成许可证
async function generateLicense() {
    try {
        // 收集表单数据
        const subject = document.getElementById('subject').value.trim();
        const issuedTo = document.getElementById('issuedTo').value.trim();
        const expireAt = document.getElementById('expireAt').value;
        const featuresStr = document.getElementById('features').value.trim();

        // 验证必填字段
        if (!subject || !issuedTo || !expireAt) {
            showToast('警告', '请填写所有必填字段', 'warning');
            return;
        }

        // 解析功能权限
        const features = featuresStr.split(',').map(f => f.trim()).filter(f => f);

        const licenseData = {
            subject: subject,
            issuedTo: issuedTo,
            expireAt: expireAt,
            features: features
        };

        showToast('处理中', '正在生成许可证...', 'info');

        const response = await fetch(`${API_BASE}/license/generate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(licenseData)
        });

        const result = await response.json();

        if (result.success) {
            document.getElementById('generatedLicense').value = result.data;
            showToast('成功', '许可证生成成功', 'success');
        } else {
            showToast('错误', result.message, 'error');
        }
    } catch (error) {
        console.error('生成许可证失败:', error);
        showToast('错误', '生成许可证失败', 'error');
    }
}

// 验证许可证
async function verifyLicense() {
    try {
        const licenseJson = document.getElementById('licenseToVerify').value.trim();

        if (!licenseJson) {
            showToast('警告', '请输入许可证内容', 'warning');
            return;
        }

        showToast('处理中', '正在验证许可证...', 'info');

        const response = await fetch(`${API_BASE}/license/verify`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                licenseJson: licenseJson
            })
        });

        const result = await response.json();
        const resultDiv = document.getElementById('verifyResult');

        if (result.success) {
            resultDiv.innerHTML = `
                <div class="bg-green-50 border border-green-200 rounded-lg p-4">
                    <div class="flex items-center mb-2">
                        <span class="text-green-600 text-lg mr-2">✅</span>
                        <h3 class="font-medium text-green-800">许可证验证成功</h3>
                    </div>
                    <p class="text-green-700 mb-3">${result.message}</p>
                    ${result.license ? `
                    <div class="bg-white rounded p-3 text-sm">
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
                            <div><strong>软件名称:</strong> ${result.license.subject}</div>
                            <div><strong>授权给:</strong> ${result.license.issuedTo}</div>
                            <div><strong>硬件ID:</strong> ${result.license.hardwareId}</div>
                            <div><strong>到期时间:</strong> ${result.license.expireAt}</div>
                            <div class="md:col-span-2"><strong>功能权限:</strong> ${result.license.features.join(', ')}</div>
                        </div>
                    </div>
                    ` : ''}
                </div>
            `;
            showToast('成功', '许可证验证通过', 'success');
        } else {
            resultDiv.innerHTML = `
                <div class="bg-red-50 border border-red-200 rounded-lg p-4">
                    <div class="flex items-center mb-2">
                        <span class="text-red-600 text-lg mr-2">❌</span>
                        <h3 class="font-medium text-red-800">许可证验证失败</h3>
                    </div>
                    <p class="text-red-700">${result.message}</p>
                </div>
            `;
            showToast('失败', '许可证验证失败', 'error');
        }
    } catch (error) {
        console.error('验证许可证失败:', error);
        showToast('错误', '验证许可证失败', 'error');

        document.getElementById('verifyResult').innerHTML = `
            <div class="bg-red-50 border border-red-200 rounded-lg p-4">
                <div class="flex items-center mb-2">
                    <span class="text-red-600 text-lg mr-2">❌</span>
                    <h3 class="font-medium text-red-800">验证过程出错</h3>
                </div>
                <p class="text-red-700">无法连接到服务器或请求格式错误</p>
            </div>
        `;
    }
}