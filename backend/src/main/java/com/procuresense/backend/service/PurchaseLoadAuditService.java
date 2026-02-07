package com.procuresense.backend.service;

import com.procuresense.backend.model.PurchaseLoadAudit;
import com.procuresense.backend.repository.PurchaseLoadAuditRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class PurchaseLoadAuditService {

    private final PurchaseLoadAuditRepository repository;

    public PurchaseLoadAuditService(PurchaseLoadAuditRepository repository) {
        this.repository = repository;
    }

    public void markLoaded(String orgId) {
        if (orgId == null || orgId.isBlank()) {
            return;
        }
        PurchaseLoadAudit audit = repository.findById(orgId)
                .orElseGet(() -> new PurchaseLoadAudit(orgId, OffsetDateTime.now()));
        audit.setLastLoadedAt(OffsetDateTime.now());
        repository.save(audit);
    }

    public OffsetDateTime getLastLoadedAt(String orgId) {
        if (orgId == null || orgId.isBlank()) {
            return null;
        }
        return repository.findById(orgId)
                .map(PurchaseLoadAudit::getLastLoadedAt)
                .orElse(null);
    }
}
