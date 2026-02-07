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
@Table(name = "reorder_insights")
public class ReorderInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false, length = 64)
    private String orgId;

    @Column(name = "sku", nullable = false, length = 128)
    private String sku;

    @Column(name = "last_purchase_at", nullable = false)
    private OffsetDateTime lastPurchaseAt;

    @Column(name = "predicted_reorder_at", nullable = false)
    private OffsetDateTime predictedReorderAt;

    @Column(name = "median_days_between", nullable = false)
    private long medianDaysBetween;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "explanation_text")
    private String explanationText;

    @Column(name = "fingerprint", nullable = false, length = 255)
    private String fingerprint;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void stampUpdatedAt() {
        updatedAt = OffsetDateTime.now();
    }

    // getters and setters

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

    public OffsetDateTime getLastPurchaseAt() {
        return lastPurchaseAt;
    }

    public void setLastPurchaseAt(OffsetDateTime lastPurchaseAt) {
        this.lastPurchaseAt = lastPurchaseAt;
    }

    public OffsetDateTime getPredictedReorderAt() {
        return predictedReorderAt;
    }

    public void setPredictedReorderAt(OffsetDateTime predictedReorderAt) {
        this.predictedReorderAt = predictedReorderAt;
    }

    public long getMedianDaysBetween() {
        return medianDaysBetween;
    }

    public void setMedianDaysBetween(long medianDaysBetween) {
        this.medianDaysBetween = medianDaysBetween;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getExplanationText() {
        return explanationText;
    }

    public void setExplanationText(String explanationText) {
        this.explanationText = explanationText;
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
