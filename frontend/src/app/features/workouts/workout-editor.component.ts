import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import {
  AbstractControl,
  ReactiveFormsModule,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  LucideCalendarDays,
  LucideDynamicIcon,
  LucidePlus,
  LucideSave,
  LucideStickyNote,
  LucideTrash2,
  provideLucideIcons
} from '@lucide/angular';
import { ApiService } from '../../core/api.service';
import { ExerciseLog, Workout, WorkoutRequest } from '../../core/models';

@Component({
  selector: 'app-workout-editor',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    LucideDynamicIcon
  ],
  providers: [
    provideLucideIcons(
      LucideCalendarDays,
      LucidePlus,
      LucideSave,
      LucideStickyNote,
      LucideTrash2
    )
  ],
  templateUrl: './workout-editor.component.html'
})
export class WorkoutEditorComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(UntypedFormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly maxDate = this.toDateInputValue(new Date());
  readonly form = this.fb.group({
    workoutDate: [this.maxDate, [Validators.required]],
    notes: ['', [Validators.maxLength(1000)]],
    exercises: this.fb.array([])
  });

  workoutId: string | null = null;
  loading = false;
  saving = false;
  submitted = false;
  error = '';

  ngOnInit(): void {
    this.workoutId = this.route.snapshot.paramMap.get('id');

    if (this.workoutId) {
      this.loading = true;
      this.api.workout(this.workoutId).subscribe({
        next: (workout) => {
          this.patchWorkout(workout);
          this.loading = false;
        },
        error: () => {
          this.error = 'Workout could not be loaded.';
          this.loading = false;
        }
      });
      return;
    }

    this.addExercise();
  }

  get isEditMode(): boolean {
    return Boolean(this.workoutId);
  }

  get exercises(): UntypedFormArray {
    return this.form.get('exercises') as UntypedFormArray;
  }

  addExercise(exercise?: ExerciseLog): void {
    this.exercises.push(this.createExerciseGroup(exercise));
  }

  removeExercise(index: number): void {
    if (this.exercises.length === 1) {
      return;
    }

    this.exercises.removeAt(index);
  }

  save(): void {
    this.submitted = true;
    this.error = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = this.toPayload();
    this.saving = true;
    const request = this.workoutId
      ? this.api.updateWorkout(this.workoutId, payload)
      : this.api.createWorkout(payload);

    request.subscribe({
      next: () => {
        this.saving = false;
        this.router.navigateByUrl('/workouts');
      },
      error: () => {
        this.error = 'Workout could not be saved. Check the fields and try again.';
        this.saving = false;
      }
    });
  }

  controlHasError(control: AbstractControl | null, error: string): boolean {
    return Boolean(control?.hasError(error) && (control.touched || control.dirty || this.submitted));
  }

  private createExerciseGroup(exercise?: ExerciseLog): UntypedFormGroup {
    return this.fb.group({
      exerciseName: [exercise?.exerciseName ?? '', [Validators.required, Validators.maxLength(120)]],
      sets: [exercise?.sets ?? 3, [Validators.required, Validators.min(1), Validators.max(100)]],
      reps: [exercise?.reps ?? 8, [Validators.required, Validators.min(1), Validators.max(1000)]],
      weight: [exercise?.weight ?? 0, [Validators.required, Validators.min(0)]],
      notes: [exercise?.notes ?? '', [Validators.maxLength(500)]]
    });
  }

  private patchWorkout(workout: Workout): void {
    this.form.patchValue({
      workoutDate: workout.workoutDate,
      notes: workout.notes ?? ''
    });

    this.exercises.clear();
    workout.exercises.forEach((exercise) => this.addExercise(exercise));

    if (!this.exercises.length) {
      this.addExercise();
    }
  }

  private toPayload(): WorkoutRequest {
    const raw = this.form.getRawValue() as {
      workoutDate: string;
      notes: string;
      exercises: ExerciseLog[];
    };

    return {
      workoutDate: raw.workoutDate,
      notes: raw.notes?.trim() || null,
      exercises: raw.exercises.map((exercise) => ({
        exerciseName: exercise.exerciseName.trim(),
        sets: Number(exercise.sets),
        reps: Number(exercise.reps),
        weight: Number(exercise.weight),
        notes: exercise.notes?.trim() || null
      }))
    };
  }

  private toDateInputValue(date: Date): string {
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 10);
  }
}
