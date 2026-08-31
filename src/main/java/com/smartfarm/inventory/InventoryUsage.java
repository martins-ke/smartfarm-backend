package com.smartfarm.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.smartfarm.projects.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_usages")
public class InventoryUsage {

    @Id
    private String id;
    
    @ManyToOne(optional = false)
    private InventoryItem inventoryItem;
    
    @ManyToOne(optional = false)
    private Project project;
    
    @Column(nullable = false)
    private BigDecimal quantityUsed;
    
    @CreationTimestamp
    private LocalDate usageDate;
    
    private String notes;

    public InventoryUsage() {}

    public InventoryUsage(String id, InventoryItem inventoryItem, Project project, BigDecimal quantityUsed, String notes) {
        this.id = id;
        this.inventoryItem = inventoryItem;
        this.project = project;
        this.quantityUsed = quantityUsed;
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public BigDecimal getQuantityUsed() {
        return quantityUsed;
    }

    public void setQuantityUsed(BigDecimal quantityUsed) {
        this.quantityUsed = quantityUsed;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public void setUsageDate(LocalDate usageDate) {
        this.usageDate = usageDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
