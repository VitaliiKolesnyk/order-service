package org.service.orderservice.service.impl;

import org.service.orderservice.client.InventoryClient;
import org.service.orderservice.dto.OrderRequest;
import org.service.orderservice.dto.OrderResponse;
import org.service.orderservice.entity.Order;
import org.service.orderservice.event.OrderPlacedEvent;
import org.service.orderservice.mapper.OrderMapper;
import org.service.orderservice.repository.OrderRepository;
import org.service.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    private final OrderMapper orderMapper;

    private final InventoryClient inventoryClient;

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Override
    public OrderResponse placeOrder(OrderRequest orderRequest) {
        boolean inStock = inventoryClient.isInStock(orderRequest.skuCode(), orderRequest.quantity());

        if (!inStock) {
            throw new RuntimeException("Product with Skucode " + orderRequest.skuCode() + " is not in stock");
        }

        Order order = orderMapper.map(orderRequest);

        Order savedOrder = orderRepository.save(order);

        sendEventroKafka(orderRequest);

        return orderMapper.map(savedOrder);
    }

    private void sendEventroKafka(OrderRequest orderRequest) {
        OrderPlacedEvent orderPlacedEvent = new OrderPlacedEvent(orderRequest.orderNumber(), orderRequest.email() );

        log.info("Start - Sending orderPlacedEvent {} to Kafka topic order-placed", orderPlacedEvent);

        kafkaTemplate.send("order-placed", orderPlacedEvent);

        log.info("End - Sending orderPlacedEvent to Kafka topic order-placed");
    }
}
