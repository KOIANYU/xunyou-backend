package com.hgz.xunyoubackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.hgz.xunyoubackend.mapper")
public class XunyouBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(XunyouBackendApplication.class, args);
    }

}
