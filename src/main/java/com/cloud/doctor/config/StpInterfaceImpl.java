package com.cloud.doctor.config;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自定义权限验证接口扩展
 * 作用：告诉 Sa-Token 当前登录用户拥有哪些角色和权限
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    /**
     * 返回一个账号所拥有的权限码集合 (目前我们只用角色，这个可以先不管，返回空)
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (核心！)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // loginId 是 Object 类型的，转成 long
        long userId = Long.parseLong(loginId.toString());

        List<String> list = new ArrayList<>();

        // --- 这里是核心逻辑 ---

        // 方案 A：去数据库查 Role (userMapper.selectById(userId).getRole())

        // 方案 B (偷懒版)：我们约定 ID=1 的超级账号是管理员
        if (userId == 1) {
            list.add("admin"); // 即使是管理员，通常也拥有普通用户权限，也可以加 "user"
        } else {
            list.add("user");  // 其他人都是普通用户
        }

        return list;
    }
}
