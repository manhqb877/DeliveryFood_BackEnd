package com.fooddelivery.tracking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrackingController {

    @GetMapping
    public String getContent(){
        return "hiAuth";
    }
}
