package com.gvera.gverabot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@Slf4j
@EnableJpaAuditing
public class GveraBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(GveraBotApplication.class, args);
    }

}
