document.addEventListener('DOMContentLoaded', function() {
    if (!checkAuth()) return;

    loadRooms();
    setupCreateRoom();
});

// 加载直播间列表
async function loadRooms() {
    try {
        const response = await fetch(`${API_URL}/rooms`, {
            headers: getAuthHeader()
        });

        if (!response.ok) {
            throw new Error('Failed to load rooms');
        }

        const rooms = await response.json();
        displayRooms(rooms);
    } catch (error) {
        console.error('Error loading rooms:', error);
        alert('加载直播间列表失败');
    }
}

// 显示直播间列表
function displayRooms(rooms) {
    const container = document.getElementById('rooms-container');
    container.innerHTML = '';

    if (rooms.length === 0) {
        container.innerHTML = '<p class="no-rooms">当前没有直播间，创建一个吧！</p>';
        return;
    }

    rooms.forEach(room => {
        const roomElement = document.createElement('div');
        roomElement.className = 'room-card';
        roomElement.innerHTML = `
            <h3>${room.title}</h3>
            <div class="room-info">
                <p>主播: ${room.broadcaster}</p>
                <p>观众: ${room.audiences.length}人</p>
                <p>连麦: ${room.activeMicUsers.length}人</p>
            </div>
            <button class="join-room-btn" data-room-id="${room.roomId}">进入直播间</button>
        `;
        container.appendChild(roomElement);
    });

    // 添加进入直播间的点击事件
    document.querySelectorAll('.join-room-btn').forEach(button => {
        button.addEventListener('click', function() {
            const roomId = this.getAttribute('data-room-id');
            window.location.href = `room.html?id=${roomId}`;
        });
    });
}

// 设置创建直播间功能
function setupCreateRoom() {
    const createBtn = document.getElementById('create-room-btn');
    if (!createBtn) return;

    createBtn.addEventListener('click', async function() {
        const title = document.getElementById('room-title').value;

        if (!title) {
            alert('请输入直播间标题');
            return;
        }

        try {
            const response = await fetch(`${API_URL}/rooms`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...getAuthHeader()
                },
                body: JSON.stringify({ title })
            });

            if (!response.ok) {
                throw new Error('Failed to create room');
            }

            const room = await response.json();
            window.location.href = `room.html?id=${room.roomId}`;
        } catch (error) {
            console.error('Error creating room:', error);
            alert('创建直播间失败');
        }
    });
}