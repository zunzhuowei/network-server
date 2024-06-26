package com.hbsoo.server.message;

/**
 * Created by zun.wei on 2024/6/5.
 */
public interface HBSMessageType {

    interface Inner {
        int PUBLISH_PUSH = -2;
        int PUBLISH = -1;
        int SUBSCRIBE = 0;
        int LOGIN = 1;
        int LOGOUT = 2;
        int HEARTBEAT = 3;
        int PING = 4;
        int PONG = 5;
        int REDIRECT = 6;
        int REDIRECT_TO_SERVER = 7;
        int REDIRECT_TO_CLIENT = 8;
        int REDIRECT_TO_ALL_CLIENT = 9;
        int REDIRECT_TO_ALL_SERVER = 10;
        int LOGIN_SYNC = 11;
        int LOGOUT_SYNC = 12;
    }

    interface Outer {
        int LOGIN = 1;
        int LOGOUT = 2;
        int HEARTBEAT = 3;
        int PING = 4;
        int PONG = 5;
        int REDIRECT = 6;
        int REDIRECT_TO_SERVER = 7;
        int REDIRECT_TO_CLIENT = 8;
        int REDIRECT_TO_ALL_CLIENT = 9;
        int REDIRECT_TO_ALL_SERVER =10;
        int LOGIN_SYNC = 11;
        int PERMISSION_DENIED = 403;
        int TOO_MANY_REQUESTS = 429;
    }
}
