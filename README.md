### network-server
This is the network server for the project.Based on netty framework.The target is let easy to build a network server.

### NOTE
调试资源泄露，启动时添加：-Xms12m -Xmx12m -Dio.netty.leakDetection.level=paranoid

### TODO LIST
1. 内网不同协议的转发
2. 外网消息转发到内网处理器中
