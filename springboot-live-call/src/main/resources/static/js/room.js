document.addEventListener('DOMContentLoaded', function() {
    if (!checkAuth()) return;

    const urlParams = new URLSearchParams(window.location.search);
    const roomId = urlParams.get('id');

    if (!roomId) {
        alert('直播间ID不能为空');
        window.location.href = 'index.html';
        return;
    }

    const currentUser = getCurrentUser();
    const isBroadcaster = currentUser.role === 'BROADCASTER';

    // 初始化WebRTC客户端
    const webrtcClient = new WebRTCClient(currentUser.username, roomId, isBroadcaster);

    // 明确设置为全局变量
    window.webrtcClient = webrtcClient;

    // 设置事件处理函数
    setupEventHandlers(webrtcClient);

    // 加载直播间信息
    loadRoomInfo(roomId);

    // 连接WebSocket
    initializeWebSocketAndMedia(webrtcClient, isBroadcaster);
});


// 设置事件处理函数
function setupEventHandlers(webrtcClient) {
    // 返回大厅按钮
    document.getElementById('back-to-lobby').addEventListener('click', function() {
        webrtcClient.close();
        window.location.href = 'index.html';
    });

    // 发送消息按钮
    document.getElementById('send-message-btn').addEventListener('click', sendChatMessage);

    // 聊天输入框回车发送
    document.getElementById('chat-input').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            sendChatMessage();
        }
    });

    // 申请连麦按钮(观众)
    const requestMicBtn = document.getElementById('request-mic-btn');
    if (requestMicBtn) {
        requestMicBtn.addEventListener('click', function() {
            console.log('申请连麦按钮被点击');

            try {
                const success = webrtcClient.requestMic();

                if (success) {
                    this.disabled = true;
                    this.textContent = '已申请连麦，等待主播接受...';
                    document.getElementById('mic-status').textContent = '连麦申请中...';
                    console.log('连麦申请已发送');
                } else {
                    console.error('连麦申请发送失败');
                }
            } catch (error) {
                console.error('申请连麦时发生错误:', error);
                alert('申请连麦失败: ' + error.message);
            }
        });
    }

    // 重写WebRTC回调函数
    webrtcClient.onLocalStreamCreated = function(stream) {
        const videoElement = document.getElementById('local-video') ||
                            document.getElementById('broadcaster-video');
        videoElement.srcObject = stream;
    };

    webrtcClient.onRemoteStreamAdded = function(username, stream) {
        let videoElement;

        if (webrtcClient.isBroadcaster) {
            // 主播看到观众视频
            const audienceContainer = document.getElementById('audience-videos-container');
            const audienceVideoDiv = document.createElement('div');
            audienceVideoDiv.className = 'audience-video-container';
            audienceVideoDiv.id = `audience-${username}`;

            audienceVideoDiv.innerHTML = `
                <video class="audience-video" id="video-${username}" autoplay playsinline></video>
                <div class="video-label">${username}</div>
                <button class="end-mic-btn" data-username="${username}">结束连麦</button>
            `;

            audienceContainer.appendChild(audienceVideoDiv);
            videoElement = document.getElementById(`video-${username}`);

            // 添加结束连麦按钮事件
            audienceVideoDiv.querySelector('.end-mic-btn').addEventListener('click', function() {
                const username = this.getAttribute('data-username');
                webrtcClient.endMic(username);
            });
        } else {
            // 观众看到主播视频
            videoElement = document.getElementById('broadcaster-video');
        }

        if (videoElement) {
            videoElement.srcObject = stream;
        }
    };

    webrtcClient.onMicRequestReceived = function(request) {
        console.log('处理连麦请求:', request);

        const requestsList = document.getElementById('mic-requests-list');
        if (!requestsList) {
            console.error('找不到连麦请求列表元素');
            return;
        }

        // 检查是否已存在相同的请求
        const existingRequest = document.getElementById(`request-${request.username}`);
        if (existingRequest) {
            console.log('已存在相同用户的连麦请求，不重复添加');
            return;
        }

        const requestItem = document.createElement('li');
        requestItem.className = 'mic-request-item';
        requestItem.id = `request-${request.username}`;
        requestItem.innerHTML = `
            <span>${request.username} 申请连麦</span>
            <div class="request-actions">
                <button class="accept-btn" data-username="${request.username}">接受</button>
                <button class="reject-btn" data-username="${request.username}">拒绝</button>
            </div>
        `;

        requestsList.appendChild(requestItem);
        console.log('连麦请求UI已添加');

        // 添加按钮事件
        const acceptBtn = requestItem.querySelector('.accept-btn');
        if (acceptBtn) {
            acceptBtn.addEventListener('click', function() {
                const username = this.getAttribute('data-username');
                console.log(`接受用户 ${username} 的连麦请求`);

                // 同时使用WebSocket和HTTP API发送响应
                webrtcClient.respondToMicRequest(username, true);

                // 备用HTTP请求
                const urlParams = new URLSearchParams(window.location.search);
                const roomId = urlParams.get('id');
                fetch(`${API_URL}/rooms/${roomId}/mic-response?username=${username}&accept=true`, {
                    method: 'POST',
                    headers: getAuthHeader()
                })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('HTTP API连麦响应失败');
                    }
                    console.log('通过HTTP API发送连麦接受成功');
                })
                .catch(error => {
                    console.error('发送连麦响应失败:', error);
                });

                requestItem.remove();

                // 添加到活跃连麦列表
                const activeList = document.getElementById('active-mics-list');
                if (activeList) {
                    const activeItem = document.createElement('li');
                    activeItem.className = 'active-mic-item';
                    activeItem.id = `active-${username}`;
                    activeItem.innerHTML = `
                        <span>${username}</span>
                        <button class="end-mic-btn" data-username="${username}">结束连麦</button>
                    `;
                    activeList.appendChild(activeItem);

                    // 添加结束连麦按钮事件
                    const endMicBtn = activeItem.querySelector('.end-mic-btn');
                    if (endMicBtn) {
                        endMicBtn.addEventListener('click', function() {
                            const username = this.getAttribute('data-username');
                            console.log(`结束与用户 ${username} 的连麦`);
                            webrtcClient.endMic(username);
                        });
                    }
                }
            });
        }

        const rejectBtn = requestItem.querySelector('.reject-btn');
        if (rejectBtn) {
            rejectBtn.addEventListener('click', function() {
                const username = this.getAttribute('data-username');
                console.log(`拒绝用户 ${username} 的连麦请求`);

                // 同时使用WebSocket和HTTP API发送响应
                webrtcClient.respondToMicRequest(username, false);

                // 备用HTTP请求
                const urlParams = new URLSearchParams(window.location.search);
                const roomId = urlParams.get('id');
                fetch(`${API_URL}/rooms/${roomId}/mic-response?username=${username}&accept=false`, {
                    method: 'POST',
                    headers: getAuthHeader()
                })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('HTTP API连麦响应失败');
                    }
                    console.log('通过HTTP API发送连麦拒绝成功');
                })
                .catch(error => {
                    console.error('发送连麦响应失败:', error);
                });

                requestItem.remove();
            });
        }
    };

    webrtcClient.onMicEnded = function(username) {
        if (webrtcClient.isBroadcaster && username) {
            // 主播结束观众连麦
            const audienceVideo = document.getElementById(`audience-${username}`);
            if (audienceVideo) {
                audienceVideo.remove();
            }

            const activeMicItem = document.getElementById(`active-${username}`);
            if (activeMicItem) {
                activeMicItem.remove();
            }
        } else {
            // 观众的连麦被结束
            const requestMicBtn = document.getElementById('request-mic-btn');
            if (requestMicBtn) {
                requestMicBtn.disabled = false;
                requestMicBtn.textContent = '申请连麦';
            }
            document.getElementById('mic-status').textContent = '连麦已结束';

            // 停止本地视频
            const localVideo = document.getElementById('local-video');
            if (localVideo && localVideo.srcObject) {
                localVideo.srcObject.getTracks().forEach(track => track.stop());
                localVideo.srcObject = null;
            }
        }
    };

    webrtcClient.onMicRequestAccepted = function() {
        const requestMicBtn = document.getElementById('request-mic-btn');
        if (requestMicBtn) {
            requestMicBtn.disabled = true;
            requestMicBtn.textContent = '连麦已接受，正在建立连接...';
        }
        document.getElementById('mic-status').textContent = '连麦已接受，等待连接建立...';

        // 可以添加一个本地视频预览
        const videoSection = document.querySelector('.video-section');
        if (!document.getElementById('local-video-container')) {
            const localVideoContainer = document.createElement('div');
            localVideoContainer.className = 'local-video-container';
            localVideoContainer.id = 'local-video-container';
            localVideoContainer.innerHTML = `
                <video id="local-video" autoplay playsinline muted></video>
                <div class="video-label">我(观众)</div>
                <button id="end-local-mic-btn">结束连麦</button>
            `;
            videoSection.appendChild(localVideoContainer);

            // 设置本地视频流
            if (webrtcClient.localStream) {
                document.getElementById('local-video').srcObject = webrtcClient.localStream;
            }

            // 添加结束连麦按钮事件
            document.getElementById('end-local-mic-btn').addEventListener('click', function() {
                webrtcClient.endMic(getCurrentUser().username);
            });
        }
    };

    webrtcClient.onMicRequestRejected = function() {
        const requestMicBtn = document.getElementById('request-mic-btn');
        if (requestMicBtn) {
            requestMicBtn.disabled = false;
            requestMicBtn.textContent = '申请连麦';
        }
        document.getElementById('mic-status').textContent = '连麦申请被拒绝';

        // 提醒用户
        alert('主播拒绝了您的连麦申请');
    };

}

