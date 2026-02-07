package com.procuresense.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "bundle_insights")
public class BundleInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false, length = 64)
    private String orgId;

    @Column(name = "sku", nullable = false, length = 128)
    private String sku;

    @Column(name = "related_sku", nullable = false, length = 128)
    private String relatedSku;

    @Column(name = "co_purchase_count", nullable = false)
    private long coPurchaseCount;

    @Column(name = "rationale_text")
    private String rationaleText;

    @Column(name = "fingerprint", nullable = false, length = 255)
    private String fingerprint;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getRelatedSku() {
        return relatedSku;
    }

    public void setRelatedSku(String relatedSku) {
        this.relatedSku = relatedSku;
    }

    public long getCoPurchaseCount() {
        return coPurchaseCount;
    }

    public void setCoPurchaseCount(long coPurchaseCount) {
        this.coPurchaseCount = coPurchaseCount;
    }

    public String getRationaleText() {
        return rationaleText;
    }

    public void setRationaleText(String rationaleText) {
        this.rationaleText = rationaleText;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
