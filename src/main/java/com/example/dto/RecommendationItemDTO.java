package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single actionable recommendation for a portfolio")
public class RecommendationItemDTO {
    private String priority;
    private String category;
    private String title;
    private String message;

    public RecommendationItemDTO() {
    }

    public RecommendationItemDTO(String priority, String category, String title, String message) {
        this.priority = priority;
        this.category = category;
        this.title = title;
        this.message = message;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}


