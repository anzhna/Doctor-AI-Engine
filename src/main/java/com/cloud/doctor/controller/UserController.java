package com.cloud.doctor.controller;

import com.cloud.doctor.common.Result;
import com.cloud.doctor.entity.dto.UserLoginReq;
import com.cloud.doctor.entity.dto.UserRegisterReq;
import com.cloud.doctor.entity.vo.UserLoginVO;
import com.cloud.doctor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
}
