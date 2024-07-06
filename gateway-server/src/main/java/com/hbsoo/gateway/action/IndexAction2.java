package com.hbsoo.gateway.action;

import com.hbsoo.database.utils.TransactionUtils;
import com.hbsoo.gateway.entity.User;
import com.hbsoo.gateway.service.IUserService;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.entity.HttpPackage;
import com.hbsoo.server.message.server.HttpServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.Date;
import java.util.List;

/**
 * Created by zun.wei on 2024/6/15.
 */
@OuterServerMessageHandler(value = 0, uri = "/index2", protocol = Protocol.HTTP)
public class IndexAction2 extends HttpServerMessageDispatcher {


    @Autowired
    private IUserService userService;
    @Autowired
    private TransactionUtils transactionUtils;

    @Override
    public void handle(ChannelHandlerContext ctx, HttpPackage httpPackage) {
        final List<User> users = userService.listAll();
        System.out.println("users = " + users);
        TransactionStatus transactionStatus = transactionUtils.begin(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
        User user = null;
        try {
            user = new User();
            user.setUsername("zunwei");
            user.setPassword("123456");
            user.setSex(1);
            user.setNickname("zunwei");
            user.setCity("beijing");
            user.setCountry("china");
            user.setProvince("beijing");
            user.setHeadimgurl("");
            user.setPrivilege("");
            user.setAuth(1);
            user.setStatus(1);
            user.setSalt("");
            user.setAndroidId("zunwei");
            user.setOpenid("zunwei");
            user.setUnionid("zunwei");
            user.setCreateTime(new Date());
            Integer add = userService.addUser(user);
            user.setPassword("654321");
            if (true) {
                //throw new RuntimeException("ddd");
            }
            userService.updateUser(user);
            //new Thread(() -> {
                transactionUtils.commit(transactionStatus);
            //}).start();
        } catch (Exception e) {
            e.printStackTrace();
            // 手动回滚事务
            //new Thread(() -> {
                transactionUtils.rollback(transactionStatus);
            //}).start();*/
        }
        responseJson(ctx, httpPackage, user);

    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }
}
