## server-message-queue
> Use the `server-framework` module to develop a message queue module that supports message subscription and message push.

### How to use
* Add the `server-message-queue` dependency in your project
```xml
 <dependency>
    <groupId>com.hbsoo</groupId>
    <artifactId>server-message-queue</artifactId>
    <version>1.0.0</version>
</dependency>
```
* Define the message topic and broker server type
```java
package com.hbsoo.gateway.queue;

import com.hbsoo.message.queue.config.MessageListener;
import com.hbsoo.message.queue.QueueMessageHandler;
import com.hbsoo.server.NowServer;
import com.hbsoo.server.config.ServerInfo;

@MessageListener(topic = "test", serverType = "hall")
public class MessageQueueTest implements QueueMessageHandler {

    @Override
    public boolean handle(String objJson) {
        final ServerInfo serverInfo = NowServer.getServerInfo();
        System.out.println("objJson = " + objJson);
        System.out.println("serverInfo = " + serverInfo.getId());
        return false;
    }
}
```
* Send a message
```java
QueueMessageSender.publish("hall", "test", genealogies.toString());
```