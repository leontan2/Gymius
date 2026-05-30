package com.gymius.repository;

import com.gymius.domain.DailyNutritionGoal;
import com.gymius.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DailyNutritionGoalRepository extends JpaRepository<DailyNutritionGoal, UUID> {

    Optional<DailyNutritionGoal> findByUser(UserAccount user);
}
