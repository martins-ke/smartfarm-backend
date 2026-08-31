package com.smartfarm.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String category; 
    
    @Column(nullable = false)
    private String unit; 
    
    @Column(nullable = false)
    private BigDecimal quantityInStock;
    
    @Column(nullable = false)
    private BigDecimal unitPrice;
    
    @Column(nullable = false)
    private BigDecimal minStockLevel;
    
    @CreationTimestamp
    private LocalDate addedOn;
    
    @UpdateTimestamp
    private LocalDate lastRestocked;

    public InventoryItem() {}

    public InventoryItem(String id, String name, String category, String unit, BigDecimal quantityInStock, BigDecimal unitPrice, BigDecimal minStockLevel) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.quantityInStock = quantityInStock;
        this.unitPrice = unitPrice;
        this.minStockLevel = minStockLevel;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getQuantityInStock() {
        return quantityInStock;
    }

    public void setQuantityInStock(BigDecimal quantityInStock) {
        this.quantityInStock = quantityInStock;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(BigDecimal minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public LocalDate getAddedOn() {
        return addedOn;
    }

    public void setAddedOn(LocalDate addedOn) {
        this.addedOn = addedOn;
    }

    public LocalDate getLastRestocked() {
        return lastRestocked;
    }

    public void setLastRestocked(LocalDate lastRestocked) {
        this.lastRestocked = lastRestocked;
    }
}
