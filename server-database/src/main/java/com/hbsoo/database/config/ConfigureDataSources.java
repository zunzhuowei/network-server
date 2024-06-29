package com.hbsoo.database.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.hbsoo.database.entity.DruidSource;
import com.hbsoo.database.utils.DynamicDataSource;
import com.hbsoo.database.utils.RealDruidSources;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by zun.wei on 2024/1/1.
 */
@Import({DataSourceExtAspect.class})
@Configuration
public class ConfigureDataSources {

    @Autowired
    private Environment environment;


    @Bean
    public DynamicDataSource dataSource(DruidProperties druidProperties) {
        List<DruidSource> druidSources = new ArrayList<>();
        MutablePropertySources sources = ((AbstractEnvironment) environment).getPropertySources();
        for (PropertySource<?> source : sources) {
            if (source instanceof EnumerablePropertySource) {
                final String[] names = ((EnumerablePropertySource) source).getPropertyNames();
                List<String> collect = Arrays.stream(names)
                        .filter(name -> name.startsWith("spring.datasource.druid.sources"))
                        .map(key -> key.split("[.]")[4])
                        .distinct()
                        .collect(Collectors.toList());
                if (collect.isEmpty()) {
                    continue;
                }
                for (int i = 0; i < collect.size(); i++) {
                    String sourceName = collect.get(i);
                    String urlKey = "spring.datasource.druid.sources[" + sourceName + "].url";
                    String usernameKey = "spring.datasource.druid.sources[" + sourceName + "].username";
                    String passwordKey = "spring.datasource.druid.sources[" + sourceName + "].password";
                    DruidSource druidSource = new DruidSource();
                    druidSource.setSourceName(sourceName);
                    druidSource.setPassword(environment.getProperty(passwordKey));
                    druidSource.setUrl(environment.getProperty(urlKey));
                    druidSource.setUsername(environment.getProperty(usernameKey));
                    druidSources.add(druidSource);
                }
            }
        }
        Map<Object, Object> targetDataSources = new HashMap<>();
        for (DruidSource druidSource : druidSources) {
            RealDruidSources.put(druidSource.getSourceName().toUpperCase(), druidSource);
            DruidDataSource druidDataSource = new DruidDataSource();
            druidDataSource.setUrl(druidSource.getUrl());
            druidDataSource.setUsername(druidSource.getUsername());
            druidDataSource.setPassword(druidSource.getPassword());
            DruidDataSource dataSource = druidProperties.dataSource(druidDataSource);
            targetDataSources.put(druidSource.getSourceName().toUpperCase(), dataSource);
        }
        DataSource master = (DataSource) targetDataSources.get("MASTER");
        if (Objects.isNull(master)) throw new RuntimeException("master datasource not configure!");
        return new DynamicDataSource(master, targetDataSources);
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DynamicDataSource dynamicDataSource) {
        return new DataSourceTransactionManager(dynamicDataSource);
    }

}
