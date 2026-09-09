package com.example.spring_boot_project_api;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.spring_boot_project_api.config.SecurityProperties;

@SpringBootApplication
@EnableConfigurationProperties(SecurityProperties.class)
public class SpringBootProjectApiApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(SpringBootProjectApiApplication.class, args);
    }
}