// 加载直播间信息
async function loadRoomInfo(roomId) {
    try {
        const response = await fetch(`${API_URL}/rooms/${roomId}`, {
            headers: getAuthHeader()
        });

        if (!response.ok) {
            throw new Error('Failed to load room info');
        }

        const room = await response.json();

        // 更新页面信息
        document.getElementById('room-title').textContent = room.title;
        document.getElementById('broadcaster-name').textContent = room.broadcaster;

        // 显示主播控制面板或观众控制面板
        const currentUser = getCurrentUser();
        const isBroadcaster = currentUser.username === room.broadcaster;

        if (isBroadcaster) {
            document.getElementById('broadcaster-controls').style.display = 'block';
            document.getElementById('audience-controls').style.display = 'none';
            document.getElementById('broadcaster-label').textContent = '我(主播)';
        } else {
            document.getElementById('broadcaster-controls').style.display = 'none';
            document.getElementById('audience-controls').style.display = 'block';
            document.getElementById('broadcaster-label').textContent = room.broadcaster;
        }

    } catch (error) {
        console.error('Error loading room info:', error);
        alert('加载直播间信息失败');
        window.location.href = 'index.html';
    }
}

// 初始化WebSocket和媒体流
async function initializeWebSocketAndMedia(webrtcClient, isBroadcaster) {
    try {
        // 连接WebSocket
        await webrtcClient.connectWebSocket();

        // 订阅直播间消息
        subscribeToRoomMessages(webrtcClient.stompClient, webrtcClient.roomId);

        // 如果是主播，初始化本地媒体流
        if (isBroadcaster) {
            const localStream = await webrtcClient.initLocalStream();
            webrtcClient.onLocalStreamCreated(localStream);
        }

    } catch (error) {
        console.error('Error initializing:', error);
        alert('初始化连接失败');
    }
}

