package com.studenthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class StudentHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudentHubApplication.class, args);
    }
}
