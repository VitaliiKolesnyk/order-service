package org.service.orderservice.dto;

public record ContactDetailsRequest(String name, String surname,
                                    String country, String city, String street) {
}
