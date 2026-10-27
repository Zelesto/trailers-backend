// src/main/java/com/pgsa/trailers/entity/Sequence.java
package com.pgsa.trailers.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sequence")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, length = 50)
    private String tableName;

    @Column(name = "\"year\"", nullable = false, length = 4)
    private String year;

    @Column(name = "next_number", nullable = false)
    private Long nextNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
