package com.procuresense.backend.repository;

import com.procuresense.backend.model.BundleInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BundleInsightRepository extends JpaRepository<BundleInsight, Long> {

    Optional<BundleInsight> findByOrgIdAndSkuAndRelatedSku(String orgId, String sku, String relatedSku);
}
