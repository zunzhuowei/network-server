package com.hbsoo.database.config;

import com.hbsoo.database.constants.DataSource;
import com.hbsoo.database.utils.DynamicDataSourceContextHolder;
import com.hbsoo.database.utils.RealDruidSources;
import com.hbsoo.database.utils.ReflectUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Created by zun.wei on 2024/1/7.
 */
@Aspect
@Order(0)
@Component
public class DataSourceExtAspect {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConfigureDataSources configureDataSources;

    @Pointcut("@annotation(org.springframework.stereotype.Controller)"
            + "|| @within(org.springframework.stereotype.Controller)"
            + "|| @annotation(com.hbsoo.database.constants.DataSource)"
            + "|| @within(com.hbsoo.database.constants.DataSource)"
    )
    public void dsPointCut() {

    }

    @Around("dsPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        DataSource dataSource = getDataSource(point);
        if (Objects.nonNull(dataSource)) {
            try {
                // 使用DataSource上的dsName作为数据源参数
                String dsName = dataSource.dsName();
                if (StringUtils.isBlank(dsName)) {
                    int index = dataSource.dsNameIndex();
                    final String dsParamName = dataSource.dsParamName();
                    if (index >= 0) {
                        Object ds = point.getArgs()[index];
                        if (StringUtils.isNotBlank(dsParamName)) {
                            // 使用方法上的参数下标和参数对象里的数据源参数作为数据源参数名称
                            dsName = ReflectUtils.invokeGetter(ds, dsParamName);
                        } else {
                            // 使用方法上的参数下标作为数据源参数
                            dsName = ds.toString();
                        }
                    } else {
                        dsName = dataSource.value().name();
                    }
                }
                dsName = dsName.toUpperCase();
                if (!RealDruidSources.exist(dsName)) {
                    // 如果数据源不存在，则使用默认数据源
                    //dsName = DataSourceType.MASTER.name();
                    throw new RuntimeException("数据源不存在:" + dsName);
                }
                DynamicDataSourceContextHolder.setDataSourceType(dsName);
                return point.proceed();
            } finally {
                // 销毁数据源 在执行方法之后
                DynamicDataSourceContextHolder.clearDataSourceType();
            }
        } else {
            try {
                return point.proceed();
            } finally {
                final String dataSourceType = DynamicDataSourceContextHolder.getDataSourceType();
                if (StringUtils.isNotBlank(dataSourceType)) {
                    // 销毁数据源 在执行方法之后
                    DynamicDataSourceContextHolder.clearDataSourceType();
                }
            }
        }
    }

    /**
     * 获取需要切换的数据源
     */
    public DataSource getDataSource(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        DataSource dataSource = AnnotationUtils.findAnnotation(signature.getMethod(), DataSource.class);
        if (Objects.nonNull(dataSource)) {
            return dataSource;
        }
        return AnnotationUtils.findAnnotation(signature.getDeclaringType(), DataSource.class);
    }

}
