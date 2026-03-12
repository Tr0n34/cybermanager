package com.cybermanager.api;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.cybermanager")
@EntityScan(basePackages = "com.cybermanager.infrastructure.entities")
@EnableJpaRepositories(basePackages = "com.cybermanager.infrastructure.repositories")
public class CmApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CmApiApplication.class, args);
    }
}

