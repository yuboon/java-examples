// API配置
const API_BASE_URL = '';

// 获取Token
export function getToken() {
    return localStorage.getItem('token') || sessionStorage.getItem('token');
}

// 设置Token
export function setToken(token, remember) {
    if (remember) {
        localStorage.setItem('token', token);
    } else {
        sessionStorage.setItem('token', token);
    }
}

// 清除Token
export function clearToken() {
    localStorage.removeItem('token');
    sessionStorage.removeItem('token');
}

// API请求封装
async function apiRequest(url, options = {}) {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    if (token) {
        options.headers = {
            ...options.headers,
            'Authorization': token
        };
    }

    try {
        const response = await fetch(API_BASE_URL + url, options);

        // 如果返回401，说明token失效，跳转到登录页
        if (response.status === 401) {
            clearToken();
            // 只有不在登录页时才跳转
            if (window.location.pathname !== '/login.html') {
                window.location.href = '/login.html';
            }
            return null;
        }

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return await response.json();
    } catch (error) {
        console.error('API请求失败:', error);
        throw error;
    }
}

// 登录API
export async function loginApi(username, password) {
    return apiRequest('/api/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            username: username,
            password: password
        })
    });
}

// 登出API
export async function logoutApi() {
    try {
        await apiRequest('/api/auth/logout', {
            method: 'POST'
        });
    } finally {
        clearToken();
    }
}

// 获取当前用户信息API
export async function getCurrentUserApi() {
    return apiRequest('/api/auth/current');
}

// 获取在线用户API
export async function getOnlineUsersApi() {
    return apiRequest('/api/auth/online');
}

// 获取用户Token列表API
export async function getUserTokensApi(username) {
    return apiRequest(`/api/auth/tokens?username=${username}`);
}

// 踢出用户API
export async function kickoutUserApi(username) {
    return apiRequest(`/api/auth/kickout?username=${username}`, {
        method: 'POST'
    });
}

// 检查登录状态
export async function checkLoginStatus() {
    const token = getToken();
    if (!token) {
        return false;
    }

    try {
        const response = await apiRequest('/api/auth/current');
        return response && response.code === 200;
    } catch (error) {
        return false;
    }
}