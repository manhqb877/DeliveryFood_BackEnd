package com.fooddelivery.core.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CoreController {

    @GetMapping
    public String getContent(){
        return "hiAuth";
    }
}
