package com.gymius.service;

import com.gymius.domain.UserAccount;
import com.gymius.domain.Workout;
import com.gymius.dto.ExerciseLogRequest;
import com.gymius.dto.WorkoutRequest;
import com.gymius.mapper.WorkoutMapper;
import com.gymius.repository.WorkoutRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    @Mock
    private WorkoutRepository workoutRepository;

    @Mock
    private WorkoutMapper workoutMapper;

    @Test
    void flushesNewWorkoutsBeforeMappingGeneratedValues() {
        WorkoutService workoutService = new WorkoutService(workoutRepository, workoutMapper);
        UserAccount user = new UserAccount();
        WorkoutRequest request = new WorkoutRequest(
                LocalDate.of(2025, 1, 2),
                "  Strong session  ",
                List.of(new ExerciseLogRequest(
                        "  Squat  ",
                        3,
                        5,
                        new BigDecimal("100"),
                        "  Controlled tempo  "
                ))
        );
        when(workoutRepository.saveAndFlush(any(Workout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        workoutService.create(user, request);

        ArgumentCaptor<Workout> workoutCaptor = ArgumentCaptor.forClass(Workout.class);
        verify(workoutRepository).saveAndFlush(workoutCaptor.capture());
        Workout savedWorkout = workoutCaptor.getValue();
        verify(workoutMapper).toDto(savedWorkout);
        assertThat(savedWorkout.getUser()).isSameAs(user);
        assertThat(savedWorkout.getNotes()).isEqualTo("Strong session");
        assertThat(savedWorkout.getExercises()).hasSize(1);
        assertThat(savedWorkout.getExercises().get(0).getExerciseName()).isEqualTo("Squat");
        assertThat(savedWorkout.getExercises().get(0).getNotes()).isEqualTo("Controlled tempo");
        assertThat(savedWorkout.getExercises().get(0).getWorkout()).isSameAs(savedWorkout);
    }
}
