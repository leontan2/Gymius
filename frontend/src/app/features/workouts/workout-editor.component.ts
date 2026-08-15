import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  LucideCalendarDays,
  LucidePlus,
  LucideSave,
  LucideStickyNote,
  LucideTrash2
} from '@lucide/angular';
import { ApiService } from '../../core/api.service';
import { toLocalDateInputValue } from '../../core/date.utils';
import { apiErrorMessage } from '../../core/http-error';
import { ExerciseLog, ExerciseLogRequest, Workout, WorkoutRequest } from '../../core/models';
import { finalize } from 'rxjs';

interface ExerciseFormControls {
  exerciseName: FormControl<string>;
  sets: FormControl<number>;
  reps: FormControl<number>;
  weight: FormControl<number>;
  notes: FormControl<string>;
}

@Component({
  selector: 'app-workout-editor',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    LucideCalendarDays,
    LucidePlus,
    LucideSave,
    LucideStickyNote,
    LucideTrash2
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './workout-editor.component.html'
})
export class WorkoutEditorComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly maxDate = toLocalDateInputValue(new Date());
  readonly maxExercises = 200;
  readonly form = this.fb.group({
    workoutDate: [this.maxDate, [Validators.required, validWorkoutDate(this.maxDate)]],
    notes: ['', [Validators.maxLength(1000)]],
    exercises: this.fb.array<FormGroup<ExerciseFormControls>>([])
  });

  workoutId: string | null = null;
  loading = false;
  saving = false;
  loadFailed = false;
  submitted = false;
  error = '';

  ngOnInit(): void {
    this.workoutId = this.route.snapshot.paramMap.get('id');

    if (this.workoutId) {
      this.loadWorkout();
      return;
    }

    this.addExercise();
  }

  get isEditMode(): boolean {
    return Boolean(this.workoutId);
  }

  get exercises(): FormArray<FormGroup<ExerciseFormControls>> {
    return this.form.controls.exercises;
  }

  addExercise(exercise?: ExerciseLog): void {
    if (this.exercises.length >= this.maxExercises) {
      return;
    }

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
    this.form.disable();
    const request = this.workoutId
      ? this.api.updateWorkout(this.workoutId, payload)
      : this.api.createWorkout(payload);

    request.pipe(finalize(() => {
      this.saving = false;
      this.form.enable();
      this.changeDetector.markForCheck();
    })).subscribe({
      next: () => {
        void this.router.navigateByUrl('/workouts');
      },
      error: (error: unknown) => {
        this.error = apiErrorMessage(error, 'Workout could not be saved. Check the fields and try again.');
      }
    });
  }

  controlHasError(control: AbstractControl | null, error: string): boolean {
    return Boolean(control?.hasError(error) && (control.touched || control.dirty || this.submitted));
  }

  controlIsInvalid(control: AbstractControl | null): boolean {
    return Boolean(control?.invalid && (control.touched || control.dirty || this.submitted));
  }

  loadWorkout(): void {
    if (!this.workoutId) {
      return;
    }

    this.loading = true;
    this.loadFailed = false;
    this.error = '';
    this.api.workout(this.workoutId)
      .pipe(finalize(() => {
        this.loading = false;
        this.changeDetector.markForCheck();
      }))
      .subscribe({
        next: (workout) => this.patchWorkout(workout),
        error: (error: unknown) => {
          this.loadFailed = true;
          this.error = apiErrorMessage(error, 'Workout could not be loaded.');
        }
      });
  }

  trackExercise(_index: number, control: AbstractControl): AbstractControl {
    return control;
  }

  private createExerciseGroup(exercise?: ExerciseLog): FormGroup<ExerciseFormControls> {
    return this.fb.group({
      exerciseName: [exercise?.exerciseName ?? '', [trimmedRequired(), Validators.maxLength(120)]],
      sets: [exercise?.sets ?? 3, [Validators.required, Validators.min(1), Validators.max(100)]],
      reps: [exercise?.reps ?? 8, [Validators.required, Validators.min(1), Validators.max(1000)]],
      weight: [exercise?.weight ?? 0, [
        Validators.required,
        Validators.min(0),
        Validators.max(999999.99),
        validWeightPrecision()
      ]],
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
    const raw = this.form.getRawValue();

    return {
      workoutDate: raw.workoutDate,
      notes: raw.notes?.trim() || null,
      exercises: raw.exercises.map((exercise): ExerciseLogRequest => ({
        exerciseName: exercise.exerciseName.trim(),
        sets: Number(exercise.sets),
        reps: Number(exercise.reps),
        weight: Number(exercise.weight),
        notes: exercise.notes?.trim() || null
      }))
    };
  }
}

function trimmedRequired(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    return typeof value === 'string' && value.trim().length > 0 ? null : { required: true };
  };
}

function validWorkoutDate(maxDate: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value) {
      return null;
    }

    if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return { invalidDate: true };
    }

    return value <= maxDate ? null : { futureDate: true };
  };
}

function validWeightPrecision(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (value === null || value === '') {
      return null;
    }

    return /^\d{1,6}(?:\.\d{1,2})?$/.test(String(value)) ? null : { precision: true };
  };
}
