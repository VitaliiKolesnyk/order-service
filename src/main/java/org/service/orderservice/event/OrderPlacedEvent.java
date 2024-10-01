package org.service.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class OrderPlacedEvent {

    private String orderNumber;

    private String email;
}
