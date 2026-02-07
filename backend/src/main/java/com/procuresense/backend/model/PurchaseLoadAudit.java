package com.procuresense.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "purchase_load_audit")
public class PurchaseLoadAudit {

    @Id
    @Column(name = "org_id", nullable = false, length = 100)
    private String orgId;

    @Column(name = "last_loaded_at", nullable = false)
    private OffsetDateTime lastLoadedAt;

    public PurchaseLoadAudit() {
    }

    public PurchaseLoadAudit(String orgId, OffsetDateTime lastLoadedAt) {
        this.orgId = orgId;
        this.lastLoadedAt = lastLoadedAt;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public OffsetDateTime getLastLoadedAt() {
        return lastLoadedAt;
    }

    public void setLastLoadedAt(OffsetDateTime lastLoadedAt) {
        this.lastLoadedAt = lastLoadedAt;
    }
}
