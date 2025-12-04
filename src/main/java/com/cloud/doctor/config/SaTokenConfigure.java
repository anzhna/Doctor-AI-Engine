package com.cloud.doctor.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(new SaInterceptor(handle->{
                    StpUtil.checkLogin();
                }))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",      // 登录
                        "/user/register",   // 注册
                        "/doc.html",        // Swagger 文档
                        "/webjars/**",      // Swagger
                        "/v3/api-docs/**"   // Swagger
                );

    }

}
