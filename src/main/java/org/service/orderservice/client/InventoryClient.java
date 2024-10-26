package org.service.orderservice.client;

import brave.Response;
import org.service.orderservice.dto.ReserveProductsRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;


public interface InventoryClient {

    @PostExchange("/api/inventory/reserveProducts")
    ResponseEntity<Boolean> reserveProducts(@RequestBody ReserveProductsRequest reserveProductsRequest);
}
