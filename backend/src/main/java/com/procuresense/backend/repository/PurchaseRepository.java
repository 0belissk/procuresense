package com.procuresense.backend.repository;

import com.procuresense.backend.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    @Query("select coalesce(count(distinct p.orderId), 0) from Purchase p")
    long countDistinctOrders();

    @Query("select coalesce(sum(p.quantity), 0) from Purchase p")
    long sumQuantities();

    @Query("select coalesce(sum(p.quantity * p.unitPrice), 0) from Purchase p")
    BigDecimal sumRevenue();
}
