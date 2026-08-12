// src/main/java/com/pgsa/trailers/config/EnumDataInitializer.java
package com.pgsa.trailers.config;

import com.pgsa.trailers.entity.system.EnumMaster;
import com.pgsa.trailers.repository.EnumMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnumDataInitializer implements ApplicationRunner {

    private final EnumMasterRepository enumRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Only seed if tables are empty
        if (enumRepository.count() == 0) {
            log.info("Seeding initial enum data...");
            seedEnums();
        } else {
            log.info("Enum data already exists, skipping seed.");
        }
    }

    private void seedEnums() {
        // Trip Statuses
        createEnum("trip", "status", "DRAFT", "Draft", "Initial draft state", 1, false, true, true, false, false, "#9E9E9E", "Draft");
        createEnum("trip", "status", "PLANNED", "Planned", "Trip has been planned", 2, true, true, true, false, false, "#2196F3", "Planned");
        createEnum("trip", "status", "ASSIGNED", "Assigned", "Driver and vehicle assigned", 3, false, true, true, false, false, "#FF9800", "Assigned");
        createEnum("trip", "status", "IN_PROGRESS", "In Progress", "Trip is in progress", 4, false, true, true, false, false, "#4CAF50", "InProgress");
        createEnum("trip", "status", "ON_HOLD", "On Hold", "Trip is temporarily paused", 5, false, true, true, false, false, "#FF5722", "OnHold");
        createEnum("trip", "status", "COMPLETED", "Completed", "Trip has been completed", 6, false, true, true, false, false, "#8BC34A", "Completed");
        createEnum("trip", "status", "FINALIZED", "Finalized", "Trip has been finalized", 7, false, true, true, false, false, "#009688", "Finalized");
        createEnum("trip", "status", "CANCELLED", "Cancelled", "Trip has been cancelled", 8, false, true, true, false, false, "#F44336", "Cancelled");

        // Trip Approval Statuses
        createEnum("trip", "approval", "PENDING", "Pending", "Awaiting approval", 1, true, true, true, false, false, "#FF9800", "Pending");
        createEnum("trip", "approval", "APPROVED", "Approved", "Trip has been approved", 2, false, true, true, false, false, "#4CAF50", "Approved");
        createEnum("trip", "approval", "REJECTED", "Rejected", "Trip has been rejected", 3, false, true, true, false, false, "#F44336", "Rejected");
        createEnum("trip", "approval", "UNDER_REVIEW", "Under Review", "Trip is under review", 4, false, true, true, false, false, "#2196F3", "Review");

        // Trip Types
        createEnum("trip", "type", "FREIGHT", "Freight", "Standard freight trip", 1, true, true, true, false, false, "#4CAF50", null);
        createEnum("trip", "type", "RETURN", "Return", "Return trip", 2, false, true, true, false, false, "#FF9800", null);
        createEnum("trip", "type", "EMPTY", "Empty", "Empty trip (no cargo)", 3, false, true, true, false, false, "#9E9E9E", null);
        createEnum("trip", "type", "MAINTENANCE", "Maintenance", "Maintenance trip", 4, false, true, true, false, false, "#F44336", null);

        // Load Statuses
        createEnum("load", "status", "PENDING", "Pending", "Load is pending", 1, true, true, true, false, false, "#FF9800", null);
        createEnum("load", "status", "PLANNED", "Planned", "Load has been planned", 2, false, true, true, false, false, "#2196F3", null);
        createEnum("load", "status", "IN_TRANSIT", "In Transit", "Load is in transit", 3, false, true, true, false, false, "#4CAF50", null);
        createEnum("load", "status", "DELIVERED", "Delivered", "Load has been delivered", 4, false, true, true, false, false, "#8BC34A", null);
        createEnum("load", "status", "COMPLETED", "Completed", "Load has been completed", 5, false, true, true, false, false, "#009688", null);
        createEnum("load", "status", "CANCELLED", "Cancelled", "Load has been cancelled", 6, false, true, true, false, false, "#F44336", null);

        // Driver Statuses
        createEnum("driver", "status", "AVAILABLE", "Available", "Driver is available", 1, true, true, true, false, false, "#4CAF50", null);
        createEnum("driver", "status", "ASSIGNED", "Assigned", "Driver is assigned to trip", 2, false, true, true, false, false, "#FF9800", null);
        createEnum("driver", "status", "ON_TRIP", "On Trip", "Driver is on a trip", 3, false, true, true, false, false, "#2196F3", null);
        createEnum("driver", "status", "ON_LEAVE", "On Leave", "Driver is on leave", 4, false, true, true, false, false, "#9E9E9E", null);
        createEnum("driver", "status", "SUSPENDED", "Suspended", "Driver is suspended", 5, false, true, true, false, false, "#F44336", null);
        createEnum("driver", "status", "INACTIVE", "Inactive", "Driver is inactive", 6, false, true, true, false, false, "#9E9E9E", null);

        // Vehicle Statuses
        createEnum("vehicle", "status", "AVAILABLE", "Available", "Vehicle is available", 1, true, true, true, false, false, "#4CAF50", null);
        createEnum("vehicle", "status", "ASSIGNED", "Assigned", "Vehicle is assigned", 2, false, true, true, false, false, "#FF9800", null);
        createEnum("vehicle", "status", "IN_TRIP", "In Trip", "Vehicle is on a trip", 3, false, true, true, false, false, "#2196F3", null);
        createEnum("vehicle", "status", "MAINTENANCE", "Maintenance", "Vehicle is in maintenance", 4, false, true, true, false, false, "#FF5722", null);
        createEnum("vehicle", "status", "OUT_OF_SERVICE", "Out of Service", "Vehicle is out of service", 5, false, true, true, false, false, "#F44336", null);

        // POD Statuses
        createEnum("pod", "status", "SCANNED", "Scanned", "POD has been scanned", 1, false, true, true, false, false, "#2196F3", null);
        createEnum("pod", "status", "PENDING", "Pending", "POD is pending", 2, true, true, true, false, false, "#FF9800", null);
        createEnum("pod", "status", "DELIVERED", "Delivered", "POD has been delivered", 3, false, true, true, false, false, "#4CAF50", null);
        createEnum("pod", "status", "VERIFIED", "Verified", "POD has been verified", 4, false, true, true, false, false, "#009688", null);
        createEnum("pod", "status", "REJECTED", "Rejected", "POD has been rejected", 5, false, true, true, false, false, "#F44336", null);

        log.info("Enum data seeding completed.");
    }

    private void createEnum(String module, String category, String code, String displayName, 
                           String description, int sortOrder, boolean isDefault, 
                           boolean isActive, boolean isSystem, boolean isEditable, 
                           boolean isDeletable, String colorCode, String iconName) {
        EnumMaster enumMaster = EnumMaster.builder()
            .moduleName(module)
            .category(category)
            .code(code)
            .displayName(displayName)
            .description(description)
            .sortOrder(sortOrder)
            .isDefault(isDefault)
            .isActive(isActive)
            .isSystem(isSystem)
            .isEditable(isEditable)
            .isDeletable(isDeletable)
            .colorCode(colorCode)
            .iconName(iconName)
            .build();
        
        enumRepository.save(enumMaster);
    }
}
