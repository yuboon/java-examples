const API_URL = 'http://localhost:8080/api';

// 检查是否已登录
function checkAuth() {
    const token = localStorage.getItem('token');
    if (!token) {
        // 未登录，跳转到登录页
        if (window.location.pathname !== '/login.html' &&
            window.location.pathname !== '/register.html') {
            window.location.href = 'login.html';
        }
        return false;
    }
    return true;
}

// 获取当前用户信息
function getCurrentUser() {
    return {
        username: localStorage.getItem('username'),
        nickname: localStorage.getItem('nickname'),
        role: localStorage.getItem('role'),
        avatar: localStorage.getItem('avatar')
    };
}

// 登录处理
async function login(username, password) {
    try {
        const response = await fetch(`${API_URL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText);
        }

        const data = await response.json();

        // 存储认证信息
        localStorage.setItem('token', data.token);
        localStorage.setItem('username', data.username);
        localStorage.setItem('nickname', data.nickname);
        localStorage.setItem('role', data.role);
        localStorage.setItem('avatar', data.avatar);

        return true;
    } catch (error) {
        console.error('Login failed:', error);
        return false;
    }
}

// 注册处理
async function register(userData) {
    try {
        const response = await fetch(`${API_URL}/auth/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(userData)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText);
        }

        return true;
    } catch (error) {
        console.error('Registration failed:', error);
        return false;
    }
}

// 退出登录
function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('nickname');
    localStorage.removeItem('role');
    localStorage.removeItem('avatar');
    window.location.href = 'login.html';
}

// 获取授权头信息
function getAuthHeader() {
    const token = localStorage.getItem('token');
    return {
        'Authorization': `Bearer ${token}`
    };
}

// 页面加载时检查认证状态
document.addEventListener('DOMContentLoaded', function() {
    // 登录页面处理
    if (window.location.pathname.includes('login.html')) {
        const loginForm = document.getElementById('login-btn');
        if (loginForm) {
            loginForm.addEventListener('click', async function() {
                const username = document.getElementById('username').value;
                const password = document.getElementById('password').value;

                if (!username || !password) {
                    alert('请输入用户名和密码');
                    return;
                }

                const success = await login(username, password);
                if (success) {
                    window.location.href = 'index.html';
                } else {
                    alert('登录失败，请检查用户名和密码');
                }
            });
        }
        return;
    }

    // 其他页面需要检查认证
    if (!checkAuth()) {
        return;
    }

    // 显示用户信息
    const userNickname = document.getElementById('user-nickname');
    const userAvatar = document.getElementById('user-avatar');
    const logoutBtn = document.getElementById('logout-btn');

    if (userNickname) {
        userNickname.textContent = getCurrentUser().nickname;
    }

    if (userAvatar) {
        userAvatar.src = getCurrentUser().avatar || 'img/default-avatar.png';
    }

    if (logoutBtn) {
        logoutBtn.addEventListener('click', logout);
    }

    // 根据用户角色显示/隐藏主播控制面板
    const broadcasterControls = document.getElementById('broadcaster-controls');
    if (broadcasterControls && getCurrentUser().role !== 'BROADCASTER') {
        broadcasterControls.style.display = 'none';
    }
});