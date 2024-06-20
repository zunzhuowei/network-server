<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">Network-Server</h1>
<h4 align="center">Simplifying the construction of network cluster services</h4>

### Introduction
> Based on Netty network framework and Springboot framework.
> 
> The target is let easy to build a network cluster server. Minimize the use of third-party dependency libraries as much as possible.
### Architecture diagram
![images](docs/pngs/Cluster.png)
![images](docs/pngs/Nodes.png)
### Existing features
1. Supports multiple network protocols, including TCP, UDP, HTTP, and WEBSOCKET; Multiple protocols can be configured on the same port.

[comment]: <> (1. 多网络协议支持，支持TCP、UDP、HTTP、WEBSOCKET协议;同端口支持多种协议（可配置）)
2. Supports multi-node clusters and dynamic node joining and exiting

[comment]: <> (2. 支持多节点集群，支持节点动态加入、退出)
3. Supports message forwarding between internal and external networks

### NOTE
调试资源泄露，启动时添加：-Xms12m -Xmx12m -Dio.netty.leakDetection.level=paranoid

### TODO LIST
1. ~~内网不同协议的转发~~
2. ~~外网消息转发到内网处理器中~~
3. ~~外部用户登录~~
4. ~~用户消息内网转发~~
5. ~~用户消息外网转发~~
6. ~~群组消息内网转发~~
7. ~~群组消息外网转发~~
8. ~~serverType写在framework中需要解决~~
9. ~~服务端、客户端消息转发待测试~~
10. ~~延迟线程池装配，任务执行完之后，处理逻辑~~
11. ~~http协议抽离~~
12. 完善readme
13. ~~mysql~~、~~mybatis~~、~~redis~~、mq等配置
14. ~~本地缓存~~~~，缓存失效时间，缓存失效时，重新查询数据库~~
15. 请求限流
16. ~~内网消息队列和失败重发机制~~，~~添加权重转发机制~~
17. ~~为避免长时间占用链接，没有登录的链接，服务端添加心跳检测机制，如果超过一定时间没有收到心跳，则主动断开链接~~
18. ~~外网支持协议配置化~~
19. ~~内网消息重组~~，~~支持延迟消息~~，~~可靠消息（保证送达与幂等性）~~
20. 分布式事务？
21. ~~内网服务器登录，将已登录的session同步给登录服务器~~
22. ~~内网服务登出，同步消息给登录服务器~~