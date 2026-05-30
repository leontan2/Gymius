package com.gymius.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "daily_nutrition_goals",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_daily_nutrition_goals_user", columnNames = "user_id")
        }
)
public class DailyNutritionGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "daily_calories", nullable = false)
    private Integer dailyCalories;

    @Column(name = "protein_goal_grams", precision = 8, scale = 2)
    private BigDecimal proteinGoalGrams;

    @Column(name = "carbs_goal_grams", precision = 8, scale = 2)
    private BigDecimal carbsGoalGrams;

    @Column(name = "fat_goal_grams", precision = 8, scale = 2)
    private BigDecimal fatGoalGrams;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public void setUser(UserAccount user) {
        this.user = user;
    }

    public Integer getDailyCalories() {
        return dailyCalories;
    }

    public void setDailyCalories(Integer dailyCalories) {
        this.dailyCalories = dailyCalories;
    }

    public BigDecimal getProteinGoalGrams() {
        return proteinGoalGrams;
    }

    public void setProteinGoalGrams(BigDecimal proteinGoalGrams) {
        this.proteinGoalGrams = proteinGoalGrams;
    }

    public BigDecimal getCarbsGoalGrams() {
        return carbsGoalGrams;
    }

    public void setCarbsGoalGrams(BigDecimal carbsGoalGrams) {
        this.carbsGoalGrams = carbsGoalGrams;
    }

    public BigDecimal getFatGoalGrams() {
        return fatGoalGrams;
    }

    public void setFatGoalGrams(BigDecimal fatGoalGrams) {
        this.fatGoalGrams = fatGoalGrams;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
