// src/main/java/com/pgsa/trailers/dto/inventory/InventoryLocationRequestDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLocationRequestDTO {
    private String name;
    private String type;
}
