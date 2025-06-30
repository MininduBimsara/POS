package com.pos.dto;

import com.pos.model.Sale;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSaleRequest {
    
    private String customerName;
    
    @NotNull(message = "Payment method is required")
    private Sale.PaymentMethod paymentMethod;
    
    @NotEmpty(message = "Sale items cannot be empty")
    @Valid
    private List<SaleItemDto> saleItems;
} 