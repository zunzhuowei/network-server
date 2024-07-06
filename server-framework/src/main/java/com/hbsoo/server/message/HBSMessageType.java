package com.hbsoo.server.message;

/**
 * Created by zun.wei on 2024/6/5.
 */
public interface HBSMessageType {

    interface Inner {
        int LOGIN = 1; //内部服务登录
        int LOGOUT = 2; //内部服务登出
        int HEARTBEAT = 3; //内部服务心跳
        int REDIRECT = 4; //内部服务消息重定向到网关转发给外部用户
        int LOGIN_SYNC = 5; //同步网关登录的外部用户到本服务器
        int LOGOUT_SYNC = 6; //同步网关登出的外部用户到本服务器，然后清除本服务器的登录信息
        int SUBSCRIBE = 7; //注册订阅消息topic到队列服务器
        int UN_SUBSCRIBE = 8; //从队列服务器取消订阅消息topic
        int PUBLISH = 9; //发布消息到队列服务器
        int PUBLISH_PUSH = 10; //队列服务器推送消息给消费者服务器
        int RESULT_CALLBACK = 11; //消费者服务器处理结果，针对事务消息队列才有效
        int RESULT_CALLBACK_PUSH = 12; //将消费者服务器处理结果推送给事务发起方服务器
        int TRANSACTION_ROLLBACK = 13; //事务发起方服务器发起回滚事件到队列服务器中
        int TRANSACTION_ROLLBACK_PUSH = 14; //队列服务器推送回滚事件消息给消费者服务器
        int GATEWAY_ROUTING_HTTP_TO_INNER_SERVER = 15; //网关转发消息给内部服务
        int GATEWAY_ROUTING_WEBSOCKET_TCP_UDP_TO_INNER_SERVER = 16; //网关转发消息给内部服务
    }

    interface Outer {
        int LOGIN = 1; // 外部用户登录
        int LOGOUT = 2; // 外部用户登出
        int HEARTBEAT = 3;
        int PING = 4;
        int PONG = 5;
        int REDIRECT = 6;
        int REDIRECT_TO_SERVER = 7;
        int REDIRECT_TO_CLIENT = 8;
        int REDIRECT_TO_ALL_CLIENT = 9;
        int REDIRECT_TO_ALL_SERVER =10;
        int LOGIN_SYNC = 11;
        int PERMISSION_DENIED = 403; // 权限不足
        int TOO_MANY_REQUESTS = 429; // 请求过多（限流）
    }
}
