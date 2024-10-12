package org.service.orderservice.event;

import lombok.Data;

@Data
public class NotificationEvent {
    private String subject;

    private String message;

    private String email;
}
