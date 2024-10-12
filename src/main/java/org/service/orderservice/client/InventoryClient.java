package org.service.orderservice.client;

import org.service.orderservice.dto.InStockRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;


public interface InventoryClient {

    @GetExchange("/api/inventory/inStock")
    boolean isInStock(@RequestBody InStockRequest inStockRequest);
}
