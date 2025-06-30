package com.pos.controller;

import com.pos.dto.CreateSaleRequest;
import com.pos.dto.SaleDto;
import com.pos.model.Sale;
import com.pos.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SaleController {
    
    private final SaleService saleService;
    
    @GetMapping
    public ResponseEntity<Page<SaleDto>> getAllSales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SaleDto> sales = saleService.getSalesWithPagination(pageable);
        return ResponseEntity.ok(sales);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SaleDto> getSaleById(@PathVariable Long id) {
        SaleDto sale = saleService.getSaleById(id);
        return ResponseEntity.ok(sale);
    }
    
    @GetMapping("/customer")
    public ResponseEntity<List<SaleDto>> getSalesByCustomerName(@RequestParam String customerName) {
        List<SaleDto> sales = saleService.getSalesByCustomerName(customerName);
        return ResponseEntity.ok(sales);
    }
    
    @GetMapping("/date-range")
    public ResponseEntity<List<SaleDto>> getSalesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<SaleDto> sales = saleService.getSalesByDateRange(startDate, endDate);
        return ResponseEntity.ok(sales);
    }
    
    @GetMapping("/payment-method")
    public ResponseEntity<List<SaleDto>> getSalesByPaymentMethod(@RequestParam Sale.PaymentMethod paymentMethod) {
        List<SaleDto> sales = saleService.getSalesByPaymentMethod(paymentMethod);
        return ResponseEntity.ok(sales);
    }
    
    @PostMapping
    public ResponseEntity<SaleDto> createSale(@Valid @RequestBody CreateSaleRequest request) {
        SaleDto createdSale = saleService.createSale(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSale);
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelSale(@PathVariable Long id) {
        saleService.cancelSale(id);
        return ResponseEntity.ok().build();
    }
} 