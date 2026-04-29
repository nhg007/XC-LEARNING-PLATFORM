package com.xc.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StudyApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyApiApplication.class, args);
    }
}
