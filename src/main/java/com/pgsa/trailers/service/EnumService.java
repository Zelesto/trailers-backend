// src/main/java/com/pgsa/trailers/service/EnumService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.system.EnumMaster;
import com.pgsa.trailers.repository.EnumMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnumService {

    private final EnumMasterRepository enumRepository;

    // ============================================================
    // READ OPERATIONS
    // ============================================================

    @Cacheable(value = "enums", key = "#moduleName + ':' + #category + ':' + (#includeInactive ? 'all' : 'active')")
    @Transactional(readOnly = true)
    public List<EnumMaster> getEnums(String moduleName, String category, Boolean includeInactive) {
        log.info("Fetching enums for module: {}, category: {}, includeInactive: {}", moduleName, category, includeInactive);
        
        if (Boolean.TRUE.equals(includeInactive)) {
            return enumRepository.findByModuleNameAndCategoryOrderBySortOrder(moduleName, category);
        }
        return enumRepository.findByModuleNameAndCategoryAndIsActiveTrueOrderBySortOrder(moduleName, category);
    }

    @Cacheable(value = "enums", key = "#moduleName + ':' + #category + ':system'")
    @Transactional(readOnly = true)
    public List<EnumMaster> getSystemEnums(String moduleName, String category) {
        return enumRepository.findByModuleNameAndCategoryAndIsSystemTrueAndIsActiveTrueOrderBySortOrder(
            moduleName, category
        );
    }

    @Cacheable(value = "enums", key = "#moduleName + ':' + #category + ':custom'")
    @Transactional(readOnly = true)
    public List<EnumMaster> getCustomEnums(String moduleName, String category) {
        return enumRepository.findByModuleNameAndCategoryAndIsSystemFalseAndIsActiveTrueOrderBySortOrder(
            moduleName, category
        );
    }

    @Cacheable(value = "enums", key = "#moduleName + ':ALL'")
    @Transactional(readOnly = true)
    public Map<String, List<EnumMaster>> getEnumsByModule(String moduleName) {
        List<EnumMaster> enums = enumRepository.findByModuleNameAndIsActiveTrue(moduleName);
        return enums.stream().collect(Collectors.groupingBy(EnumMaster::getCategory));
    }

    @Transactional(readOnly = true)
    public EnumMaster getEnumById(Long id) {
        return enumRepository.findById(id).orElse(null);
    }

    @Cacheable(value = "enumCodes", key = "#moduleName + ':' + #category + ':' + #code")
    @Transactional(readOnly = true)
    public EnumMaster getEnum(String moduleName, String category, String code) {
        return enumRepository.findByModuleNameAndCategoryAndCode(moduleName, category, code)
            .orElse(null);
    }

    @Cacheable(value = "enumDefaults", key = "#moduleName + ':' + #category")
    @Transactional(readOnly = true)
    public EnumMaster getDefaultEnum(String moduleName, String category) {
        return enumRepository.findByModuleNameAndCategoryAndIsDefaultTrue(moduleName, category)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<String> getEnumTypes() {
        return enumRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public List<String> getModules() {
        return enumRepository.findDistinctModules();
    }

    // ============================================================
    // CREATE OPERATIONS
    // ============================================================

    @Transactional
    @CacheEvict(value = {"enums", "enumCodes", "enumDefaults"}, allEntries = true)
    public EnumMaster createEnum(EnumMaster enumMaster) {
        // Validate
        if (enumMaster.getIsSystem() != null && enumMaster.getIsSystem()) {
            throw new RuntimeException("Cannot create system enums manually");
        }

        // Check for duplicates
        Optional<EnumMaster> existing = enumRepository.findByModuleNameAndCategoryAndCode(
            enumMaster.getModuleName(),
            enumMaster.getCategory(),
            enumMaster.getCode()
        );
        
        if (existing.isPresent()) {
            throw new RuntimeException("Enum already exists: " + enumMaster.getCode());
        }

        // Set defaults
        if (enumMaster.getSortOrder() == null) enumMaster.setSortOrder(0);
        if (enumMaster.getIsDefault() == null) enumMaster.setIsDefault(false);
        if (enumMaster.getIsActive() == null) enumMaster.setIsActive(true);
        if (enumMaster.getIsSystem() == null) enumMaster.setIsSystem(false);
        if (enumMaster.getIsEditable() == null) enumMaster.setIsEditable(true);
        if (enumMaster.getIsDeletable() == null) enumMaster.setIsDeletable(true);

        enumMaster.setCreatedAt(LocalDateTime.now());
        enumMaster.setUpdatedAt(LocalDateTime.now());

        log.info("Creating custom enum: {} - {} - {}", enumMaster.getModuleName(), enumMaster.getCategory(), enumMaster.getCode());
        return enumRepository.save(enumMaster);
    }

    // ============================================================
    // UPDATE OPERATIONS
    // ============================================================

    @Transactional
    @CacheEvict(value = {"enums", "enumCodes", "enumDefaults"}, allEntries = true)
    public EnumMaster updateEnum(Long id, EnumMaster enumMaster) {
        EnumMaster existing = enumRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Enum not found with ID: " + id));
        
        // Update fields
        if (enumMaster.getDisplayName() != null) {
            existing.setDisplayName(enumMaster.getDisplayName());
        }
        if (enumMaster.getDescription() != null) {
            existing.setDescription(enumMaster.getDescription());
        }
        if (enumMaster.getSortOrder() != null) {
            existing.setSortOrder(enumMaster.getSortOrder());
        }
        if (enumMaster.getIsDefault() != null) {
            existing.setIsDefault(enumMaster.getIsDefault());
        }
        if (enumMaster.getIsActive() != null) {
            existing.setIsActive(enumMaster.getIsActive());
        }
        if (enumMaster.getColorCode() != null) {
            existing.setColorCode(enumMaster.getColorCode());
        }
        if (enumMaster.getIconName() != null) {
            existing.setIconName(enumMaster.getIconName());
        }
        if (enumMaster.getMetadata() != null) {
            existing.setMetadata(enumMaster.getMetadata());
        }
        if (enumMaster.getUpdatedBy() != null) {
            existing.setUpdatedBy(enumMaster.getUpdatedBy());
        }
        
        existing.setUpdatedAt(LocalDateTime.now());

        log.info("Updating enum: {} - {}", existing.getModuleName(), existing.getCode());
        return enumRepository.save(existing);
    }

    // ============================================================
    // DELETE OPERATIONS
    // ============================================================

    @Transactional
    @CacheEvict(value = {"enums", "enumCodes", "enumDefaults"}, allEntries = true)
    public void deleteEnum(Long id, String updatedBy) {
        EnumMaster enumMaster = enumRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Enum not found with ID: " + id));
        
        if (enumMaster.getIsSystem() && !enumMaster.getIsDeletable()) {
            throw new RuntimeException("System enums cannot be deleted");
        }
        
        // Soft delete - just deactivate
        enumMaster.setIsActive(false);
        enumMaster.setUpdatedBy(updatedBy);
        enumMaster.setUpdatedAt(LocalDateTime.now());
        
        enumRepository.save(enumMaster);
        log.info("Soft deleted enum: {} - {}", enumMaster.getModuleName(), enumMaster.getCode());
    }

    @Transactional
    @CacheEvict(value = {"enums", "enumCodes", "enumDefaults"}, allEntries = true)
    public EnumMaster toggleEnumStatus(Long id, String updatedBy) {
        EnumMaster enumMaster = enumRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Enum not found with ID: " + id));
        
        if (enumMaster.getIsSystem() && !enumMaster.getIsEditable()) {
            throw new RuntimeException("Cannot modify system enum status");
        }
        
        enumMaster.setIsActive(!enumMaster.getIsActive());
        enumMaster.setUpdatedBy(updatedBy);
        enumMaster.setUpdatedAt(LocalDateTime.now());
        
        log.info("Toggled enum status: {} - {} -> active: {}", 
            enumMaster.getModuleName(), enumMaster.getCode(), enumMaster.getIsActive());
        
        return enumRepository.save(enumMaster);
    }
}
