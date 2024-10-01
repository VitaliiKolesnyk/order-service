package org.service.orderservice.mapper;

import org.service.orderservice.dto.OrderRequest;
import org.service.orderservice.dto.OrderResponse;
import org.service.orderservice.entity.Order;

public interface OrderMapper {

    OrderResponse map(Order order);

    Order map(OrderRequest orderRequest);
}
