package com.hbsoo.gateway.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hbsoo.database.constants.DataSource;
import com.hbsoo.gateway.entity.Genealogy;
import com.hbsoo.gateway.mapper.GenealogyMapper;
import com.hbsoo.gateway.service.IGenealogyService;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.PlainOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.List;

/**
 * Created by zun.wei on 2024/6/15.
 */
@Service
public class GenealogyService extends ServiceImpl<GenealogyMapper, Genealogy> implements IGenealogyService {

    @Resource
    GenealogyMapper genealogyMapper;
    @Autowired
    private RedissonClient redissonClient;


    @Override
    @DataSource(dsName = "SLAVE")
    public List<Genealogy> listAll() {
        final String id = redissonClient.getId();
        System.out.println("id = " + id);
        final RBucket<String> test = redissonClient.getBucket(PlainOptions.name("test")
                .timeout(Duration.ofSeconds(3))
                .retryInterval(Duration.ofSeconds(5)));
        final String s = test.get();
        System.out.println("s = " + s);
        test.set(list().toString());
        final String s1 = test.get();
        System.out.println("s1 = " + s1);
        return list();
    }

}
