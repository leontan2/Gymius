import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideActivity,
  LucideArrowUpRight,
  LucideCalendarDays,
  LucideClock,
  LucideDumbbell,
  LucideFlame,
  LucidePlus,
  LucideScale,
  LucideTarget,
  LucideTrophy
} from '@lucide/angular';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { apiErrorMessage } from '../../core/http-error';
import { Dashboard, DashboardInsight } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    RouterLink,
    LucideActivity,
    LucideArrowUpRight,
    LucideCalendarDays,
    LucideClock,
    LucideDumbbell,
    LucideFlame,
    LucidePlus,
    LucideScale,
    LucideTarget,
    LucideTrophy
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  dashboard?: Dashboard;
  loading = true;
  error = '';

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = '';
    this.api.dashboard().pipe(finalize(() => {
      this.loading = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: (dashboard) => {
        this.dashboard = dashboard;
      },
      error: (error: unknown) => {
        this.error = apiErrorMessage(error, 'Dashboard data could not be loaded.');
      }
    });
  }

  trackWorkout(_index: number, workout: Dashboard['recentWorkouts'][number]): string {
    return workout.id;
  }

  workoutsRemainingLabel(insight: DashboardInsight): string {
    if (insight.workoutsRemaining === 0) {
      return 'Goal complete';
    }

    return `${insight.workoutsRemaining} ${insight.workoutsRemaining === 1 ? 'workout' : 'workouts'} to go`;
  }

  streakLabel(insight: DashboardInsight): string {
    if (insight.trainingStreakWeeks === 0) {
      return "Start this week's streak";
    }

    return insight.trainingStreakWeeks === 1 ? 'week with a workout' : 'weeks in a row';
  }

  lastWorkoutValue(insight: DashboardInsight): string {
    if (insight.daysSinceLastWorkout === null) {
      return 'Start';
    }

    if (insight.daysSinceLastWorkout === 0) {
      return 'Today';
    }

    return `${insight.daysSinceLastWorkout}d`;
  }

  lastWorkoutLabel(insight: DashboardInsight): string {
    if (insight.daysSinceLastWorkout === null) {
      return 'No workouts logged yet';
    }

    if (insight.daysSinceLastWorkout === 0) {
      return 'Last workout logged today';
    }

    return `${insight.daysSinceLastWorkout} ${insight.daysSinceLastWorkout === 1 ? 'day' : 'days'} since last workout`;
  }

  focusLabel(insight: DashboardInsight): string {
    if (!insight.topExerciseName) {
      return 'Log exercises to see your focus';
    }

    if (insight.topExerciseLogCount === 1) {
      return 'Logged once in the last 30 days';
    }

    return `Logged ${insight.topExerciseLogCount} times in the last 30 days`;
  }

  clampedProgress(value: number, maximum: number): number {
    return Math.min(Math.max(value, 0), maximum);
  }
}
