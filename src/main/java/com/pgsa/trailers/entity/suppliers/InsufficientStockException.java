// src/main/java/com/pgsa/trailers/exception/InsufficientStockException.java
package com.pgsa.trailers.entity;

public class InsufficientStockException extends RuntimeException {
    private final Long itemId;
    private final String itemName;
    private final int availableQuantity;
    private final int requestedQuantity;

    public InsufficientStockException(String message, Long itemId, String itemName, 
                                       int availableQuantity, int requestedQuantity) {
        super(message);
        this.itemId = itemId;
        this.itemName = itemName;
        this.availableQuantity = availableQuantity;
        this.requestedQuantity = requestedQuantity;
    }

    public InsufficientStockException(String message) {
        super(message);
        this.itemId = null;
        this.itemName = null;
        this.availableQuantity = 0;
        this.requestedQuantity = 0;
    }

    // Getters
    public Long getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public int getAvailableQuantity() { return availableQuantity; }
    public int getRequestedQuantity() { return requestedQuantity; }
}
