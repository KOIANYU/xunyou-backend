package com.hgz.xunyoubackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.hgz.xunyoubackend.mapper")
@EnableScheduling
public class XunyouBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(XunyouBackendApplication.class, args);
    }

}
