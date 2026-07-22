package com.hospital.noshow.service;

import com.hospital.noshow.entity.AuditLog;
import com.hospital.noshow.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void writeLog(String entityType, Long entityId, String oldStatus, String newStatus, String changedBy) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setOldStatus(oldStatus);
        auditLog.setNewStatus(newStatus);
        auditLog.setChangedBy(changedBy);
        // changedAt is set by AuditLog's @PrePersist — not set here
        // remarks is not part of this signature — left null
        auditLogRepository.save(auditLog);
    }
}