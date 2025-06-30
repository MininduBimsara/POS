package com.pos.dto;

import com.pos.model.Sale;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleDto {
    
    private Long id;
    private String customerName;
    private BigDecimal totalAmount;
    private Sale.PaymentMethod paymentMethod;
    private Sale.SaleStatus status;
    private LocalDateTime createdAt;
    private List<SaleItemDto> saleItems;
} 