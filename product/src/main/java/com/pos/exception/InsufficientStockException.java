package com.pos.exception;

public class InsufficientStockException extends RuntimeException {
    
    public InsufficientStockException(String message) {
        super(message);
    }
    
    public InsufficientStockException(String productName, Integer requestedQuantity, Integer availableStock) {
        super(String.format("Insufficient stock for product '%s'. Requested: %d, Available: %d", 
                productName, requestedQuantity, availableStock));
    }
} 
 