package com.gymius.repository;

import com.gymius.domain.ExerciseLog;
import com.gymius.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExerciseLogRepository extends JpaRepository<ExerciseLog, UUID> {

    @Query("""
            select exercise
            from ExerciseLog exercise
            join fetch exercise.workout workout
            where workout.user = :user
            order by lower(exercise.exerciseName), workout.workoutDate
            """)
    List<ExerciseLog> findAllForUser(@Param("user") UserAccount user);
}
