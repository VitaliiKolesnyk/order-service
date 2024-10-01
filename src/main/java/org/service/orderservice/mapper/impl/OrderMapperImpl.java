package org.service.orderservice.mapper.impl;

import org.service.orderservice.dto.OrderRequest;
import org.service.orderservice.dto.OrderResponse;
import org.service.orderservice.entity.Order;
import org.service.orderservice.mapper.OrderMapper;
import org.springframework.stereotype.Component;

@Component
public class OrderMapperImpl implements OrderMapper {

    public OrderResponse map(Order order) {
        return new OrderResponse(order.getId(), order.getOrderNumber(),
                order.getSkuCode(), order.getPrice(), order.getQuantity());
    }

    public Order map(OrderRequest orderRequest) {
        return Order.builder()
                .id(orderRequest.id())
                .orderNumber(orderRequest.orderNumber())
                .skuCode(orderRequest.skuCode())
                .price(orderRequest.price())
                .quantity(orderRequest.quantity())
                .build();
    }


}
