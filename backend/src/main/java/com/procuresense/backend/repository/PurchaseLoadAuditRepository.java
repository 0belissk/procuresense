package com.procuresense.backend.repository;

import com.procuresense.backend.model.PurchaseLoadAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseLoadAuditRepository extends JpaRepository<PurchaseLoadAudit, String> {
}
