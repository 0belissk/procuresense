package com.procuresense.backend.repository;

import com.procuresense.backend.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    @Query("select coalesce(count(distinct p.orderId), 0) from Purchase p where p.orgId = :orgId")
    long countDistinctOrdersByOrg(@Param("orgId") String orgId);

    long countByOrgId(String orgId);

    @Query("select coalesce(sum(p.quantity), 0) from Purchase p where p.orgId = :orgId")
    long sumQuantitiesByOrg(@Param("orgId") String orgId);

    @Query("select coalesce(sum(p.quantity * p.unitPrice), 0) from Purchase p where p.orgId = :orgId")
    BigDecimal sumRevenueByOrg(@Param("orgId") String orgId);

    @Modifying
    @Query("delete from Purchase p where p.orgId = :orgId")
    void deleteByOrgId(@Param("orgId") String orgId);

    @Query("select coalesce(count(distinct p.product.sku), 0) from Purchase p where p.orgId = :orgId")
    long countDistinctSkusByOrg(@Param("orgId") String orgId);

    @Query("select min(p.purchasedAt) from Purchase p where p.orgId = :orgId")
    OffsetDateTime findFirstPurchaseDate(@Param("orgId") String orgId);

    @Query("select max(p.purchasedAt) from Purchase p where p.orgId = :orgId")
    OffsetDateTime findLastPurchaseDate(@Param("orgId") String orgId);
}
