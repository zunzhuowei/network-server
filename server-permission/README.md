## server-permission

> The `permission` module of the framework,
> Use `Spring AOP` and annotations to control interface permissions.
> The HTTP protocol uses the request header entrainment JWT method,
> the UDP protocol uses the JWT string as the first field,
> and the AttributeKey:permission field in the channel is used for TCP and WebSocket.

### How to use

```java
@PermissionAuth(permission = {Permission.USER}, permissionStr = {"user:add", "user:delete"})
@OuterServerMessageHandler(value = 0, uri = "/index", protocol = Protocol.HTTP)
public class IndexAction extends HttpServerMessageDispatcher {

}
```
### Attention
* If the client uses `http` protocol, you need to add the Authentication field to the request header,
  which is a jwt string. Generated with the `JwtUtils` class.
  When setting parameters, add a permission parameter, which is a permission string, and multiple permissions are separated by commas
  
* In the client uses `UDP` protocol, the first field requested must be a jwt string, generated with the `JwtUtils` class.
  When setting parameters, add a permission parameter, which is a permission string, and multiple permissions are separated by commas

* If the client is `TCP` or `WebSocket`, you need to save the permission string to the channel after the client logs in, as shown below.
```java
AttributeKeyConstants.setAttr(context.channel(),AttributeKeyConstants.permissionAttr,new String[]{"user","admin"});
```

