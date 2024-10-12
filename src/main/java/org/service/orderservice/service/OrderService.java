package org.service.orderservice.service;


import org.service.orderservice.dto.OrderRequest;
import org.service.orderservice.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse placeOrder(OrderRequest orderRequest);

    List<OrderResponse> getOrders();

    OrderResponse updateOrder(Long id, OrderRequest orderRequest);

    void deleteOrder(Long id);

    void sendEventToKafka(String orderNumber, OrderRequest orderRequest);

    OrderResponse getOrder(Long id);

    List<OrderResponse> getOrdersByUserId(String userId);
}
