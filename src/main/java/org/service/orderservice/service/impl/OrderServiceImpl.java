package org.service.orderservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.service.orderservice.client.InventoryClient;
import org.service.orderservice.dto.*;
import org.service.orderservice.entity.ContactDetails;
import org.service.orderservice.entity.Order;
import org.service.orderservice.entity.OrderProduct;
import org.service.orderservice.entity.Product;
import org.service.orderservice.event.OrderCancelEvent;
import org.service.orderservice.event.OrderEvent;
import org.service.orderservice.event.ProductEvent;
import org.service.orderservice.exception.NotInStockException;
import org.service.orderservice.mapper.OrderMapper;
import org.service.orderservice.mapper.ProductMapper;
import org.service.orderservice.repository.OrderRepository;
import org.service.orderservice.repository.ProductRepository;
import org.service.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService  {

    private final OrderRepository orderRepository;

    private final OrderMapper orderMapper;

    private final ProductMapper productMapper;

    private final InventoryClient inventoryClient;

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final ProductRepository productRepository;

    @Override
    public OrderResponse placeOrder(OrderRequest orderRequest) {
        String orderNumber = UUID.randomUUID().toString();
        ReserveProductsRequest reserveProductsRequest = new ReserveProductsRequest(orderRequest.products(), orderNumber);

        try {
            reserveProducts(reserveProductsRequest);
        } catch (RuntimeException e) {
            log.error("Failed to reserve products: {}", e.getMessage());
            throw e;
        }

        Double totalAmount = orderRequest.products().stream().mapToDouble(dto -> dto.price()).sum() * orderRequest.quantity();

        Order order = orderMapper.map(orderRequest);
        order.setTotalAmount(totalAmount);
        order.setOrderNumber(orderNumber);
        order.setQuantity(orderRequest.quantity());
        order.setStatus(Status.NEW);
        order.setProducts(getProducts(orderRequest.products(), order));
        order.setOrderedAt(LocalDateTime.now());
        setOrderProduct(order, orderRequest.products());
        ContactDetails contactDetails = order.getContactDetails();
        contactDetails.setOrder(order);

        Order savedOrder = orderRepository.save(order);
        sendEventToKafka("Placed", orderNumber, contactDetails.getName(), contactDetails.getEmail());

        return orderMapper.map(savedOrder);
    }

    public void reserveProducts(ReserveProductsRequest reserveProductsRequest) {
        ResponseEntity<Boolean> responseEntity = inventoryClient.reserveProducts(reserveProductsRequest);

        if (responseEntity.getStatusCode().is5xxServerError() && responseEntity.getBody() != null && !responseEntity.getBody()) {
            throw new RuntimeException("Circuit breaker enabled");
        }

        if (responseEntity.getBody() != null && !responseEntity.getBody()) {
            throw new NotInStockException("Products are out of stock");
        }
    }

    @Override
    public List<OrderResponse> getOrders() {
        List<Order> orders = orderRepository.findAll();

        return orderMapper.map(orders);
    }

    @Override
    public OrderResponse updateOrder(Long id, OrderRequest orderRequest) {
        Order order = orderRepository.findById(id).orElseThrow( () ->
                new RuntimeException("Order with id " + orderRequest.id() + " not found"));

        order.setStatus(orderRequest.status());

        if (orderRequest.status().equals(Status.DELIVERED)) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        if (orderRequest.status().equals(Status.CANCELLED)) {
            String orderNumber = orderRequest.orderNumber();
            ContactDetails contactDetails = getContactDetailsByOrderNumber(orderNumber);
            sendEventToKafka("Canceled", orderNumber, contactDetails.getName(), contactDetails.getEmail());
        }

       return orderMapper.map(orderRepository.save(order));
    }

    @Override
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    @Override
    public void sendEventToKafka(String status, String orderNumber, String name, String email) {
        OrderEvent event = new OrderEvent(status, orderNumber, name, email);

        log.info("Start - Sending orderPlacedEvent {} to Kafka topic order-topic", event);

        kafkaTemplate.send("order-topic", event);

        log.info("End - Sending orderPlacedEvent to Kafka topic order-placed");
    }

    @Override
    public OrderResponse getOrder(Long id) {
        Order order =  orderRepository.findById(id).
                orElseThrow(() -> new RuntimeException("Order not found"));

        return orderMapper.map(order);
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(String userId) {
        return orderMapper.map(orderRepository.findOrdersByUserId(userId));
    }

    private List<Product> getProducts(List<ProductDto> productDtos, Order order) {
        List<Product> products = new ArrayList<>();

        productDtos.forEach(productDto -> {
            Optional<Product> product = productRepository.findBySkuCode(productDto.skuCode());

            if (product.isPresent()) {
                Product productEntity = product.get();
                products.add(productEntity);

                productEntity.getOrders().add(order);
            }
        });

        return products;
    }

    private void setOrderProduct(Order order, List<ProductDto> products) {
        for (ProductDto product : products) {
            Product productFromDb = productRepository.findBySkuCode(product.skuCode())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            OrderProduct orderProduct = new OrderProduct();
            orderProduct.setProduct(productFromDb);
            orderProduct.setOrder(order);
            orderProduct.setQuantity(product.quantity());

            order.getOrderProducts().add(orderProduct);
            productFromDb.getOrderProducts().add(orderProduct);
        }
    }

    @KafkaListener(topics = {"product-events", "order-cancel-events"}, groupId = "order-service-group")
    public void listen(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        String message = record.value();

        switch (topic) {
            case "product-events":
                ProductEvent productEvent = deserialize(message, ProductEvent.class);
                handleProductEvent(productEvent);
                break;
            case "order-cancel-events":
                OrderCancelEvent orderCancelEvent = deserialize(message, OrderCancelEvent.class);
                handleOrderCancelEvent(orderCancelEvent);
                break;
            default:
                throw new IllegalArgumentException("Unknown topic: " + topic);
        }
    }

    private <T> T deserialize(String json, Class<T> targetType) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, targetType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON message", e);
        }
    }

    private void handleProductEvent(ProductEvent productEvent) {
        log.info("Got Message from product-events topic {}", productEvent);

        String action = productEvent.action();

        switch (action) {
            case "CREATE": saveProduct(productEvent);
                break;
            case "DELETE": deleteProduct(productEvent);
                break;
            case "UPDATE": updateProduct(productEvent);
                break;
        }
    }

    private void saveProduct(ProductEvent productEvent) {
        Product product = new Product();
        product.setName(productEvent.name());
        product.setDescription(productEvent.description());
        product.setPrice(productEvent.price());
        product.setSkuCode(productEvent.skuCode());
        product.setThumbnailUrl(productEvent.thumbnailUrl());

        productRepository.save(product);
    }

    private void deleteProduct(ProductEvent productEvent) {
        Product product = productRepository.findBySkuCode(productEvent.skuCode())
                .orElseThrow(() -> new RuntimeException("Product not found"));


        productRepository.delete(product);
    }

    private void updateProduct(ProductEvent productEvent) {
        Product product = productRepository.findBySkuCode(productEvent.skuCode())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName(productEvent.name());
        product.setDescription(productEvent.description());
        product.setPrice(productEvent.price());
        product.setThumbnailUrl(productEvent.thumbnailUrl());

        productRepository.save(product);
    }

    private void handleOrderCancelEvent(OrderCancelEvent orderCancelEvent) {
        log.info("Got Message from order-cancel-events topic {}", orderCancelEvent);

        Order order = orderRepository.findOrderByOrderNumber(orderCancelEvent.orderNumber())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getStatus().equals(Status.CANCELLED)) {
            order.setStatus(Status.CANCELLED);

            orderRepository.save(order);

            String orderNumber = orderCancelEvent.orderNumber();
            ContactDetails contactDetails = getContactDetailsByOrderNumber(orderNumber);
            sendEventToKafka("Canceled", orderNumber, contactDetails.getName(), contactDetails.getEmail());
        }
    }

    private ContactDetails getContactDetailsByOrderNumber(String orderNumber) {
        Order order = orderRepository.findOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        return order.getContactDetails();
    }
}
