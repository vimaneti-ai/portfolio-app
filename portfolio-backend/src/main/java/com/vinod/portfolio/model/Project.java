package com.vinod.portfolio.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a portfolio project. Maps to the projects table in MySQL.
 * The Angular app fetches these instead of hardcoding them in the HTML.
 */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 100)
    private String kicker;

    @Column(nullable = false, length = 1500)
    private String description;

    @Column(length = 200)
    private String metric;

    // Used by the frontend filter: frontend / backend / fullstack
    @Column(nullable = false, length = 50)
    private String category;

    // Comma-separated tags, e.g. "Angular,TypeScript,SQL"
    @Column(length = 300)
    private String tags;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getKicker() { return kicker; }
    public void setKicker(String kicker) { this.kicker = kicker; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
