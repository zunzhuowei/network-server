package com.hbsoo.server.session;

/**
 * 外部用户登录登出监听
 * Created by zun.wei on 2024/7/15.
 */
public interface OutsideUserLoginLogoutListener {

    /**
     * 在用户登录之前触发,触发之后就执行真正登录操作
     * @param userId 用户id
     * @param userSession 用户session
     */
    void onLogin(Long userId, UserSession userSession);

    /**
     * 在用户退出之前触发，触发之后就执行真正退出
     * @param userId 用户id
     */
    void onLogout(Long userId);

}
