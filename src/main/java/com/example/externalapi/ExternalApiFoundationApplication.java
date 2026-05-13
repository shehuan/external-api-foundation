package com.example.externalapi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@MapperScan("com.example.externalapi.mapper")
@ConfigurationPropertiesScan
@SpringBootApplication
public class ExternalApiFoundationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExternalApiFoundationApplication.class, args);
    }
}

