package com.procuresense.backend.repository;

import com.procuresense.backend.model.ReorderInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReorderInsightRepository extends JpaRepository<ReorderInsight, Long> {

    Optional<ReorderInsight> findByOrgIdAndSku(String orgId, String sku);
}
