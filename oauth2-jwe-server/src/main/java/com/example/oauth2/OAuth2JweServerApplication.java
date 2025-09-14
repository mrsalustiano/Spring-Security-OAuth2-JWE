package com.example.oauth2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OAuth2JweServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OAuth2JweServerApplication.class, args);
    }
}