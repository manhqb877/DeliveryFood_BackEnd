package com.fooddelivery.auth.exception;


public class ForBiddenException extends RuntimeException {
    public ForBiddenException(String message) {
        super(message);
    }
}