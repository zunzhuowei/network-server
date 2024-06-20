### network-server
This is the network server for the project.Based on netty framework.The target is let easy to build a network server.

### NOTE
调试资源泄露，启动时添加：-Xms12m -Xmx12m -Dio.netty.leakDetection.level=paranoid

### TODO LIST
~~1. 内网不同协议的转发~~
~~2. 外网消息转发到内网处理器中~~
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
~~17. 为避免长时间占用链接，没有登录的链接，服务端添加心跳检测机制，如果超过一定时间没有收到心跳，则主动断开链接~~
18. ~~外网支持协议配置化~~
19. ~~内网消息重组~~，~~支持延迟消息~~，~~可靠消息（保证送达与幂等性）~~
20. 分布式事务？
21. ~~内网服务器登录，将已登录的session同步给登录服务器~~
22. ~~内网服务登出，同步消息给登录服务器~~