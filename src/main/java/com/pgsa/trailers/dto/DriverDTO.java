package com.pgsa.trailers.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DriverDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String licenseNumber;
    private LocalDate licenseExpiry;
    private String phoneNumber;
    private String email;
    private String status;
    private LocalDate hireDate;
    private String licenseType;

    // Linked AppUser account
    private AppUserDTO appUser;

    // Add password here
    private String password;
}
