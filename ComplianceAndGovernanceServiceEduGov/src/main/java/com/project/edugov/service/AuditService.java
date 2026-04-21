package com.project.edugov.service;

import com.project.edugov.model.Audit;
import java.util.List;

public interface AuditService {

    List<Audit> getAllAudits();

    Audit getAuditById(Long id);

    Audit createAudit(Audit audit, Long creatorId);

    Audit updateAudit(Long id, Audit auditDetails);

    void deleteAudit(Long id);

    Audit reviewAudit(Long auditId, String status, String findings, Long auditorId);
}

