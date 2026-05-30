package com.gymius.service;

import com.gymius.domain.ExerciseLog;
import com.gymius.domain.UserAccount;
import com.gymius.domain.Workout;
import com.gymius.dto.ExerciseLogDto;
import com.gymius.dto.ExerciseLogRequest;
import com.gymius.dto.WorkoutDto;
import com.gymius.dto.WorkoutRequest;
import com.gymius.mapper.WorkoutMapper;
import com.gymius.repository.WorkoutRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutMapper workoutMapper;

    public WorkoutService(WorkoutRepository workoutRepository, WorkoutMapper workoutMapper) {
        this.workoutRepository = workoutRepository;
        this.workoutMapper = workoutMapper;
    }

    @Transactional(readOnly = true)
    public List<WorkoutDto> list(UserAccount user) {
        return workoutRepository.findByUserOrderByWorkoutDateDescCreatedAtDesc(user).stream()
                .map(workoutMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkoutDto get(UserAccount user, UUID workoutId) {
        return workoutMapper.toDto(findOwnedWorkout(user, workoutId));
    }

    @Transactional
    public WorkoutDto create(UserAccount user, WorkoutRequest request) {
        Workout workout = new Workout();
        workout.setUser(user);
        applyRequest(workout, request);
        return workoutMapper.toDto(workoutRepository.save(workout));
    }

    @Transactional
    public WorkoutDto update(UserAccount user, UUID workoutId, WorkoutRequest request) {
        Workout workout = findOwnedWorkout(user, workoutId);
        applyRequest(workout, request);
        return workoutMapper.toDto(workout);
    }

    @Transactional
    public void delete(UserAccount user, UUID workoutId) {
        Workout workout = findOwnedWorkout(user, workoutId);
        workoutRepository.delete(workout);
    }

    @Transactional
    public ExerciseLogDto addExercise(UserAccount user, UUID workoutId, ExerciseLogRequest request) {
        Workout workout = findOwnedWorkout(user, workoutId);
        ExerciseLog exercise = toExercise(request, workout.getExercises().size());
        workout.addExercise(exercise);
        return workoutMapper.toDto(exercise);
    }

    @Transactional
    public ExerciseLogDto updateExercise(UserAccount user, UUID workoutId, UUID exerciseId, ExerciseLogRequest request) {
        Workout workout = findOwnedWorkout(user, workoutId);
        ExerciseLog exercise = findExercise(workout, exerciseId);
        applyExerciseRequest(exercise, request, exercise.getSortOrder());
        return workoutMapper.toDto(exercise);
    }

    @Transactional
    public void deleteExercise(UserAccount user, UUID workoutId, UUID exerciseId) {
        Workout workout = findOwnedWorkout(user, workoutId);
        ExerciseLog exercise = findExercise(workout, exerciseId);
        workout.getExercises().remove(exercise);
    }

    private Workout findOwnedWorkout(UserAccount user, UUID workoutId) {
        return workoutRepository.findByIdAndUser(workoutId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found."));
    }

    private ExerciseLog findExercise(Workout workout, UUID exerciseId) {
        return workout.getExercises().stream()
                .filter(exercise -> exercise.getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found."));
    }

    private void applyRequest(Workout workout, WorkoutRequest request) {
        workout.setWorkoutDate(request.workoutDate());
        workout.setNotes(trimToNull(request.notes()));
        workout.getExercises().clear();

        for (int index = 0; index < request.exercises().size(); index++) {
            workout.addExercise(toExercise(request.exercises().get(index), index));
        }
    }

    private ExerciseLog toExercise(ExerciseLogRequest request, int sortOrder) {
        ExerciseLog exercise = new ExerciseLog();
        applyExerciseRequest(exercise, request, sortOrder);
        return exercise;
    }

    private void applyExerciseRequest(ExerciseLog exercise, ExerciseLogRequest request, int sortOrder) {
        exercise.setExerciseName(request.exerciseName().trim());
        exercise.setSets(request.sets());
        exercise.setReps(request.reps());
        exercise.setWeight(request.weight());
        exercise.setNotes(trimToNull(request.notes()));
        exercise.setSortOrder(sortOrder);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
