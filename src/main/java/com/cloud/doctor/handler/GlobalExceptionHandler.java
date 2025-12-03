package com.cloud.doctor.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.cloud.doctor.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 作用：拦截系统中抛出的所有异常，统一转换为 Result 格式返回给前端
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 拦截 Sa-Token 未登录异常
    @ExceptionHandler(NotLoginException.class)
    public Result<String> handleNotLoginException(NotLoginException e) {
        log.warn("用户未登录或Token无效：{}", e.getMessage());
        // 返回 401 状态码（或者你约定的 500）
        return Result.error("请先登录！");
    }

    // 2. 拦截 Sa-Token 权限不足异常
    @ExceptionHandler(NotRoleException.class)
    public Result<String> handleNotRoleException(NotRoleException e) {
        log.warn("权限不足：{}", e.getMessage());
        return Result.error("无权访问，缺少角色：" + e.getRole());
    }

    // 3. 拦截业务异常 (RuntimeException)
    // 比如你在 Service 里抛出的 "号源已抢光"、"密码错误" 都会走到这里
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        log.error("业务异常：{}", e.getMessage()); // 打印简单日志
        return Result.error(e.getMessage()); // 把异常信息直接返给前端
    }

    // 4. 拦截所有其他未知异常 (兜底)
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统未知错误", e); // 打印完整堆栈，方便排查 bug
        return Result.error("系统繁忙，请稍后重试");
    }
}