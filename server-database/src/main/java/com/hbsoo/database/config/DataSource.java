package com.hbsoo.database.config;

import java.lang.annotation.*;

/**
 * 自定义多数据源切换注解
 *
 * 优先级：先方法，后类，如果方法覆盖了类上的数据源类型，以方法的为准，否则以类上的为准
 *
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DataSource
{
    /**
     * 切换数据源名称
     */
    public DataSourceType value() default DataSourceType.MASTER;

    /**
     * 数据源名称,优先使用dsName 再使用 value值
     */
    public String dsName() default "";

    /**
     * 数据源名称所在方法的参数下标位置；
     *  functionName(int a,String dataSourceName); @DataSource(dsNameIndex = 1)
     */
    public int dsNameIndex() default -1;

    /**
     * 数据源参数名，配合 dsNameIndex 使用;
     * dsNameIndex 得到对象，再根据对象getter（dsParamName）得到数据源名称
     */
    public String dsParamName() default "";


}