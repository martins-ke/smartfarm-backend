package com.smartfarm.harvest;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "harvest_inventory",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_name", "item_name"})
)
public class HarvestInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_name", nullable = false)
    @JsonProperty("item_name")
    private String itemName;

    @Column(name = "available_quantity", nullable = false)
    @JsonProperty("available_quantity")
    private float availableQuantity;

    private String units;

    @Column(name = "project_name", nullable = false)
    @JsonProperty("projectName")
    private String projectName;

    /** The family base unit quantities are stored in (e.g. "kg", "piece", "litre") */
    @Column(name = "base_unit")
    @JsonProperty("base_unit")
    private String baseUnit;

    /** The unit the user selected when recording harvest (e.g. "bag90", "tray") */
    @Column(name = "display_unit")
    @JsonProperty("display_unit")
    private String displayUnit;

    public HarvestInventory() {}

    public HarvestInventory(String itemName, float availableQuantity, String units, String projectName, String baseUnit, String displayUnit) {
        this.itemName = itemName;
        this.availableQuantity = availableQuantity;
        this.units = units;
        this.projectName = projectName;
        this.baseUnit = baseUnit;
        this.displayUnit = displayUnit;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @JsonProperty("item_name")
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    @JsonProperty("available_quantity")
    public float getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(float availableQuantity) { this.availableQuantity = availableQuantity; }

    public String getUnits() { return units; }
    public void setUnits(String units) { this.units = units; }

    @JsonProperty("projectName")
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    @JsonProperty("base_unit")
    public String getBaseUnit() { return baseUnit; }
    public void setBaseUnit(String baseUnit) { this.baseUnit = baseUnit; }

    @JsonProperty("display_unit")
    public String getDisplayUnit() { return displayUnit; }
    public void setDisplayUnit(String displayUnit) { this.displayUnit = displayUnit; }
}
