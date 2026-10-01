package com.fooddelivery.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.fooddelivery.orders.repository.mongo")
@EnableJpaRepositories(basePackages = "com.fooddelivery.orders.repository")
@EnableFeignClients(basePackages = "com.fooddelivery.orders.client")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}

