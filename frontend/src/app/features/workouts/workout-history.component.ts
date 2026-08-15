import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideCalendarDays,
  LucideDumbbell,
  LucidePencil,
  LucidePlus,
  LucideSearch,
  LucideTrash2
} from '@lucide/angular';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { apiErrorMessage } from '../../core/http-error';
import { ExerciseLog, Workout } from '../../core/models';

@Component({
  selector: 'app-workout-history',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    RouterLink,
    LucideCalendarDays,
    LucideDumbbell,
    LucidePencil,
    LucidePlus,
    LucideSearch,
    LucideTrash2
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './workout-history.component.html'
})
export class WorkoutHistoryComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  workouts: Workout[] = [];
  filteredWorkouts: Workout[] = [];
  query = '';
  loading = true;
  error = '';
  deletingWorkoutId: string | null = null;

  ngOnInit(): void {
    this.loadWorkouts();
  }

  updateQuery(event: Event): void {
    this.query = (event.target as HTMLInputElement).value;
    this.applyFilter();
  }

  clearSearch(): void {
    this.query = '';
    this.applyFilter();
  }

  private applyFilter(): void {
    const term = this.query.trim().toLowerCase();
    if (!term) {
      this.filteredWorkouts = this.workouts;
      return;
    }

    this.filteredWorkouts = this.workouts.filter((workout) =>
      workout.workoutDate.includes(term)
      || workout.exercises.some((exercise) => exercise.exerciseName.toLowerCase().includes(term))
      || (workout.notes ?? '').toLowerCase().includes(term)
    );
  }

  deleteWorkout(workout: Workout): void {
    if (this.deletingWorkoutId !== null) {
      return;
    }

    const confirmed = window.confirm(`Delete workout from ${workout.workoutDate}?`);
    if (!confirmed) {
      return;
    }

    this.error = '';
    this.deletingWorkoutId = workout.id;
    this.api.deleteWorkout(workout.id).pipe(finalize(() => {
      this.deletingWorkoutId = null;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: () => {
        this.workouts = this.workouts.filter((item) => item.id !== workout.id);
        this.applyFilter();
      },
      error: (error: unknown) => {
        this.error = apiErrorMessage(error, 'Workout could not be deleted.');
      }
    });
  }

  trackByWorkoutId(_index: number, workout: Workout): string {
    return workout.id;
  }

  trackExercise(index: number, exercise: ExerciseLog): string {
    return exercise.id ?? `${exercise.exerciseName}-${index}`;
  }

  loadWorkouts(): void {
    this.loading = true;
    this.error = '';
    this.api.workouts().pipe(finalize(() => {
      this.loading = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: (workouts) => {
        this.workouts = workouts;
        this.applyFilter();
      },
      error: (error: unknown) => {
        this.error = apiErrorMessage(error, 'Workout history could not be loaded.');
      }
    });
  }
}
