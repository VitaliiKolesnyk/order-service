package org.service.orderservice.service.impl;

import org.service.orderservice.client.InventoryClient;
import org.service.orderservice.dto.*;
import org.service.orderservice.entity.ContactDetails;
import org.service.orderservice.entity.Order;
import org.service.orderservice.entity.OrderProduct;
import org.service.orderservice.entity.Product;
import org.service.orderservice.event.NotificationEvent;
import org.service.orderservice.exception.NotInStockException;
import org.service.orderservice.mapper.OrderMapper;
import org.service.orderservice.mapper.ProductMapper;
import org.service.orderservice.repository.OrderRepository;
import org.service.orderservice.repository.ProductRepository;
import org.service.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final ProductRepository productRepository;

    @Override
    public OrderResponse placeOrder(OrderRequest orderRequest) {
        InStockRequest inStockRequest = new InStockRequest(orderRequest.products());
        boolean inStock = inventoryClient.isInStock(inStockRequest);

        if (!inStock) {
            throw new NotInStockException("Products are not in stock, please contact us");
        }

        String orderNumber = UUID.randomUUID().toString();
        Double totalAmount = orderRequest.products().stream().mapToDouble(dto -> dto.price()).sum() * orderRequest.quantity();

        Order order = orderMapper.map(orderRequest);
        order.setTotalAmount(totalAmount);
        order.setOrderNumber(orderNumber);
        order.setQuantity(orderRequest.quantity());
        order.setStatus(Status.NEW);
        order.setProducts(getProducts(orderRequest.products(), order));
        setOrderProduct(order, orderRequest.products());
        ContactDetails contactDetails = order.getContactDetails();
        contactDetails.setOrder(order);

        Order savedOrder = orderRepository.save(order);
        sendEventToKafka(orderNumber, orderRequest);

        return orderMapper.map(savedOrder);
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

       return orderMapper.map(orderRepository.save(order));
    }

    @Override
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    @Override
    public void sendEventToKafka(String orderNumber, OrderRequest orderRequest) {
        NotificationEvent event = new NotificationEvent();
        event.setEmail(orderRequest.userDetails().email());
        event.setSubject(String.format("Order %s was received", orderNumber));
        event.setMessage(String.format("""
                Dear %s,
                
                Your order was received and currently is being processed;
                """, orderRequest.contactDetails().name()));

        log.info("Start - Sending orderPlacedEvent {} to Kafka topic order-placed", event);

        kafkaTemplate.send("order-placed-topic", event);

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
}
