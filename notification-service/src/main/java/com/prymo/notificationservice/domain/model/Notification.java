package com.prymo.notificationservice.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    private String gateway;
    private String recipient;
    private String message;
}
