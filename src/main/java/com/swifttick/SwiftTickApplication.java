package com.swifttick;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SwiftTickApplication {
    public static void main(String[] args) {
        SpringApplication.run(SwiftTickApplication.class, args);
    }
}
