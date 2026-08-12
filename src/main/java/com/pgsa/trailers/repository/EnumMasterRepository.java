// src/main/java/com/pgsa/trailers/repository/EnumMasterRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.system.EnumMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnumMasterRepository extends JpaRepository<EnumMaster, Long> {

    // Standard queries
    List<EnumMaster> findByModuleNameAndCategoryAndIsActiveTrueOrderBySortOrder(
        String moduleName, String category
    );

    List<EnumMaster> findByModuleNameAndCategoryOrderBySortOrder(
        String moduleName, String category
    );

    List<EnumMaster> findByModuleNameAndIsActiveTrue(String moduleName);

    Optional<EnumMaster> findByModuleNameAndCategoryAndCode(
        String moduleName, String category, String code
    );

    Optional<EnumMaster> findByModuleNameAndCategoryAndIsDefaultTrue(
        String moduleName, String category
    );

    // System enum queries
    List<EnumMaster> findByModuleNameAndCategoryAndIsSystemTrueAndIsActiveTrueOrderBySortOrder(
        String moduleName, String category
    );

    // Custom enum queries
    List<EnumMaster> findByModuleNameAndCategoryAndIsSystemFalseAndIsActiveTrueOrderBySortOrder(
        String moduleName, String category
    );

    // Admin queries
    List<EnumMaster> findByModuleName(String moduleName);

    boolean existsByModuleNameAndCategoryAndCode(
        String moduleName, String category, String code
    );

    @Query("SELECT DISTINCT e.category FROM EnumMaster e")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT e.moduleName FROM EnumMaster e")
    List<String> findDistinctModules();
}
