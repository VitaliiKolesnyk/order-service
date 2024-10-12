package org.service.orderservice.dto;

public record ContactDetailsResponse(String name, String surname,
                                     String country, String city, String street) {
}
