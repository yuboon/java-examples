// danmaku.js
document.addEventListener('DOMContentLoaded', function() {
    // 获取DOM元素
    const videoPlayer = document.getElementById('video-player');
    const danmakuContainer = document.getElementById('danmaku-container');
    const danmakuInput = document.getElementById('danmaku-input');
    const colorPicker = document.getElementById('color-picker');
    const fontSizeSelect = document.getElementById('font-size');
    const sendBtn = document.getElementById('send-btn');

    // 视频ID（实际应用中可能从URL或其他地方获取）
    const videoId = 'video123';

    // 用户信息（实际应用中可能从登录系统获取）
    const userId = 'user' + Math.floor(Math.random() * 1000);
    const username = '用户' + userId.substring(4);

    // WebSocket连接
    let stompClient = null;

    // 连接WebSocket
    function connect() {
        const socket = new SockJS('/ws-danmaku');
        stompClient = Stomp.over(socket);

        stompClient.connect({}, function(frame) {
            console.log('Connected to WebSocket: ' + frame);

            // 订阅当前视频的弹幕频道
            stompClient.subscribe('/topic/video/' + videoId, function(response) {
                const danmaku = JSON.parse(response.body);
                showDanmaku(danmaku);
            });

            // 获取历史弹幕
            loadHistoryDanmaku();
        }, function(error) {
            console.error('WebSocket连接失败: ', error);
            // 尝试重新连接
            setTimeout(connect, 5000);
        });
    }

    // 加载历史弹幕
    function loadHistoryDanmaku() {
        fetch(`/api/danmaku/video/${videoId}`)
            .then(response => response.json())
            .then(danmakus => {
                // 记录历史弹幕，用于播放到相应时间点时显示
                window.historyDanmakus = danmakus;
                console.log(`已加载${danmakus.length}条历史弹幕`);
            })
            .catch(error => console.error('获取历史弹幕失败:', error));
    }

    // 发送弹幕
    function sendDanmaku() {
        const content = danmakuInput.value.trim();
        if (!content) return;

        const danmaku = {
            content: content,
            color: colorPicker.value,
            fontSize: parseInt(fontSizeSelect.value),
            time: videoPlayer.currentTime,
            videoId: videoId,
            userId: userId,
            username: username
        };

        stompClient.send('/app/danmaku/send', {}, JSON.stringify(danmaku));

        // 清空输入框
        danmakuInput.value = '';
    }

    // 显示弹幕
    function showDanmaku(danmaku) {
        // 创建弹幕元素
        const danmakuElement = document.createElement('div');
        danmakuElement.className = 'danmaku';
        danmakuElement.textContent = danmaku.content;
        danmakuElement.style.color = danmaku.color;
        danmakuElement.style.fontSize = danmaku.fontSize + 'px';

        // 随机分配轨道（垂直位置）
        const trackHeight = danmaku.fontSize + 5; // 轨道高度
        const maxTrack = Math.floor(danmakuContainer.clientHeight / trackHeight);
        const trackNumber = Math.floor(Math.random() * maxTrack);
        danmakuElement.style.top = (trackNumber * trackHeight) + 'px';

        // 计算动画持续时间（基于容器宽度）
        const duration = 8 + Math.random() * 4; // 8-12秒
        danmakuElement.style.animationDuration = duration + 's';

        // 添加到容器
        danmakuContainer.appendChild(danmakuElement);

        // 动画结束后移除元素
        setTimeout(() => {
            danmakuContainer.removeChild(danmakuElement);
        }, duration * 1000);
    }

    // 视频时间更新时，显示对应时间点的历史弹幕
    videoPlayer.addEventListener('timeupdate', function() {
        const currentTime = videoPlayer.currentTime;

        // 如果历史弹幕已加载
        if (window.historyDanmakus && window.lastCheckedTime !== Math.floor(currentTime)) {
            window.lastCheckedTime = Math.floor(currentTime);

            // 检查是否有需要在当前时间点显示的弹幕
            window.historyDanmakus.forEach(danmaku => {
                // 如果弹幕时间点在当前时间的±0.5秒内且尚未显示
                if (Math.abs(danmaku.time - currentTime) <= 0.5 &&
                    (!window.displayedDanmakus || !window.displayedDanmakus.includes(danmaku.id))) {

                    // 记录已显示的弹幕ID
                    if (!window.displayedDanmakus) {
                        window.displayedDanmakus = [];
                    }
                    window.displayedDanmakus.push(danmaku.id);

                    // 显示弹幕
                    showDanmaku(danmaku);
                }
            });
        }
    });

    // 视频跳转时重置已显示弹幕记录
    videoPlayer.addEventListener('seeking', function() {
        window.displayedDanmakus = [];
    });

    // 绑定发送按钮点击事件
    sendBtn.addEventListener('click', sendDanmaku);

    // 绑定输入框回车事件
    danmakuInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            sendDanmaku();
        }
    });

    // 连接WebSocket
    connect();
});