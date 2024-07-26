//package com.hbsoo.gateway.service.impl;
//
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.hbsoo.gateway.entity.Genealogy;
//import com.hbsoo.gateway.mapper.GenealogyMapper;
//import com.hbsoo.gateway.service.IGenealogyService;
//import org.redisson.api.RedissonClient;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.cache.CacheManager;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//import java.util.List;
//import java.util.concurrent.Callable;
//
///**
// * Created by zun.wei on 2024/6/15.
// */
//@Service
////@CacheConfig(cacheManager = "cacheManager")
////@CacheConfig(cacheManager = "redissonSpringCacheManager")
//public class GenealogyService extends ServiceImpl<GenealogyMapper, Genealogy> implements IGenealogyService {
//
//    @Resource
//    GenealogyMapper genealogyMapper;
//    @Autowired
//    private RedissonClient redissonClient;
//    @Autowired
//    private CacheManager cacheManager;
//    @Qualifier("redissonSpringCacheManager")
//    @Autowired
//    private CacheManager redissonSpringCacheManager;
//
//
////    @Override
////    @DataSource(dsName = "SLAVE")
////    @Cacheable(cacheNames = "genealogy", key = "'genealogy'")
////    public List<Genealogy> listAll() {
////        final String id = redissonClient.getId();
////        System.out.println("id = " + id);
////        final RBucket<String> test = redissonClient.getBucket(PlainOptions.name("test")
////                .timeout(Duration.ofSeconds(3))
////                .retryInterval(Duration.ofSeconds(5)));
////        final List<Genealogy> list = list();
////        test.set(list.toString());
////        return list;
////    }
//
//    @Override
//    public List<Genealogy> listAll() {
//        return cacheManager.getCache("genealogy")
//                .get("genealogy", new Callable<List<Genealogy>>() {
//                    @Override
//                    public List<Genealogy> call() throws Exception {
//                        return redissonSpringCacheManager.getCache("genealogy")
//                                .get("genealogy", new Callable<List<Genealogy>>() {
//                                    @Override
//                                    public List<Genealogy> call() throws Exception {
//                                        final String id = redissonClient.getId();
//                                        System.out.println("id = " + id);
//                                        return list();
//                                    }
//                                });
//                    }
//                });
//    }
//
////    public User getUserById(Long userId) {
////        // 1. 尝试从本地Caffeine缓存中获取
////        Cache.ValueWrapper valueWrapper = cacheManager.getCache("userCache").get(userId);
////        if (valueWrapper != null) {
////            return (User) valueWrapper.get();
////        }
////
////        // 2. 本地缓存未命中，尝试从Redis中获取
////        RMapCache<Long, User> userMap = redissonClient.getMapCache("userCache");
////        User userFromRedis = userMap.get(userId);
////        if (userFromRedis != null) {
////            // 同时将数据放入本地Caffeine缓存
////            cacheManager.getCache("userCache").put(userId, userFromRedis);
////            return userFromRedis;
////        }
////
////        // 3. Redis也未命中，从MySQL中获取
////        User userFromDB = userRepository.findById(userId).orElse(null);
////        if (userFromDB != null) {
////            // 数据存入Redis和本地Caffeine缓存
////            userMap.fastPut(userId, userFromDB, 60, TimeUnit.SECONDS); // 设置Redis缓存过期时间为60秒，按需调整
////            cacheManager.getCache("userCache").put(userId, userFromDB);
////        }
////
////        return userFromDB;
////    }
//}
