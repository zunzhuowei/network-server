<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">Network-Server</h1>
<h4 align="center">Simplifying the construction of network cluster services</h4>

### Introduction
> Based on Netty network framework and Springboot framework.
> 
> The target is let easy to build a network cluster server. Minimize the use of third-party dependency libraries as much as possible.
### Architecture diagram
![images](docs/pngs/Cluster.png)
---
![images](docs/pngs/Nodes.png)
### Existing features
1. Supports multiple network protocols, including TCP, UDP, HTTP, and WEBSOCKET; Multiple protocols can be configured on the same port.

[comment]: <> (1. 多网络协议支持，支持TCP、UDP、HTTP、WEBSOCKET协议;同端口支持多种协议（可配置）)
2. Supports multi-node clusters and dynamic node joining and exiting

[comment]: <> (2. 支持多节点集群，支持节点动态加入、退出)
3. Supports message forwarding between internal and external networks

### Modules introduction
1. `server-framework`: The core module of the framework, 
   which provides the basic configuration of the framework, 
   and the configuration of the network protocol,
   the message forwarding, and the message processing.
   

2. `server-database`: The database module of the framework, 
   use `Mybatis` and `MySQL` to implement the database operation.
   If you need to use a MySQL database, you can use it.`that is optional`.
   

3. `server-cache`: The cache module of the framework, 
   use `Redisson` to implement the `Redis` cache operation,
   and use `Caffeine` to implement the local cache operation.
   If you need to use a caching module, you can use it.`that is optional`.

4. `gateway-server`、`hall-server`、`room-server`: These are three sample nodes of the cluster, and you can refer to their configurations to get your own cluster

### How to use
1. You must had installed JDK 1.8+ and Maven 3.x
2. Switch to the project directory and run `mvn clean install`
3. Add the `server-framework` dependency in your project
```xml
<dependency>
   <groupId>com.hbsoo</groupId>
   <artifactId>server-framework</artifactId>
   <version>1.0.0</version>
</dependency>
```
4. Add the `server-framework` Configuration to your Springboot project `application.yml` file.
```yaml
hbsoo:
  server:
    id: 1000 #Current node id
    threadPoolSize:
      innerClient: 5 #Inner client side business thread pool size
      innerServer: 5 #Inner server side business thread pool size
      outerServer: 5 #outer server side business thread pool size
    outerServer:
      enable: true #Whether to enable the outer server
      port: 5555 #Outer server port
      protocol: "TCP,UDP,WEBSOCKET,HTTP" #Outer server protocol,Which protocols to use.
    innerServers:
      - host: 192.168.1.104
        port: 6000
        type: gateway #Inner server type,that's namespace customized
        clientAmount: 1 #Connect to inner server client amount
        weight: 10 #Inner server weight
        id: 1000 #Inner server id; At least one id in the list of innerServers is associated with the current node id
      - host: 192.168.1.104
        port: 6003
        type: hall
        clientAmount: 1
        id: 2000
      - host: 192.168.1.104
        port: 6006
        type: room
        clientAmount: 1
        id: 3000
```
### How to develop
1. You must had installed JDK 1.8+ and Maven 3.x
2. Clone the project and import it into your IDE
3. Run the project


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
23. ~~用户登录接口抽离由使用框架者实现~~，~~同时还有用户登录内网同步和登出内网同步接口~~
24. ~~内网消息不可达时，可选路由到相同类型的其他节点~~
25. 消息转发，重复发送检测
26. MQTT协议支持？
27. 协议头配置化？
28. 注册接口？
29. 接口权限控制？