package org.service.orderservice.service;

import org.service.orderservice.event.ProductEvent;

public interface ProductService {

    void listen(ProductEvent productEvent);
}
