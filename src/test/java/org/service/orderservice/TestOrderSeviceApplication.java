package org.service.orderservice;

import org.springframework.boot.SpringApplication;

public class TestOrderSeviceApplication {

    public static void main(String[] args) {
        SpringApplication.from(OrderServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
