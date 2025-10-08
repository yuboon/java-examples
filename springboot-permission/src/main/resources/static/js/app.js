// API 配置
const API_BASE = 'http://localhost:8080/api';

// 当前用户信息
let currentUser = {
    id: '',
    name: '',
    dept: ''
};

// 页面加载完成
document.addEventListener('DOMContentLoaded', () => {
    loadDocuments();
    loadPolicies();
});

// 切换用户
function switchUser() {
    const select = document.getElementById('userSelect');
    const value = select.value;

    if (!value) {
        showToast('请选择用户', 'error');
        return;
    }

    const [id, name, dept] = value.split(',');
    currentUser = { id, name, dept };

    document.getElementById('currentUser').textContent = `已登录：${name} (${dept})`;
    showToast(`已切换到用户：${name}`, 'success');

    loadDocuments();
}

// 切换标签页
function switchTab(tab) {
    // 切换标签样式
    document.querySelectorAll('[id^="tab-"]').forEach(el => {
        el.classList.remove('border-blue-500', 'text-blue-500');
        el.classList.add('text-gray-500');
    });
    document.getElementById(`tab-${tab}`).classList.add('border-blue-500', 'text-blue-500');
    document.getElementById(`tab-${tab}`).classList.remove('text-gray-500');

    // 切换内容
    document.querySelectorAll('[id^="content-"]').forEach(el => el.classList.add('hidden'));
    document.getElementById(`content-${tab}`).classList.remove('hidden');

    // 加载数据
    if (tab === 'policies') {
        loadPolicies();
    }
}

// 加载文档列表
async function loadDocuments() {
    try {
        const response = await axios.get(`${API_BASE}/documents`);
        const documents = response.data.data || [];

        const tbody = document.getElementById('documentList');
        tbody.innerHTML = documents.map(doc => `
            <tr>
                <td class="border p-2">${doc.id}</td>
                <td class="border p-2">${doc.title || '-'}</td>
                <td class="border p-2">${doc.ownerId}</td>
                <td class="border p-2">${doc.dept}</td>
                <td class="border p-2">
                    <span class="px-2 py-1 rounded text-sm ${getTypeColor(doc.type)}">
                        ${doc.type}
                    </span>
                </td>
                <td class="border p-2">
                    <button onclick="editDocument('${doc.id}')" class="bg-blue-500 text-white px-2 py-1 rounded text-sm hover:bg-blue-600 mr-1">编辑</button>
                    <button onclick="deleteDocument('${doc.id}')" class="bg-red-500 text-white px-2 py-1 rounded text-sm hover:bg-red-600">删除</button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('加载文档失败', error);
        showToast('加载文档失败', 'error');
    }
}

// 创建文档
async function createDocument() {
    const title = document.getElementById('docTitle').value;
    const ownerId = document.getElementById('docOwnerId').value;
    const dept = document.getElementById('docDept').value;
    const type = document.getElementById('docType').value;

    if (!title || !ownerId || !dept) {
        showToast('请填写完整信息', 'error');
        return;
    }

    try {
        await axios.post(`${API_BASE}/documents`, {
            title, ownerId, dept, type
        }, getHeaders());

        showToast('创建成功', 'success');
        clearDocumentForm();
        loadDocuments();
    } catch (error) {
        showToast(error.response?.data?.message || '创建失败', 'error');
    }
}

// 编辑文档
async function editDocument(id) {
    if (!currentUser.id) {
        showToast('请先选择用户', 'error');
        return;
    }

    const newTitle = prompt('请输入新标题：');
    if (!newTitle) return;

    try {
        // 先获取文档信息
        const getResp = await axios.get(`${API_BASE}/documents/${id}`, getHeaders());
        const doc = getResp.data.data;

        // 更新文档
        const response = await axios.put(`${API_BASE}/documents/${id}`, {
            ...doc,
            title: newTitle
        }, getHeaders());

        if(response.data.code != 200){
            showToast(response.data.message,'error');
            return;
        }

        showToast('编辑成功', 'success');
        loadDocuments();
    } catch (error) {
        showToast(error.response?.data?.message || '编辑失败', 'error');
    }
}

// 删除文档
async function deleteDocument(id) {
    if (!currentUser.id) {
        showToast('请先选择用户', 'error');
        return;
    }

    if (!confirm('确认删除？')) return;

    try {
        // 先获取文档信息（用于权限检查）
        const getResp = await axios.get(`${API_BASE}/documents/${id}`, getHeaders());
        const doc = getResp.data.data;

        const response = await axios.delete(`${API_BASE}/documents/${id}`, {
            headers: getHeaders().headers,
            data: doc
        });

        if(response.data.code != 200){
            showToast(response.data.message,'error');
            return;
        }

        showToast('删除成功', 'success');
        loadDocuments();
    } catch (error) {
        showToast(error.response?.data?.message || '删除失败', 'error');
    }
}

// 加载策略列表
async function loadPolicies() {
    try {
        const response = await axios.get(`${API_BASE}/policies`);
        const policies = response.data.data || [];

        const tbody = document.getElementById('policyList');
        tbody.innerHTML = policies.map((policy, index) => `
            <tr>
                <td class="border p-2 font-mono text-sm">${policy[0]}</td>
                <td class="border p-2 font-mono text-sm">${policy[1]}</td>
                <td class="border p-2">
                    <span class="px-2 py-1 rounded text-sm ${getActionColor(policy[2])}">
                        ${policy[2]}
                    </span>
                </td>
                <td class="border p-2">
                    <button onclick="removePolicy('${policy[0]}', '${escHtml(policy[1])}', '${policy[2]}')"
                            class="bg-red-500 text-white px-2 py-1 rounded text-sm hover:bg-red-600">
                        删除
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('加载策略失败', error);
        showToast('加载策略失败', 'error');
    }
}

function escHtml(str) {
  return str.replace(/[&<>"']/g, m => ({
    '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
  })[m]);
}