// 订阅直播间消息
function subscribeToRoomMessages(stompClient, roomId) {
    stompClient.subscribe(`/topic/room/${roomId}`, function(message) {
        const chatMessage = JSON.parse(message.body);
        displayChatMessage(chatMessage);
    });
}

// 发送聊天消息
function sendChatMessage() {
    const inputElement = document.getElementById('chat-input');
    const content = inputElement.value.trim();

    if (!content) return;

    const urlParams = new URLSearchParams(window.location.search);
    const roomId = urlParams.get('id');

    // 添加检查确保webrtcClient存在
    if (window.webrtcClient && window.webrtcClient.stompClient && window.webrtcClient.stompClient.connected) {
        window.webrtcClient.stompClient.send(`/app/chat.send/${roomId}`, {}, JSON.stringify({
            content: content,
            type: 'CHAT'
        }));

        inputElement.value = '';
    } else {
        console.error('WebSocket连接未建立');
        alert('消息发送失败，WebSocket连接未建立');
    }
}

// 显示聊天消息
function displayChatMessage(message) {
    const messagesContainer = document.getElementById('chat-messages');
    const messageElement = document.createElement('div');
    messageElement.className = 'chat-message';

    switch (message.type) {
        case 'CHAT':
            messageElement.innerHTML = `
                <span class="message-sender">${message.sender}:</span>
                <span class="message-content">${message.content}</span>
                <span class="message-time">${formatTime(message.timestamp)}</span>
            `;
            break;
        case 'JOIN':
        case 'LEAVE':
            messageElement.className += ' system-message';
            messageElement.innerHTML = `
                <span class="message-content">${message.content}</span>
                <span class="message-time">${formatTime(message.timestamp)}</span>
            `;
            break;
    }

    messagesContainer.appendChild(messageElement);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

function setupMicResponsePolling(roomId, username) {
    // 只有观众且有正在等待的连麦请求时才需要轮询
    const isBroadcaster = getCurrentUser().role === 'BROADCASTER';
    if (isBroadcaster) {
        return;
    }

    let isPolling = false;
    const requestMicBtn = document.getElementById('request-mic-btn');

    // 监听按钮状态变化
    const observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            if (mutation.type === 'attributes' && mutation.attributeName === 'disabled') {
                // 按钮被禁用，表示已发送连麦请求
                if (requestMicBtn.disabled && !isPolling) {
                    startPolling();
                } else if (!requestMicBtn.disabled && isPolling) {
                    stopPolling();
                }
            }
        });
    });

    observer.observe(requestMicBtn, { attributes: true });

    let pollingInterval;

    function startPolling() {
        console.log('开始轮询连麦响应');
        isPolling = true;

        pollingInterval = setInterval(async function() {
            try {
                const response = await fetch(`${API_URL}/rooms/${roomId}/mic-status?username=${username}`, {
                    headers: getAuthHeader()
                });

                if (!response.ok) {
                    throw new Error('获取连麦状态失败');
                }

                const data = await response.json();
                console.log('轮询到的连麦状态:', data);

                if (data.status === 'ACCEPTED') {
                    console.log('连麦请求已被接受');
                    // 手动触发处理
                    webrtcClient.handleMicResponse({ status: 'ACCEPTED' });
                    stopPolling();
                } else if (data.status === 'REJECTED') {
                    console.log('连麦请求已被拒绝');
                    // 手动触发处理
                    webrtcClient.handleMicResponse({ status: 'REJECTED' });
                    stopPolling();
                }
                // PENDING状态继续轮询

            } catch (error) {
                console.error('轮询连麦状态失败:', error);
            }
        }, 2000); // 每2秒轮询一次
    }

    function stopPolling() {
        if (pollingInterval) {
            clearInterval(pollingInterval);
            isPolling = false;
            console.log('停止轮询连麦响应');
        }
    }

    // 页面卸载时清除轮询
    window.addEventListener('beforeunload', stopPolling);
}

// 格式化时间
function formatTime(timestamp) {
    if (!timestamp) return '';

    const date = new Date(timestamp);
    return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
}