package com.pgsa.trailers.repository.ops;

import com.pgsa.trailers.entity.ops.SowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SowTemplateRepository extends JpaRepository<SowTemplate, Long> {
    List<SowTemplate> findAllByOrderByNameAsc();
    List<SowTemplate> findByIsActiveTrueOrderByNameAsc();
    boolean existsByNameIgnoreCaseAndIsActiveTrue(String name);
}
