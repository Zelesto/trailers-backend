package com.pgsa.trailers.service.finance;

import com.pgsa.trailers.entity.finance.AuditLog;
import com.pgsa.trailers.repository.finance.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AuditLog logAction(AuditLog auditLog) {
        if (auditLog.getAuditUuid() == null) {
            auditLog.setAuditUuid(UUID.randomUUID().toString());
        }
        auditLog.setCreatedAt(LocalDateTime.now());
        return auditLogRepository.save(auditLog);
    }

    @Transactional
    public AuditLog logAction(
            Long userId,
            String username,
            String actionType,
            String module,
            String entityType,
            Long entityId,
            String entityReference,
            Object oldValue,
            Object newValue,
            String ipAddress,
            String userAgent,
            String sessionId
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setActionType(actionType);
        auditLog.setModule(module);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setEntityReference(entityReference);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setSessionId(sessionId);
        
        try {
            if (oldValue != null) {
                auditLog.setOldValue(objectMapper.writeValueAsString(oldValue));
            }
            if (newValue != null) {
                auditLog.setNewValue(objectMapper.writeValueAsString(newValue));
            }
        } catch (Exception e) {
            log.warn("Failed to serialize audit values: {}", e.getMessage());
        }
        
        return logAction(auditLog);
    }

    @Transactional
    public AuditLog logError(
            Long userId,
            String username,
            String module,
            String action,
            String errorMessage,
            String stackTrace,
            String ipAddress,
            String userAgent
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setActionType("ERROR");
        auditLog.setModule(module);
        auditLog.setEntityType(action);
        auditLog.setErrorCode(500);
        auditLog.setErrorMessage(errorMessage);
        auditLog.setStackTrace(stackTrace);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        
        return logAction(auditLog);
    }
}
