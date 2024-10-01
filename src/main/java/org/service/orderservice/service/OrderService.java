package org.service.orderservice.service;


import org.service.orderservice.dto.OrderRequest;
import org.service.orderservice.dto.OrderResponse;

public interface OrderService {

    OrderResponse placeOrder(OrderRequest orderRequest);
}
