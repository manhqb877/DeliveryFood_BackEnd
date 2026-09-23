package com.fooddelivery.auth.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private String eventId;
    private String eventType;
    private Long userId;
    private String phone;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private Long areaId;
    private String action;
    private Instant timestamp;
}
