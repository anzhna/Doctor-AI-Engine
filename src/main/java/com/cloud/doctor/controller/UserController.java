package com.cloud.doctor.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.cloud.doctor.common.Result;
import com.cloud.doctor.entity.dto.UserLoginReq;
import com.cloud.doctor.entity.dto.UserRegisterReq;
import com.cloud.doctor.entity.dto.UserUpdateReq;
import com.cloud.doctor.entity.vo.UserInfoVO;
import com.cloud.doctor.entity.vo.UserLoginVO;
import com.cloud.doctor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Tag(name = "用户管理模块") // Swagger 分类标题
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册") // Swagger 接口说明
    public Result<String> register(@RequestBody UserRegisterReq req) {
        try {
            userService.register(req);
            return Result.success("注册成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginReq req) {
        try {
            UserLoginVO vo = userService.login(req);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }


    // 1. 获取个人信息
    @GetMapping("/info")
    @Operation(summary = "获取当前登录用户信息")
    public Result<UserInfoVO> getUserInfo() {
        // 从 Token 获取 ID，不需要前端传，安全！
        long userId = StpUtil.getLoginIdAsLong();
        return Result.success(userService.getUserInfo(userId));
    }

    // 2. 修改个人信息
    @PostMapping("/update")
    @Operation(summary = "修改个人信息")
    public Result<String> updateUserInfo(@RequestBody UserUpdateReq req) {
        long userId = StpUtil.getLoginIdAsLong();
        userService.updateUserInfo(userId, req);
        return Result.success("修改成功");
    }

    // 3. 退出登录
    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<String> logout() {
        // Sa-Token 注销：删 Redis、让 Token 失效
        StpUtil.logout();
        return Result.success("退出成功");
    }
}
