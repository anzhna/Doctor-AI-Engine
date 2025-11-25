package com.cloud.doctor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.cloud.doctor.mapper")
@SpringBootApplication
public class DoctorApplication {
    public static void main(String[] args) {
        SpringApplication.run(DoctorApplication.class, args);
        System.out.println("---------- (♥◠‿◠)ﾉ  云医智能启动成功   ლ(╹◡╹ლ) ----------");
    }
}
