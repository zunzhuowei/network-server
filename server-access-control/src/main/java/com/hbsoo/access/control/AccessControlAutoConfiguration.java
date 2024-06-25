package com.hbsoo.access.control;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Created by zun.wei on 2024/6/25.
 */
@Import({
        AccessLimitAspect.class,
        AccessControlProperties.class
})
@Configuration
public class AccessControlAutoConfiguration {

    @Autowired
    private AccessControlProperties accessControlProperties;

    @Bean
    public SystemRateLimiter systemRateLimiter() {
        return new SystemRateLimiter(accessControlProperties.getGlobalRateSize(), accessControlProperties.getUserRateSize());
    }

}
