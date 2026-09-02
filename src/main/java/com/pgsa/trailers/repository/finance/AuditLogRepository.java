package com.pgsa.trailers.repository.finance;

import com.pgsa.trailers.entity.finance.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
    List<AuditLog> findByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM AuditLog a WHERE a.module = :module ORDER BY a.createdAt DESC")
    List<AuditLog> findByModule(@Param("module") String module);

    @Query("SELECT a FROM AuditLog a WHERE a.actionType = :actionType ORDER BY a.createdAt DESC")
    List<AuditLog> findByActionType(@Param("actionType") String actionType);

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId")
    List<AuditLog> findByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<AuditLog> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT a FROM AuditLog a WHERE a.username LIKE %:username% ORDER BY a.createdAt DESC")
    List<AuditLog> findByUsernameContaining(@Param("username") String username);

    @Query("SELECT a FROM AuditLog a WHERE a.errorMessage IS NOT NULL ORDER BY a.createdAt DESC")
    List<AuditLog> findErrorLogs();

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.actionType = :actionType AND a.module = :module")
    long countByActionTypeAndModule(
            @Param("actionType") String actionType,
            @Param("module") String module
    );

    @Query("SELECT a FROM AuditLog a WHERE a.auditUuid = :auditUuid")
    AuditLog findByAuditUuid(@Param("auditUuid") String auditUuid);
}
