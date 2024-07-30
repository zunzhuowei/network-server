package com.hbsoo.server.utils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 共享成员变量操作助手
 * Created by zun.wei on 2024/7/30.
 */
public class SharedBeanHelper {

    private static final Map<String, SharedBean> sharedBeans = new ConcurrentHashMap<>();

    private static ThreadPoolScheduler threadPoolScheduler() {
        //outsideServerThreadPoolScheduler,insideServerThreadPoolScheduler
        return SpringBeanFactory.getBean("insideServerThreadPoolScheduler", ThreadPoolScheduler.class);
    }

    public static <T> Optional<T> getBean(String uniqueKey, Class<T> clazz) {
        Optional<SharedBean> sharedBean = getSharedBean(uniqueKey, clazz);
        return sharedBean.map(bean -> (T) bean.bean);
    }

    private static <T> Optional<SharedBean> getSharedBean(String uniqueKey, Class<T> clazz) {
        return Optional.ofNullable(sharedBeans.get(clazz.getName() + ":" + uniqueKey));
    }

    public static <T> Optional<T> addBeanIfNotExist(T bean, String uniqueKey) {
        String beanKey = bean.getClass().getName() + ":" + uniqueKey;
        SharedBean sharedBean = sharedBeans.computeIfAbsent(beanKey, key -> {
            int threadKey = threadPoolScheduler().getOperationThreadKey(key);
            return new SharedBean(threadKey, bean);
        });
        return Optional.ofNullable((T) sharedBean.bean);
    }

    public static <T> Optional<T> addBeanIfNotExist(T bean, Function<T, String> uniqueKeyFunction) {
        String uniqueKey = uniqueKeyFunction.apply(bean);
        return addBeanIfNotExist(bean, uniqueKey);
    }

    public static <T> void removeBean(String uniqueKey, Class<T> clazz) {
        String beanKey = clazz.getName() + ":" + uniqueKey;
        Optional<SharedBean> sharedBean = getSharedBean(uniqueKey, clazz);
        if (!sharedBean.isPresent()) {
            return;
        }
        synchronized (sharedBean.get()) {
            ThreadPoolScheduler scheduler = threadPoolScheduler();
            int threadKey = sharedBean.get().operationThreadKey;
            scheduler.executeByThreadIndex(threadKey, () -> sharedBeans.remove(beanKey));
        }
    }

    public static <T> void removeBean(T bean, Function<T, String> uniqueKeyFunction) {
        String uniqueKey = uniqueKeyFunction.apply(bean);
        removeBean(uniqueKey, bean.getClass());
    }

    public static <T> void removeBean(T bean, String uniqueKey) {
        removeBean(uniqueKey, bean.getClass());
    }

    public static <T> void operateBean(T bean, Function<T, String> uniqueKeyFunction, Consumer<T> operateConsumer,
                                       Consumer<T> notFoundConsumer) {
        String uniqueKey = uniqueKeyFunction.apply(bean);
        Optional<SharedBean> sharedBean = getSharedBean(uniqueKey, bean.getClass());
        if (!sharedBean.isPresent()) {
            notFoundConsumer.accept(bean);
            return;
        }
        synchronized (sharedBean.get()) {
            ThreadPoolScheduler scheduler = threadPoolScheduler();
            scheduler.executeByThreadIndex(sharedBean.get().getOperationThreadKey(),
                    () -> operateConsumer.accept((T) sharedBean.get().bean));
        }
    }

    public static <T> void operateBean(String uniqueKey, Class<T> clazz, Consumer<T> operateConsumer,
                                       Runnable notFoundConsumer) {
        Optional<SharedBean> sharedBean = getSharedBean(uniqueKey, clazz);
        if (!sharedBean.isPresent()) {
            notFoundConsumer.run();
            return;
        }
        synchronized (sharedBean.get()) {
            ThreadPoolScheduler scheduler = threadPoolScheduler();
            scheduler.executeByThreadIndex(sharedBean.get().getOperationThreadKey(),
                    () -> operateConsumer.accept((T) sharedBean.get().bean));
        }
    }

    private static class SharedBean {
        private final int operationThreadKey;
        private final Object bean;

        private SharedBean(int operationThreadKey, Object bean) {
            this.operationThreadKey = operationThreadKey;
            this.bean = bean;
        }

        public int getOperationThreadKey() {
            return operationThreadKey;
        }

        public Object getBean() {
            return bean;
        }
    }

}
