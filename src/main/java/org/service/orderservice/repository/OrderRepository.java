package org.service.orderservice.repository;

import org.service.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findOrdersByUserId(String userId);

    Optional<Order> findOrderByOrderNumber(String orderNumber);
}
