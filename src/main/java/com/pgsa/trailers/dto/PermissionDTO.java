package com.pgsa.trailers.dto;

import lombok.Data;

@Data
public class PermissionDTO {
    private Long id;
    private String resource;
    private String action;
}
