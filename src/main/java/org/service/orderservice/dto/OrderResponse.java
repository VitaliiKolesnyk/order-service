package org.service.orderservice.dto;

import org.service.orderservice.entity.ContactDetails;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(Long id,
                            String orderNumber,
                            int quantity,
                            List<ProductDto> products,
                            ContactDetailsResponse contactDetails,
                            Double totalAmount,
                            Status status) {
}
