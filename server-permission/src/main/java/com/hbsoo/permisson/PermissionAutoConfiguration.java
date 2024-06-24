package com.hbsoo.permisson;

import com.hbsoo.permisson.utils.AESUtil;
import com.hbsoo.permisson.utils.JwtUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Created by zun.wei on 2024/6/24.
 */
@Import({
        PermissionAspect.class,
        JwtUtils.class,
        AESUtil.class,
})
@Configuration
public class PermissionAutoConfiguration {



}
