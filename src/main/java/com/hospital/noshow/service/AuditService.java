package com.hospital.noshow.service;

public interface AuditService {

    // Signature per §3/§14: writeLog(entityType, entityId, old, new, changedBy)
    void writeLog(String entityType, Long entityId, String oldStatus, String newStatus, String changedBy);
}