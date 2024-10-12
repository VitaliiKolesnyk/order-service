package org.service.orderservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.service.orderservice.dto.Error;

@ControllerAdvice
public class OrderControllerAdvice {

    @ExceptionHandler(NotInStockException.class)
    public ResponseEntity<Error> catchNotInStockException(NotInStockException e) {
        Error error = new Error(e.getMessage());

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
