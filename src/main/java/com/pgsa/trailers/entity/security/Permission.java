package com.pgsa.trailers.entity.security;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "permission",
        uniqueConstraints = {
                @UniqueConstraint(name = "permission_resource_action_key", columnNames = {"resource", "action"})
        }
)
@Getter
@Setter
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource", nullable = false)
    private String resource;

    @Column(name = "action", nullable = false)
    private String action;
}
