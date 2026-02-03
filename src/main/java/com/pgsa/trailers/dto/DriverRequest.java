package com.pgsa.trailers.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DriverRequest {
    private String firstName;
    private String lastName;
    private String licenseNumber;
    private LocalDate licenseExpiry;
    private String phoneNumber;
    private String email;
    private String status;
    private LocalDate hireDate;
    private String licenseType;

    // Needed for AppUser creation
    private String password;
}
