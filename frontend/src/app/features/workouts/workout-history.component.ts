import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideCalendarDays,
  LucideDumbbell,
  LucideDynamicIcon,
  LucidePencil,
  LucidePlus,
  LucideSearch,
  LucideTrash2,
  provideLucideIcons
} from '@lucide/angular';
import { ApiService } from '../../core/api.service';
import { Workout } from '../../core/models';

@Component({
  selector: 'app-workout-history',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    DecimalPipe,
    RouterLink,
    LucideDynamicIcon
  ],
  providers: [
    provideLucideIcons(
      LucideCalendarDays,
      LucideDumbbell,
      LucidePencil,
      LucidePlus,
      LucideSearch,
      LucideTrash2
    )
  ],
  templateUrl: './workout-history.component.html'
})
export class WorkoutHistoryComponent implements OnInit {
  private readonly api = inject(ApiService);

  workouts: Workout[] = [];
  query = '';
  loading = true;
  error = '';

  ngOnInit(): void {
    this.loadWorkouts();
  }

  get filteredWorkouts(): Workout[] {
    const term = this.query.trim().toLowerCase();
    if (!term) {
      return this.workouts;
    }

    return this.workouts.filter((workout) =>
      workout.workoutDate.includes(term)
      || workout.exercises.some((exercise) => exercise.exerciseName.toLowerCase().includes(term))
      || (workout.notes ?? '').toLowerCase().includes(term)
    );
  }

  updateQuery(event: Event): void {
    this.query = (event.target as HTMLInputElement).value;
  }

  deleteWorkout(workout: Workout): void {
    const confirmed = window.confirm(`Delete workout from ${workout.workoutDate}?`);
    if (!confirmed) {
      return;
    }

    this.api.deleteWorkout(workout.id).subscribe({
      next: () => {
        this.workouts = this.workouts.filter((item) => item.id !== workout.id);
      },
      error: () => {
        this.error = 'Workout could not be deleted.';
      }
    });
  }

  trackByWorkoutId(_index: number, workout: Workout): string {
    return workout.id;
  }

  private loadWorkouts(): void {
    this.api.workouts().subscribe({
      next: (workouts) => {
        this.workouts = workouts;
        this.loading = false;
      },
      error: () => {
        this.error = 'Workout history could not be loaded.';
        this.loading = false;
      }
    });
  }
}
