package org.service.orderservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service.orderservice.dto.ProductDto;
import org.service.orderservice.entity.Order;
import org.service.orderservice.entity.Product;
import org.service.orderservice.event.ProductEvent;
import org.service.orderservice.repository.ProductRepository;
import org.service.orderservice.service.ProductService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    //@KafkaListener(topics = "product-events", groupId = "order-service-group")
    public void listen(ProductEvent productEvent) {
        log.info("Got Message from product-events topic {}", productEvent);

        String action = productEvent.action();

        switch (action) {
            case "CREATE": saveProduct(productEvent);
            break;
            case "DELETE": deleteProduct(productEvent);
            break;
        }
    }

    private void saveProduct(ProductEvent productEvent) {
        Product product = new Product();
        product.setName(productEvent.name());
        product.setDescription(productEvent.description());
        product.setPrice(productEvent.price());
        product.setSkuCode(productEvent.skuCode());

        productRepository.save(product);
    }

    private void deleteProduct(ProductEvent productEvent) {
        Product product = productRepository.findBySkuCode(productEvent.skuCode())
                .orElseThrow(() -> new RuntimeException("Product not found"));


        productRepository.delete(product);
    }
}