// 添加策略
async function addPolicy() {
    const subRule = document.getElementById('subRule').value;
    const objRule = document.getElementById('objRule').value;
    const action = document.getElementById('policyAction').value;

    if (!subRule || !objRule) {
        showToast('请填写完整信息', 'error');
        return;
    }

    try {
        await axios.post(`${API_BASE}/policies`, {
            subRule, objRule, action
        });

        showToast('添加成功', 'success');
        clearPolicyForm();
        loadPolicies();
    } catch (error) {
        showToast(error.response?.data?.message || '添加失败', 'error');
    }
}

// 删除策略
async function removePolicy(subRule, objRule, action) {
    if (!confirm('确认删除此策略？')) return;

    try {
        await axios.delete(`${API_BASE}/policies`, {
            data: { subRule, objRule, action }
        });

        showToast('删除成功', 'success');
        loadPolicies();
    } catch (error) {
        showToast(error.response?.data?.message || '删除失败', 'error');
    }
}

// 获取请求头（携带用户信息）
function getHeaders() {
    return {
        headers: {
            'X-User-Id': currentUser.id,
            'X-User-Name': currentUser.name,
            'X-User-Dept': currentUser.dept
        }
    };
}

// 清空文档表单
function clearDocumentForm() {
    document.getElementById('docTitle').value = '';
    document.getElementById('docOwnerId').value = '';
    document.getElementById('docDept').value = '';
}

// 清空策略表单
function clearPolicyForm() {
    document.getElementById('subRule').value = '';
    document.getElementById('objRule').value = '';
}

// 显示提示消息
function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.classList.remove('hidden', 'bg-blue-500', 'bg-green-500', 'bg-red-500');

    if (type === 'success') toast.classList.add('bg-green-500');
    else if (type === 'error') toast.classList.add('bg-red-500');
    else toast.classList.add('bg-blue-500');

    setTimeout(() => toast.classList.add('hidden'), 3000);
}

// 获取类型颜色
function getTypeColor(type) {
    const colors = {
        'report': 'bg-blue-100 text-blue-800',
        'contract': 'bg-green-100 text-green-800',
        'public': 'bg-gray-100 text-gray-800'
    };
    return colors[type] || 'bg-gray-100 text-gray-800';
}

// 获取操作颜色
function getActionColor(action) {
    const colors = {
        'read': 'bg-blue-100 text-blue-800',
        'edit': 'bg-yellow-100 text-yellow-800',
        'delete': 'bg-red-100 text-red-800'
    };
    return colors[action] || 'bg-gray-100 text-gray-800';
}
