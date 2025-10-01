// API 基础地址
const API_BASE_URL = 'http://localhost:8080/api/shamir';

// DOM 元素
const splitForm = document.getElementById('splitForm');
const combineForm = document.getElementById('combineForm');
const secretInput = document.getElementById('secretInput');
const totalSharesInput = document.getElementById('totalShares');
const thresholdInput = document.getElementById('threshold');
const sharesInput = document.getElementById('sharesInput');
const splitResult = document.getElementById('splitResult');
const combineResult = document.getElementById('combineResult');
const sharesContainer = document.getElementById('sharesContainer');
const splitMessage = document.getElementById('splitMessage');
const sessionIdElement = document.getElementById('sessionId');
const combineMessage = document.getElementById('combineMessage');
const combineStatus = document.getElementById('combineStatus');
const recoveredSecret = document.getElementById('recoveredSecret');
const recoveredSecretContainer = document.getElementById('recoveredSecretContainer');
const requiredShares = document.getElementById('requiredShares');

// 更新门限值显示
thresholdInput.addEventListener('input', () => {
    requiredShares.textContent = thresholdInput.value;
});

// 拆分密钥表单提交
splitForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const secret = secretInput.value.trim();
    const totalShares = parseInt(totalSharesInput.value);
    const threshold = parseInt(thresholdInput.value);

    // 参数校验
    if (!secret) {
        showError('请输入原始密钥');
        return;
    }

    if (threshold > totalShares) {
        showError('门限值不能大于总份额数');
        return;
    }

    if (threshold < 2 || totalShares < 2) {
        showError('门限值和总份额数必须至少为 2');
        return;
    }

    try {
        // 显示加载状态
        const submitButton = splitForm.querySelector('button[type="submit"]');
        const originalText = submitButton.textContent;
        submitButton.textContent = '🔄 处理中...';
        submitButton.disabled = true;

        // 调用后端 API
        const response = await fetch(`${API_BASE_URL}/split`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                secret: secret,
                totalShares: totalShares,
                threshold: threshold
            })
        });

        const data = await response.json();

        if (response.ok && data.shares) {
            // 显示拆分结果
            displaySplitResult(data);

            // 自动填充门限值到右侧提示
            requiredShares.textContent = threshold;
        } else {
            showError(data.message || '拆分失败，请检查输入参数');
        }

        // 恢复按钮状态
        submitButton.textContent = originalText;
        submitButton.disabled = false;

    } catch (error) {
        console.error('Error:', error);
        showError('网络错误，请确保后端服务已启动（端口 8080）');

        // 恢复按钮状态
        const submitButton = splitForm.querySelector('button[type="submit"]');
        submitButton.textContent = '🚀 开始拆分';
        submitButton.disabled = false;
    }
});

// 恢复密钥表单提交
combineForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const sharesText = sharesInput.value.trim();

    if (!sharesText) {
        showCombineError('请输入密钥份额');
        return;
    }

    // 解析份额（每行一个）
    const shares = sharesText
        .split('\n')
        .map(line => line.trim())
        .filter(line => line.length > 0);

    if (shares.length < 2) {
        showCombineError('至少需要 2 个密钥份额');
        return;
    }

    try {
        // 显示加载状态
        const submitButton = combineForm.querySelector('button[type="submit"]');
        const originalText = submitButton.textContent;
        submitButton.textContent = '🔄 处理中...';
        submitButton.disabled = true;

        // 调用后端 API
        const response = await fetch(`${API_BASE_URL}/combine`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                shares: shares
            })
        });

        const data = await response.json();

        if (response.ok && data.success) {
            // 显示恢复成功结果
            displayCombineSuccess(data);
        } else {
            showCombineError(data.message || '恢复失败，请检查份额格式');
        }

        // 恢复按钮状态
        submitButton.textContent = originalText;
        submitButton.disabled = false;

    } catch (error) {
        console.error('Error:', error);
        showCombineError('网络错误，请确保后端服务已启动（端口 8080）');

        // 恢复按钮状态
        const submitButton = combineForm.querySelector('button[type="submit"]');
        submitButton.textContent = '🔓 恢复密钥';
        submitButton.disabled = false;
    }
});

