package com.pos.service;

import com.pos.dto.CreateSaleRequest;
import com.pos.dto.SaleDto;
import com.pos.dto.SaleItemDto;
import com.pos.exception.InsufficientStockException;
import com.pos.exception.ResourceNotFoundException;
import com.pos.model.Product;
import com.pos.model.Sale;
import com.pos.model.SaleItem;
import com.pos.repository.ProductRepository;
import com.pos.repository.SaleItemRepository;
import com.pos.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SaleService {
    
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    
    public List<SaleDto> getAllSales() {
        return saleRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Page<SaleDto> getSalesWithPagination(Pageable pageable) {
        return saleRepository.findAll(pageable)
                .map(this::convertToDto);
    }
    
    public SaleDto getSaleById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "id", id));
        return convertToDto(sale);
    }
    
    public List<SaleDto> getSalesByCustomerName(String customerName) {
        return saleRepository.findByCustomerNameContainingIgnoreCase(customerName, Pageable.unpaged())
                .getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<SaleDto> getSalesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return saleRepository.findByCreatedAtBetween(startDate, endDate).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<SaleDto> getSalesByPaymentMethod(Sale.PaymentMethod paymentMethod) {
        return saleRepository.findByPaymentMethod(paymentMethod).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public SaleDto createSale(CreateSaleRequest request) {
        // Create the sale
        Sale sale = new Sale();
        sale.setCustomerName(request.getCustomerName());
        sale.setPaymentMethod(request.getPaymentMethod());
        sale.setStatus(Sale.SaleStatus.COMPLETED);
        
        Sale savedSale = saleRepository.save(sale);
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        // Process each sale item
        for (SaleItemDto itemDto : request.getSaleItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemDto.getProductId()));
            
            // Check stock availability
            if (product.getStockQuantity() < itemDto.getQuantity()) {
                throw new InsufficientStockException(product.getName(), itemDto.getQuantity(), product.getStockQuantity());
            }
            
            // Create sale item
            SaleItem saleItem = new SaleItem();
            saleItem.setSale(savedSale);
            saleItem.setProduct(product);
            saleItem.setQuantity(itemDto.getQuantity());
            saleItem.setUnitPrice(product.getPrice());
            saleItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
            
            saleItemRepository.save(saleItem);
            
            // Update stock
            productService.updateStock(product.getId(), -itemDto.getQuantity());
            
            // Add to total
            totalAmount = totalAmount.add(saleItem.getTotalPrice());
        }
        
        // Update sale total
        savedSale.setTotalAmount(totalAmount);
        saleRepository.save(savedSale);
        
        return convertToDto(savedSale);
    }
    
    public void cancelSale(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "id", id));
        
        if (sale.getStatus() == Sale.SaleStatus.CANCELLED) {
            throw new RuntimeException("Sale is already cancelled");
        }
        
        // Restore stock for each item
        List<SaleItem> saleItems = saleItemRepository.findBySaleId(id);
        for (SaleItem item : saleItems) {
            productService.updateStock(item.getProduct().getId(), item.getQuantity());
        }
        
        sale.setStatus(Sale.SaleStatus.CANCELLED);
        saleRepository.save(sale);
    }
    
    private SaleDto convertToDto(Sale sale) {
        SaleDto dto = new SaleDto();
        dto.setId(sale.getId());
        dto.setCustomerName(sale.getCustomerName());
        dto.setTotalAmount(sale.getTotalAmount());
        dto.setPaymentMethod(sale.getPaymentMethod());
        dto.setStatus(sale.getStatus());
        dto.setCreatedAt(sale.getCreatedAt());
        
        // Get sale items
        List<SaleItem> saleItems = saleItemRepository.findBySaleId(sale.getId());
        List<SaleItemDto> saleItemDtos = saleItems.stream()
                .map(this::convertToSaleItemDto)
                .collect(Collectors.toList());
        dto.setSaleItems(saleItemDtos);
        
        return dto;
    }
    
    private SaleItemDto convertToSaleItemDto(SaleItem saleItem) {
        SaleItemDto dto = new SaleItemDto();
        dto.setId(saleItem.getId());
        dto.setProductId(saleItem.getProduct().getId());
        dto.setProductName(saleItem.getProduct().getName());
        dto.setQuantity(saleItem.getQuantity());
        dto.setUnitPrice(saleItem.getUnitPrice());
        dto.setTotalPrice(saleItem.getTotalPrice());
        return dto;
    }
} 