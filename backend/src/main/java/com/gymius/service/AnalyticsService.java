package com.gymius.service;

import com.gymius.domain.ExerciseLog;
import com.gymius.domain.UserAccount;
import com.gymius.domain.Workout;
import com.gymius.dto.DashboardDto;
import com.gymius.dto.DashboardInsightDto;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class AnalyticsService {

    private static final int WEEKLY_WORKOUT_GOAL = 3;
    private static final int FOCUS_WINDOW_DAYS = 30;

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
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        long weeklyCount = workouts.stream()
                .filter(workout -> !workout.getWorkoutDate().isBefore(weekStart))
                .filter(workout -> !workout.getWorkoutDate().isAfter(today))
                .count();

        BigDecimal totalVolume = workouts.stream()
                .map(workoutMapper::totalVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardDto(
                workouts.stream().limit(5).map(workoutMapper::toSummaryDto).toList(),
                weeklyCount,
                workouts.size(),
                totalVolume,
                dashboardInsight(workouts, today, weekStart, weeklyCount)
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

    private DashboardInsightDto dashboardInsight(
            List<Workout> workouts,
            LocalDate today,
            LocalDate weekStart,
            long weeklyCount
    ) {
        Workout lastWorkout = lastCompletedWorkout(workouts, today);
        Long daysSinceLastWorkout = lastWorkout == null
                ? null
                : ChronoUnit.DAYS.between(lastWorkout.getWorkoutDate(), today);
        ExerciseFocus topExercise = topExerciseFocus(workouts, today);
        long workoutsRemaining = Math.max(0, WEEKLY_WORKOUT_GOAL - weeklyCount);

        return new DashboardInsightDto(
                WEEKLY_WORKOUT_GOAL,
                (int) Math.min(100, Math.round((weeklyCount * 100.0) / WEEKLY_WORKOUT_GOAL)),
                workoutsRemaining,
                trainingStreakWeeks(workouts, today, weekStart),
                daysSinceLastWorkout,
                lastWorkout == null ? null : lastWorkout.getWorkoutDate(),
                topExercise == null ? null : topExercise.exerciseName(),
                topExercise == null ? 0 : topExercise.count(),
                guidanceTitle(workouts, weeklyCount, daysSinceLastWorkout),
                guidanceBody(workouts, weeklyCount, workoutsRemaining, daysSinceLastWorkout)
        );
    }

    private Workout lastCompletedWorkout(List<Workout> workouts, LocalDate today) {
        return workouts.stream()
                .filter(workout -> !workout.getWorkoutDate().isAfter(today))
                .findFirst()
                .orElse(null);
    }

    private long trainingStreakWeeks(List<Workout> workouts, LocalDate today, LocalDate weekStart) {
        Set<LocalDate> workoutWeeks = new HashSet<>();

        for (Workout workout : workouts) {
            if (!workout.getWorkoutDate().isAfter(today)) {
                workoutWeeks.add(workout.getWorkoutDate().with(DayOfWeek.MONDAY));
            }
        }

        long streak = 0;
        LocalDate cursor = weekStart;
        while (workoutWeeks.contains(cursor)) {
            streak++;
            cursor = cursor.minusWeeks(1);
        }

        return streak;
    }

    private ExerciseFocus topExerciseFocus(List<Workout> workouts, LocalDate today) {
        LocalDate focusStart = today.minusDays(FOCUS_WINDOW_DAYS - 1L);
        Map<String, ExerciseFocus> exercisesByName = new HashMap<>();

        for (Workout workout : workouts) {
            if (workout.getWorkoutDate().isBefore(focusStart) || workout.getWorkoutDate().isAfter(today)) {
                continue;
            }

            for (ExerciseLog exercise : workout.getExercises()) {
                String key = normalizeName(exercise.getExerciseName());
                exercisesByName
                        .computeIfAbsent(key, ignored -> new ExerciseFocus(exercise.getExerciseName()))
                        .increment();
            }
        }

        return exercisesByName.values().stream()
                .sorted(Comparator
                        .comparingInt(ExerciseFocus::count)
                        .reversed()
                        .thenComparing(ExerciseFocus::exerciseName, String.CASE_INSENSITIVE_ORDER))
                .findFirst()
                .orElse(null);
    }

    private String guidanceTitle(List<Workout> workouts, long weeklyCount, Long daysSinceLastWorkout) {
        if (workouts.isEmpty()) {
            return "Start with one solid session";
        }

        if (daysSinceLastWorkout == null) {
            return "Log the first completed session";
        }

        if (weeklyCount >= WEEKLY_WORKOUT_GOAL) {
            return "Weekly target hit";
        }

        if (daysSinceLastWorkout != null && daysSinceLastWorkout == 0) {
            return "Session captured";
        }

        if (daysSinceLastWorkout != null && daysSinceLastWorkout == 1) {
            return "Recovery window";
        }

        if (daysSinceLastWorkout != null && daysSinceLastWorkout <= 3) {
            return "Good time to train";
        }

        return "Restart the rhythm";
    }

    private String guidanceBody(
            List<Workout> workouts,
            long weeklyCount,
            long workoutsRemaining,
            Long daysSinceLastWorkout
    ) {
        if (workouts.isEmpty()) {
            return "Log your first workout to unlock streaks, recovery timing, and exercise focus.";
        }

        if (daysSinceLastWorkout == null) {
            return "Your workout history starts after today. Log a completed session to unlock streaks, recovery timing, and exercise focus.";
        }

        if (weeklyCount >= WEEKLY_WORKOUT_GOAL) {
            return "You are at " + weeklyCount + " of " + WEEKLY_WORKOUT_GOAL
                    + " workouts this week. Use the next session for technique, mobility, or an optional top-up.";
        }

        if (daysSinceLastWorkout != null && daysSinceLastWorkout == 0) {
            return "You are at " + weeklyCount + " of " + WEEKLY_WORKOUT_GOAL
                    + " workouts this week. Add notes now while the lift details are fresh.";
        }

        if (daysSinceLastWorkout != null && daysSinceLastWorkout == 1) {
            return "It has been 1 day since your last workout. Keep the next session crisp, or recover if you feel heavy.";
        }

        if (daysSinceLastWorkout != null && daysSinceLastWorkout <= 3) {
            return "You have " + workoutsRemaining + " " + workoutLabel(workoutsRemaining)
                    + " left for the weekly target. This is a strong window to book the next session.";
        }

        return "It has been " + daysSinceLastWorkout + " " + dayLabel(daysSinceLastWorkout)
                + " since your last workout. Start small and rebuild the week with one clean session.";
    }

    private String workoutLabel(long count) {
        return count == 1 ? "workout" : "workouts";
    }

    private String dayLabel(long count) {
        return count == 1 ? "day" : "days";
    }

    private static class ExerciseFocus {
        private final String exerciseName;
        private int count;

        private ExerciseFocus(String exerciseName) {
            this.exerciseName = exerciseName;
        }

        private String exerciseName() {
            return exerciseName;
        }

        private int count() {
            return count;
        }

        private void increment() {
            count++;
        }
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
