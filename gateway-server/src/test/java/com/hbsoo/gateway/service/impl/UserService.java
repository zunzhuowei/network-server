//package com.hbsoo.gateway.service.impl;
//
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.hbsoo.gateway.entity.User;
//import com.hbsoo.gateway.mapper.UserMapper;
//import com.hbsoo.gateway.service.IUserService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
///**
// * Created by zun.wei on 2024/6/29.
// */
//@Service
//public class UserService extends ServiceImpl<UserMapper, User> implements IUserService {
//
//
//    @Autowired
//    private UserMapper userMapper;
//
//    @Override
//    public User getUser(Long id) {
//        return null;
//    }
//
//    @Override
//    public List<User> listAll() {
//        return list();
//    }
//
//    @Override
//    public Integer addUser(User user) {
//        return save(user) ? 1 : 0;
//    }
//
//    @Override
//    public Integer updateUser(User user) {
//        return updateById(user) ? 1 : 0;
//    }
//
//    @Override
//    public Integer deleteUser(Long id) {
//        return removeById(id) ? 1 : 0;
//    }
//
//
//}
