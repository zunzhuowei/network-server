## server-access-control
> The `access control` module of the framework,
> Use `Spring AOP` and annotations and `Guava library` to limit the rate of the interface.
> You can limit the rate of interfaces uniformly or based on user granularity, and configure IP blacklists and whitelists.
> 
### How to use
1. Configure globally in application.yml file
```yaml
hbsoo:
  server:
    access:
      globalRateSize: 1000 #Global throttling of the number of visits per second
      userRateSize: 10 #Users are throttled on the number of visits per second
      blockIpList: #IP access is prohibited
        - 127.0.0.1
      whiteIpList: #IP access is allowed,Remove all traffic restrictions
        - 127.0.0.1
```
2. Use the AccessLimit annotation for personalized configurations
```java
@AccessLimit(rateSize = 1000, userRateSize = 10)
@OuterServerMessageHandler(value = 0, uri = "/index", protocol = Protocol.HTTP)
public class IndexAction extends HttpServerMessageDispatcher {
    
}
```