package com.pgsa.trailers.enums;

public enum PaymentStatus {
    CAPTURED,   // Payment recorded but not yet allocated
    ALLOCATED,  // Linked to invoices / costs
    POSTED      // Finalised in account statement
}
