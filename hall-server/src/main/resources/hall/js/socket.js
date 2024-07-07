var socketBuilder;
var socket = {};
let heartbeatTimer;
let heartbeatTimeout;
let reconnectTimer;
// 定义心跳间隔时间，单位毫秒
const HEARTBEAT_INTERVAL = 2500;
// 定义心跳检测超时时间，单位毫秒
const HEARTBEAT_TIMEOUT = 5000;
(function () {
    socketBuilder = {
        littleEndian: false,
        socket: {},
        build(websocket, ssl) {
            var socket;
            if (typeof (WebSocket) === undefined) {
                console.log("您的浏览器不支持WebSocket");
            } else {
                //var domain = window.location.host;
                var wsUrl = ssl ? "wss://" + websocket : "ws://" + websocket;
                console.log("connect websocket --::" + wsUrl);
                socket = new WebSocket(wsUrl);
                socket.binaryType = 'arraybuffer';
                this.socket = socket;
            }
            return this.socket;
        },
        connect(websocket, ssl) {
            socket = socketBuilder.build(websocket, ssl);
            //打开事件
            socket.onopen = function (evt) {
                receiver.onOpen(evt);
                heartbeatTimer = setInterval(sendHeartbeat, HEARTBEAT_INTERVAL);
            };
            //获得消息事件
            socket.onmessage = function (evt) {
                if (heartbeatTimeout) {
                    clearTimeout(heartbeatTimeout);
                    heartbeatTimeout = null;
                }
                receiver.onMsg(evt.data);
                //发现消息进入 开始处理前端触发逻辑
            };

            //关闭事件
            socket.onclose = function (evt) {
                clearHeartbeatTimers();
                receiver.onClose(evt);
            };

            //发生了错误事件
            socket.onerror = function (evt) {
                clearHeartbeatTimers();
                receiver.onError(evt);
                //此时可以尝试刷新页面
            };



            function reconnect() {
                if (reconnectTimer) {
                    clearTimeout(reconnectTimer);
                }
                console.log('Attempting to reconnect...');
                reconnectTimer = setTimeout(() => {
                    this.connect(websocket, ssl);
                }, 3000); // 尝试重连的时间间隔
            }

            // 发送心跳包
            function sendHeartbeat() {
                // 登录成功发送心跳包
                var buffer = sender.buildHead(99).finish();
                socket.send(buffer);
            }
            // 清除心跳相关的定时器
            function clearHeartbeatTimers() {
                if (heartbeatTimer) {
                    clearInterval(heartbeatTimer);
                }
                if (heartbeatTimeout) {
                    clearTimeout(heartbeatTimeout);
                }
            }

            // 设置心跳超时，如果在规定时间内没有收到服务端的心跳响应，则认为连接已断开
            heartbeatTimer = setInterval(function () {
                heartbeatTimeout = setTimeout(function () {
                    console.error('Heartbeat timeout, connection is considered lost');
                    //socket.close();
                }, HEARTBEAT_TIMEOUT);
            }, HEARTBEAT_INTERVAL);
        }
    }
})();


