package com.cloud.doctor.service;

import com.cloud.doctor.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.doctor.entity.dto.UserLoginReq;
import com.cloud.doctor.entity.dto.UserRegisterReq;
import com.cloud.doctor.entity.dto.UserUpdateReq;
import com.cloud.doctor.entity.vo.UserInfoVO;
import com.cloud.doctor.entity.vo.UserLoginVO;

/**
* @author 10533
* @description 针对表【sys_user(用户表)】的数据库操作Service
* @createDate 2025-11-20 22:59:12
*/
public interface UserService extends IService<User> {
    //用户注册
    void register(UserRegisterReq req);

    //用户登录
    UserLoginVO login(UserLoginReq req);

    //查询个人信息
    UserInfoVO getUserInfo(Long userId);

    //修改个人信息
    void updateUserInfo(Long userId, UserUpdateReq req);


}
