package com.fooddelivery.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.WebApplicationType;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ApiGatewayApplication.class);
        app.setWebApplicationType(WebApplicationType.REACTIVE); // Bắt buộc Netty/WebFlux
        app.run(args);
    }

}
