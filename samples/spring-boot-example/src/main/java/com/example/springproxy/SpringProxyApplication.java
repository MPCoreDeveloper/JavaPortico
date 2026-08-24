package com.example.springproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot app demonstrating DI wiring of the JavaPortico proxy pipeline:
 * a legacy REST petstore (Spring MVC) behind a generated {@code PetServiceProxy}.
 */
@SpringBootApplication
public class SpringProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringProxyApplication.class, args);
    }
}
