package com.fooddelivery.orders.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @GetMapping
    public String getContent(){
        return "hiAuth";
    }
}
