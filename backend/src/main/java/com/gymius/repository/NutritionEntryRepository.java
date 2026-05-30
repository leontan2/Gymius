package com.gymius.repository;

import com.gymius.domain.NutritionEntry;
import com.gymius.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface NutritionEntryRepository extends JpaRepository<NutritionEntry, UUID> {

    List<NutritionEntry> findByUserAndEntryDateOrderByMealTimeDescCreatedAtDesc(UserAccount user, LocalDate entryDate);
}
