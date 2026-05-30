package com.gymius.service;

import com.gymius.domain.ExerciseLog;
import com.gymius.domain.UserAccount;
import com.gymius.domain.Workout;
import com.gymius.dto.DashboardDto;
import com.gymius.dto.PersonalRecordDto;
import com.gymius.dto.ProgressPointDto;
import com.gymius.dto.ProgressSeriesDto;
import com.gymius.mapper.WorkoutMapper;
import com.gymius.repository.ExerciseLogRepository;
import com.gymius.repository.WorkoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
public class AnalyticsService {

    private final WorkoutRepository workoutRepository;
    private final ExerciseLogRepository exerciseLogRepository;
    private final WorkoutMapper workoutMapper;

    public AnalyticsService(
            WorkoutRepository workoutRepository,
            ExerciseLogRepository exerciseLogRepository,
            WorkoutMapper workoutMapper
    ) {
        this.workoutRepository = workoutRepository;
        this.exerciseLogRepository = exerciseLogRepository;
        this.workoutMapper = workoutMapper;
    }

    @Transactional(readOnly = true)
    public DashboardDto dashboard(UserAccount user) {
        List<Workout> workouts = workoutRepository.findByUserOrderByWorkoutDateDescCreatedAtDesc(user);
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        long weeklyCount = workouts.stream()
                .filter(workout -> !workout.getWorkoutDate().isBefore(weekStart))
                .filter(workout -> !workout.getWorkoutDate().isAfter(weekEnd))
                .count();

        BigDecimal totalVolume = workouts.stream()
                .map(workoutMapper::totalVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardDto(
                workouts.stream().limit(5).map(workoutMapper::toSummaryDto).toList(),
                weeklyCount,
                workouts.size(),
                totalVolume
        );
    }

    @Transactional(readOnly = true)
    public List<ProgressSeriesDto> progress(UserAccount user) {
        Map<String, ProgressBucket> grouped = new TreeMap<>();

        for (ExerciseLog exercise : exerciseLogRepository.findAllForUser(user)) {
            String key = normalizeName(exercise.getExerciseName());
            ProgressBucket bucket = grouped.computeIfAbsent(key, ignored -> new ProgressBucket(exercise.getExerciseName()));
            bucket.add(exercise, workoutMapper.exerciseVolume(exercise));
        }

        return grouped.values().stream()
                .map(ProgressBucket::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PersonalRecordDto> personalRecords(UserAccount user) {
        Map<String, PersonalRecordBucket> grouped = new LinkedHashMap<>();

        for (ExerciseLog exercise : exerciseLogRepository.findAllForUser(user)) {
            String key = normalizeName(exercise.getExerciseName());
            grouped.computeIfAbsent(key, ignored -> new PersonalRecordBucket(exercise.getExerciseName()))
                    .consider(exercise);
        }

        return grouped.values().stream()
                .map(PersonalRecordBucket::toDto)
                .sorted(Comparator.comparing(PersonalRecordDto::exerciseName))
                .toList();
    }

    private String normalizeName(String exerciseName) {
        return exerciseName.trim().toLowerCase(Locale.ROOT);
    }

    private static class ProgressBucket {
        private final String exerciseName;
        private final Map<LocalDate, MutableProgressPoint> pointsByDate = new TreeMap<>();

        private ProgressBucket(String exerciseName) {
            this.exerciseName = exerciseName;
        }

        private void add(ExerciseLog exercise, BigDecimal volume) {
            LocalDate date = exercise.getWorkout().getWorkoutDate();
            MutableProgressPoint point = pointsByDate.computeIfAbsent(date, ignored -> new MutableProgressPoint());
            point.volume = point.volume.add(volume);

            if (point.maxWeight == null || exercise.getWeight().compareTo(point.maxWeight) > 0) {
                point.maxWeight = exercise.getWeight();
            }
        }

        private ProgressSeriesDto toDto() {
            List<ProgressPointDto> points = new ArrayList<>();
            pointsByDate.forEach((date, point) ->
                    points.add(new ProgressPointDto(date, point.maxWeight, point.volume))
            );
            return new ProgressSeriesDto(exerciseName, points);
        }
    }

    private static class MutableProgressPoint {
        private BigDecimal maxWeight;
        private BigDecimal volume = BigDecimal.ZERO;
    }

    private static class PersonalRecordBucket {
        private final String exerciseName;
        private ExerciseLog highestWeight;
        private ExerciseLog highestReps;

        private PersonalRecordBucket(String exerciseName) {
            this.exerciseName = exerciseName;
        }

        private void consider(ExerciseLog exercise) {
            if (highestWeight == null || exercise.getWeight().compareTo(highestWeight.getWeight()) > 0) {
                highestWeight = exercise;
            }

            if (highestReps == null || exercise.getReps() > highestReps.getReps()) {
                highestReps = exercise;
            }
        }

        private PersonalRecordDto toDto() {
            return new PersonalRecordDto(
                    exerciseName,
                    highestWeight.getWeight(),
                    highestWeight.getReps(),
                    highestWeight.getWorkout().getWorkoutDate(),
                    highestWeight.getWorkout().getId(),
                    highestReps.getReps(),
                    highestReps.getWeight(),
                    highestReps.getWorkout().getWorkoutDate(),
                    highestReps.getWorkout().getId()
            );
        }
    }
}
