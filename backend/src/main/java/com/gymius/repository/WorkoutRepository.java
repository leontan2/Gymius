package com.gymius.repository;

import com.gymius.domain.UserAccount;
import com.gymius.domain.Workout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutRepository extends JpaRepository<Workout, UUID> {

    List<Workout> findByUserOrderByWorkoutDateDescCreatedAtDesc(UserAccount user);

    Optional<Workout> findByIdAndUser(UUID id, UserAccount user);
}
