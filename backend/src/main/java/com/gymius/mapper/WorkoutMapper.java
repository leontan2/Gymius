package com.gymius.mapper;

import com.gymius.domain.ExerciseLog;
import com.gymius.domain.Workout;
import com.gymius.dto.ExerciseLogDto;
import com.gymius.dto.WorkoutDto;
import com.gymius.dto.WorkoutSummaryDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class WorkoutMapper {

    public WorkoutDto toDto(Workout workout) {
        List<ExerciseLogDto> exercises = workout.getExercises().stream()
                .map(this::toDto)
                .toList();

        return new WorkoutDto(
                workout.getId(),
                workout.getWorkoutDate(),
                workout.getNotes(),
                exercises,
                totalVolume(workout),
                workout.getCreatedAt(),
                workout.getUpdatedAt()
        );
    }

    public WorkoutSummaryDto toSummaryDto(Workout workout) {
        return new WorkoutSummaryDto(
                workout.getId(),
                workout.getWorkoutDate(),
                workout.getExercises().size(),
                totalVolume(workout)
        );
    }

    public ExerciseLogDto toDto(ExerciseLog exercise) {
        return new ExerciseLogDto(
                exercise.getId(),
                exercise.getExerciseName(),
                exercise.getSets(),
                exercise.getReps(),
                exercise.getWeight(),
                exercise.getNotes(),
                exercise.getSortOrder(),
                exerciseVolume(exercise)
        );
    }

    public BigDecimal totalVolume(Workout workout) {
        return workout.getExercises().stream()
                .map(this::exerciseVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal exerciseVolume(ExerciseLog exercise) {
        if (exercise.getWeight() == null || exercise.getSets() == null || exercise.getReps() == null) {
            return BigDecimal.ZERO;
        }

        return exercise.getWeight()
                .multiply(BigDecimal.valueOf(exercise.getSets()))
                .multiply(BigDecimal.valueOf(exercise.getReps()));
    }
}
