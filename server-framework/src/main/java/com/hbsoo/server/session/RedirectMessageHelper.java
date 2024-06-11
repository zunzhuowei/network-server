//package com.hbsoo.server.session;
//
//import com.hbsoo.server.message.HBSPackage;
//
///**
// * 内部消息重定向
// * Created by zun.wei on 2024/6/10.
// */
//public final class RedirectMessageHelper {
//    /*
//    1. 消息注册到其他服务器？
//    2. 消息注册到其他客户端？
//     */
//    public static void redirectMsg(HBSPackage.Builder msgBuilder, ServerType serverType, Long id) {
//        switch (serverType) {
//            case INNER_SERVER:
//                InnerServerSessionManager.sendMsg2ServerByTypeAndKey(msgBuilder, serverType, id);
//                break;
//            case OUTER_SERVER:
//                OuterServerSessionManager.sendMsg2ServerByTypeAndKey(msgBuilder, serverType, id);
//                break;
//            case INNER_CLIENT:
//                InnerClientSessionManager.sendMsg2ServerByTypeAndKey()
//        }
//    }
//
//    // 重定向消息
//    public static void redirectMsg(HBSPackage.Builder msgBuilder, ServerType serverType, Integer serverId) {
//        switch (serverType) {
//            case INNER_SERVER:
//                InnerServerSessionManager.sendMsg2ServerByServerId(msgBuilder, serverId);
//                break;
//            case OUTER_SERVER:
//                OuterServerSessionManager.sendMsg2ServerByServerId(msgBuilder, serverId);
//                break;
//            case INNER_CLIENT:
//                InnerClientSessionManager.sendMsg2ServerByServerId(msgBuilder, serverId);
//        }
//    }
//    //本服务消息转发
//    //内网服务消息转发
//    //延时消息转发
//    //toSelf,toServer
//
//}
