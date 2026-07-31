package com.example.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Portfolio {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PortfolioItem> items = new ArrayList<>();

    // Constructors
    public Portfolio() {
    }

    public Portfolio(Long id, String name, String description, LocalDateTime createdAt, LocalDateTime updatedAt, List<PortfolioItem> items) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = items;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<PortfolioItem> getItems() { return items; }
    public void setItems(List<PortfolioItem> items) { this.items = items; }

    // Helper methods
    public void addItem(PortfolioItem item) {
        items.add(item);
        item.setPortfolio(this);
    }

    public void removeItem(PortfolioItem item) {
        items.remove(item);
        item.setPortfolio(null);
    }
}