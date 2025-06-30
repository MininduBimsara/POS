package com.pos.repository;

import com.pos.model.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    
    Page<Sale> findByCustomerNameContainingIgnoreCase(String customerName, Pageable pageable);
    
    List<Sale> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT s FROM Sale s WHERE s.totalAmount >= :minAmount")
    List<Sale> findSalesAboveAmount(@Param("minAmount") Double minAmount);
    
    List<Sale> findByPaymentMethod(Sale.PaymentMethod paymentMethod);
    
    List<Sale> findByStatus(Sale.SaleStatus status);
} 