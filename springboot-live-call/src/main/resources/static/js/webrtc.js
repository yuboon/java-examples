class WebRTCClient {
    constructor(username, roomId, isBroadcaster) {
        this.username = username;
        this.roomId = roomId;
        this.isBroadcaster = isBroadcaster;
        this.localStream = null;
        this.peerConnections = {};
        this.stompClient = null;
        this.mediaConstraints = {
            audio: true,
            video: true
        };

        // ICE服务器配置
        this.iceServers = {
            iceServers: [
                { urls: 'stun:stun.l.google.com:19302' },
                { urls: 'stun:stun1.l.google.com:19302' }
            ]
        };
    }

    // 初始化WebSocket连接
    async connectWebSocket() {
        return new Promise((resolve, reject) => {
            const socket = new SockJS('/ws');

            // 增加STOMP客户端调试
            const client = Stomp.over(socket);
            client.debug = function(str) {
                console.log('STOMP Debug: ' + str);
            };
            this.stompClient = client;

            const headers = {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            };

            this.stompClient.connect(headers, frame => {
                console.log('Connected to WebSocket:', frame);

                // 订阅信令消息
                this.stompClient.subscribe(`/user/queue/signal`, message => {
                    console.log('Received signal message:', message.body);
                    const signalData = JSON.parse(message.body);
                    this.handleSignalingData(signalData);
                });

                // 订阅连麦响应
                this.stompClient.subscribe(`/user/queue/mic-response`, message => {
                    console.log('Received mic response:', message.body);
                    try {
                        const response = JSON.parse(message.body);
                        this.handleMicResponse(response);
                    } catch (e) {
                        console.log('Non-JSON mic response:', message.body);
                    }
                });

                // 订阅连麦结束通知
                this.stompClient.subscribe(`/user/queue/mic-ended`, message => {
                    console.log('Received mic ended notification:', message.body);
                    const data = message.body;
                    this.handleMicEnded(data);
                });

                // 如果是主播，订阅连麦请求
                if (this.isBroadcaster) {
                    console.log('Broadcaster subscribing to mic-requests');

                    // 方法1: 标准订阅
                    this.stompClient.subscribe(`/user/queue/mic-requests`, message => {
                        console.log('Received mic request (standard):', message.body);
                        try {
                            const request = JSON.parse(message.body);
                            this.handleMicRequest(request);
                        } catch (e) {
                            console.error('Error parsing mic request:', e);
                        }
                    });

                    // 方法2: 尝试直接订阅完整路径
                    const username = localStorage.getItem('username');
                    this.stompClient.subscribe(`/user/${username}/queue/mic-requests`, message => {
                        console.log('Received mic request (direct):', message.body);
                        try {
                            const request = JSON.parse(message.body);
                            this.handleMicRequest(request);
                        } catch (e) {
                            console.error('Error parsing mic request:', e);
                        }
                    });
                }

                resolve();
            }, error => {
                console.error('WebSocket connection error:', error);
                reject(error);
            });
        });
    }

    // 初始化本地媒体流
    async initLocalStream() {
        try {
            this.localStream = await navigator.mediaDevices.getUserMedia(this.mediaConstraints);
            return this.localStream;
        } catch (error) {
            console.error('Error accessing media devices:', error);
            throw error;
        }
    }

    // 创建一个新的RTCPeerConnection
    createPeerConnection(remoteUsername) {
        const pc = new RTCPeerConnection(this.iceServers);

        // 添加本地流到连接
        this.localStream.getTracks().forEach(track => {
            pc.addTrack(track, this.localStream);
        });

        // 监听ICE候选
        pc.onicecandidate = event => {
            if (event.candidate) {
                this.sendSignalingData(remoteUsername, {
                    type: 'ice-candidate',
                    candidate: event.candidate
                });
            }
        };

        // 监听远程流
        pc.ontrack = event => {
            const remoteStream = event.streams[0];
            this.onRemoteStreamAdded(remoteUsername, remoteStream);
        };

        this.peerConnections[remoteUsername] = pc;
        return pc;
    }

    // 发起连接(创建offer)
    async createOffer(remoteUsername) {
        try {
            const pc = this.createPeerConnection(remoteUsername);
            const offer = await pc.createOffer();
            await pc.setLocalDescription(offer);

            this.sendSignalingData(remoteUsername, {
                type: 'offer',
                sdp: pc.localDescription
            });
        } catch (error) {
            console.error('Error creating offer:', error);
        }
    }

    // 响应offer(创建answer)
    async handleOffer(offer, remoteUsername) {
        try {
            const pc = this.createPeerConnection(remoteUsername);
            await pc.setRemoteDescription(new RTCSessionDescription(offer));

            const answer = await pc.createAnswer();
            await pc.setLocalDescription(answer);

            this.sendSignalingData(remoteUsername, {
                type: 'answer',
                sdp: pc.localDescription
            });
        } catch (error) {
            console.error('Error handling offer:', error);
        }
    }

    // 处理answer
    async handleAnswer(answer, remoteUsername) {
        try {
            const pc = this.peerConnections[remoteUsername];
            if (pc) {
                await pc.setRemoteDescription(new RTCSessionDescription(answer));
            }
        } catch (error) {
            console.error('Error handling answer:', error);
        }
    }

    // 处理ICE候选
    async handleIceCandidate(candidate, remoteUsername) {
        try {
            const pc = this.peerConnections[remoteUsername];
            if (pc) {
                await pc.addIceCandidate(new RTCIceCandidate(candidate));
            }
        } catch (error) {
            console.error('Error handling ICE candidate:', error);
        }
    }

    // 发送信令数据
    sendSignalingData(receiver, data) {
        if (!this.stompClient) {
            console.error('WebSocket not connected');
            return;
        }

        this.stompClient.send(`/app/signal/${this.roomId}`, {}, JSON.stringify({
            receiver: receiver,
            content: JSON.stringify(data)
        }));
    }

    // 处理收到的信令数据
    handleSignalingData(signalData) {
        const sender = signalData.sender;
        const content = JSON.parse(signalData.content);

        switch (content.type) {
            case 'offer':
                this.handleOffer(content.sdp, sender);
                break;
            case 'answer':
                this.handleAnswer(content.sdp, sender);
                break;
            case 'ice-candidate':
                this.handleIceCandidate(content.candidate, sender);
                break;
            default:
                console.warn('Unknown signal type:', content.type);
        }
    }

    // 申请连麦
    requestMic() {
        if (!this.stompClient || !this.stompClient.connected) {
            console.error('WebSocket not connected');
            alert('WebSocket连接未建立，无法申请连麦');
            return false;
        }

        console.log(`Sending mic request for room ${this.roomId} by user ${this.username}`);

        try {
            // 添加更多错误处理
            this.stompClient.send(`/app/mic.request/${this.roomId}`, {}, JSON.stringify({
                // 添加更多信息以便调试
                timestamp: new Date().getTime(),
                username: this.username
            }));
            console.log('Mic request sent successfully');
            return true;
        } catch (error) {
            console.error('Error sending mic request:', error);
            alert('发送连麦请求失败: ' + error.message);
            return false;
        }
    }

    // 处理连麦响应
    async handleMicResponse(response) {
        console.log('处理连麦响应:', response);

        // 兼容字符串和对象两种响应格式
        let status;
        if (typeof response === 'string') {
            console.log('收到字符串格式的响应:', response);
            // 尝试解析字符串响应
            if (response.includes('ACCEPTED')) {
                status = 'ACCEPTED';
            } else if (response.includes('REJECTED')) {
                status = 'REJECTED';
            } else {
                console.warn('无法识别的响应内容:', response);
                return;
            }
        } else {
            // 对象格式响应
            status = response.status;
        }

        console.log('连麦响应状态:', status);

        if (status === 'ACCEPTED') {
            try {
                console.log('连麦请求已接受，初始化媒体流');
                // 初始化本地媒体流(如果还没有)
                if (!this.localStream) {
                    await this.initLocalStream();
                    this.onLocalStreamCreated(this.localStream);
                }

                // 等待主播发起WebRTC连接
                console.log('等待主播发起WebRTC连接');
                this.onMicRequestAccepted();
            } catch (error) {
                console.error('初始化媒体流失败:', error);
                alert('连麦初始化失败: ' + error.message);
            }
        } else if (status === 'REJECTED') {
            console.log('连麦请求被拒绝');
            this.onMicRequestRejected();
        } else {
            console.warn('未知的连麦响应状态:', status);
        }
    }

    // 添加新的回调方法
    onMicRequestAccepted() {
        console.log('连麦请求已接受，等待连接建立');
        // 这个方法会由使用者实现
    }

    // 处理连麦请求(主播)
    async handleMicRequest(request) {
        // 通知UI显示请求
        this.onMicRequestReceived(request);
    }

    // 响应连麦请求(主播)
    async respondToMicRequest(username, accept) {
        if (!this.stompClient) {
            console.error('WebSocket not connected');
            return;
        }

        this.stompClient.send(`/app/mic.response/${this.roomId}`, {}, JSON.stringify({
            username: username,
            accept: accept
        }));

        if (accept) {
            // 初始化本地媒体流(如果还没有)
            if (!this.localStream) {
                await this.initLocalStream();
                this.onLocalStreamCreated(this.localStream);
            }

            // 主播发起WebRTC连接
            await this.createOffer(username);
        }
    }

    // 结束连麦
    endMic(username) {
        if (!this.stompClient) {
            console.error('WebSocket not connected');
            return;
        }

        this.stompClient.send(`/app/mic.end/${this.roomId}`, {}, username);
        this.closePeerConnection(username);
    }

    // 处理连麦结束
    handleMicEnded(data) {
        if (this.isBroadcaster) {
            // 主播收到用户结束连麦的通知
            this.closePeerConnection(data);
            this.onMicEnded(data);
        } else {
            // 用户收到主播结束连麦的通知
            this.closePeerConnection(this.roomId);
            this.onMicEnded();
        }
    }

    // 关闭对等连接
    closePeerConnection(username) {
        const pc = this.peerConnections[username];
        if (pc) {
            pc.close();
            delete this.peerConnections[username];
        }
    }

    // 关闭所有连接
    close() {
        // 关闭所有对等连接
        Object.keys(this.peerConnections).forEach(username => {
            this.closePeerConnection(username);
        });

        // 关闭本地媒体流
        if (this.localStream) {
            this.localStream.getTracks().forEach(track => track.stop());
        }

        // 断开WebSocket连接
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.disconnect();
        }
    }

    // 以下方法需要在使用时实现
    onLocalStreamCreated(stream) {
        // 显示本地视频流
        console.log('Local stream created, implement this method');
    }

    onRemoteStreamAdded(username, stream) {
        // 显示远程视频流
        console.log('Remote stream added for', username, 'implement this method');
    }

    onMicRequestReceived(request) {
        // 显示连麦请求
        console.log('Mic request received from', request.username, 'implement this method');
    }

    onMicRequestRejected() {
        // 连麦请求被拒绝
        console.log('Mic request rejected, implement this method');
    }

    onMicEnded(username) {
        // 连麦结束
        console.log('Mic ended for', username, 'implement this method');
    }
}