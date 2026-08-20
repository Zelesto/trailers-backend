// src/main/java/com/pgsa/trailers/service/TripFinalizationStatus.java
package com.pgsa.trailers.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripFinalizationStatus {
    private Long tripId;
    private String tripNumber;
    private String currentStatus;  // Changed from TripStatus to String
    private boolean canBeFinalized;
    private boolean hasPods;
    private long podCount;
    private long invalidPods;
    private String message;
}
