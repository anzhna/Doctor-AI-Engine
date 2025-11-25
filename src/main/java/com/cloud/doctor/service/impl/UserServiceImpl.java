package com.cloud.doctor.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.doctor.entity.User;
import com.cloud.doctor.entity.dto.UserLoginReq;
import com.cloud.doctor.entity.dto.UserRegisterReq;
import com.cloud.doctor.entity.vo.UserLoginVO;
import com.cloud.doctor.service.UserService;
import com.cloud.doctor.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
* @author
* @description 针对表【sys_user(用户表)】的数据库操作Service实现
* @createDate 2025-11-24
*/
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    private final UserMapper userMapper;

    @Override
    public void register(UserRegisterReq req) {
        // 1. 校验手机号是否已存在
        User existUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, req.phone())); // record 的 get 方法不带 get 前缀，直接用 phone()

        if (existUser != null) {
            throw new RuntimeException("该手机号已注册");
        }

        // 2. 密码加密 (BCrypt加密)
        String encryptedPwd = BCrypt.hashpw(req.password());

        // 3. 构建 User 对象并保存
        User user = new User();
        user.setPhone(req.phone());
        user.setPassword(encryptedPwd);
        user.setRealName(req.realName());
        user.setUsername("用户" + req.phone().substring(7)); // 默认生成一个用户名

        userMapper.insert(user);
    }

    @Override
    public UserLoginVO login(UserLoginReq req) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, req.phone()));

        if (user == null) {
            throw new RuntimeException("手机号不存在");
        }
        if (!BCrypt.checkpw(req.password(), user.getPassword())) {
            throw new RuntimeException("账号或密码错误");
        }

        StpUtil.login(user.getId());

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        UserLoginVO userLoginVO = UserLoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .token(tokenInfo.tokenValue)
                .tokenName(tokenInfo.getTokenName())
                .build();

        return userLoginVO;
    }
}