// 显示拆分结果
function displaySplitResult(data) {
    splitMessage.textContent = data.message;
    sessionIdElement.textContent = `会话 ID: ${data.sessionId}`;

    // 清空之前的份额
    sharesContainer.innerHTML = '';

    // 显示每个份额
    data.shares.forEach((share, index) => {
        const shareCard = document.createElement('div');
        shareCard.className = 'share-card bg-gray-50 border border-gray-300 rounded-lg p-4 hover:bg-gray-100';

        shareCard.innerHTML = `
            <div class="flex justify-between items-start mb-2">
                <span class="font-semibold text-purple-700">份额 ${index + 1}</span>
                <button onclick="copyShare(${index})"
                        class="text-sm bg-purple-500 hover:bg-purple-600 text-white px-3 py-1 rounded transition">
                    📋 复制
                </button>
            </div>
            <div class="bg-white border border-gray-200 rounded p-2">
                <code id="share-${index}" class="text-xs text-gray-700 break-all font-mono">${share}</code>
            </div>
        `;

        sharesContainer.appendChild(shareCard);
    });

    splitResult.classList.remove('hidden');

    // 平滑滚动到结果区域
    splitResult.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

// 显示恢复成功结果
function displayCombineSuccess(data) {
    combineStatus.className = 'bg-green-50 border border-green-200 rounded-lg p-4 mb-4';
    combineMessage.className = 'text-green-800 font-medium';
    combineMessage.textContent = data.message;

    recoveredSecret.textContent = data.secret;
    recoveredSecretContainer.classList.remove('hidden');

    combineResult.classList.remove('hidden');
    combineResult.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

// 显示拆分错误
function showError(message) {
    alert('❌ 错误：' + message);
}

// 显示恢复错误
function showCombineError(message) {
    combineStatus.className = 'bg-red-50 border border-red-200 rounded-lg p-4 mb-4';
    combineMessage.className = 'text-red-800 font-medium';
    combineMessage.textContent = message;

    recoveredSecretContainer.classList.add('hidden');
    combineResult.classList.remove('hidden');
    combineResult.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

// 复制份额到剪贴板
function copyShare(index) {
    const shareElement = document.getElementById(`share-${index}`);
    const shareText = shareElement.textContent;

    // 使用现代 Clipboard API
    if (navigator.clipboard) {
        navigator.clipboard.writeText(shareText).then(() => {
            // 显示复制成功提示
            showToast('✅ 份额已复制到剪贴板');
        }).catch(err => {
            console.error('复制失败:', err);
            fallbackCopy(shareText);
        });
    } else {
        fallbackCopy(shareText);
    }
}

// 降级复制方案
function fallbackCopy(text) {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();

    try {
        document.execCommand('copy');
        showToast('✅ 份额已复制到剪贴板');
    } catch (err) {
        console.error('复制失败:', err);
        alert('复制失败，请手动复制');
    }

    document.body.removeChild(textarea);
}

// 显示临时提示
function showToast(message) {
    const toast = document.createElement('div');
    toast.className = 'fixed bottom-4 right-4 bg-gray-800 text-white px-6 py-3 rounded-lg shadow-lg z-50 animate-fade-in';
    toast.textContent = message;

    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity 0.3s ease';
        setTimeout(() => document.body.removeChild(toast), 300);
    }, 2000);
}

// 自动连接测试
window.addEventListener('load', async () => {
    try {
        const response = await fetch(`${API_BASE_URL}/health`);
        if (response.ok) {
            console.log('✅ 后端服务连接成功');
        }
    } catch (error) {
        console.warn('⚠️ 后端服务未启动，请先启动 Spring Boot 应用');
        showToast('⚠️ 后端服务未启动，请先启动 Spring Boot 应用');
    }
});