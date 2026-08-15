import { DatePipe, DecimalPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  inject
} from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  LucideBadgeCheck,
  LucideCamera,
  LucideCircleAlert,
  LucideClock,
  LucideFlame,
  LucideImageUp,
  LucideSalad,
  LucideSparkles,
  LucideTarget,
  LucideUpload
} from '@lucide/angular';
import { Subscription, finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { toLocalDateInputValue } from '../../core/date.utils';
import { apiErrorMessage } from '../../core/http-error';
import { readImageDimensions } from '../../core/image-dimensions';
import {
  MealAnalysis,
  NutritionConfidence,
  NutritionEntry,
  NutritionEntryRequest,
  NutritionToday
} from '../../core/models';

@Component({
  selector: 'app-nutrition',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    LucideBadgeCheck,
    LucideCamera,
    LucideCircleAlert,
    LucideClock,
    LucideFlame,
    LucideImageUp,
    LucideSalad,
    LucideSparkles,
    LucideTarget,
    LucideUpload
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './nutrition.component.html'
})
export class NutritionComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly fb = inject(NonNullableFormBuilder);
  private analysisSubscription?: Subscription;
  private todaySubscription?: Subscription;
  private analysisRequestId = 0;
  private todayRequestId = 0;

  readonly maxFileBytes = 5 * 1024 * 1024;
  readonly maxSourceFileBytes = 20 * 1024 * 1024;
  readonly maxSourcePixels = 40_000_000;
  readonly todayInputValue = toLocalDateInputValue(new Date());

  readonly confirmForm = this.fb.group({
    calories: [0, [Validators.required, Validators.min(0), Validators.max(10000)]],
    notes: ['', [Validators.maxLength(1000)]]
  });

  readonly goalForm = this.fb.group({
    dailyCalories: [2200, [Validators.required, Validators.min(800), Validators.max(8000)]]
  });

  today?: NutritionToday;
  analysis?: MealAnalysis;
  selectedFileName = '';
  previewUrl = '';
  loadingToday = true;
  analyzing = false;
  saving = false;
  savingGoal = false;
  error = '';
  todayError = '';
  savedMessage = '';

  ngOnInit(): void {
    this.loadToday();
  }

  ngOnDestroy(): void {
    this.analysisRequestId++;
    this.todayRequestId++;
    this.analysisSubscription?.unsubscribe();
    this.todaySubscription?.unsubscribe();
    this.revokePreview();
  }

  get calorieProgress(): number {
    if (!this.today?.goal.dailyCalories) {
      return 0;
    }

    return Math.min((this.today.caloriesConsumed / this.today.goal.dailyCalories) * 100, 100);
  }

  get detectedFoodText(): string {
    const description = this.analysis?.foodItems.map((item) => item.name).join(', ') || 'Meal photo';
    return description.slice(0, 1000);
  }

  openFilePicker(input: HTMLInputElement): void {
    if (this.analyzing) {
      return;
    }

    input.value = '';
    input.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (file) {
      void this.prepareAndAnalyze(file);
    }
  }

  saveEntry(): void {
    this.error = '';
    this.savedMessage = '';

    if (!this.analysis) {
      this.error = 'Analyze a meal photo before saving.';
      return;
    }

    if (this.confirmForm.invalid) {
      this.confirmForm.markAllAsTouched();
      return;
    }

    const raw = this.confirmForm.getRawValue();
    const payload: NutritionEntryRequest = {
      entryDate: this.today?.date ?? this.todayInputValue,
      foodItems: this.detectedFoodText,
      calories: Number(raw.calories),
      calorieMin: this.analysis.calorieMin,
      calorieMax: this.analysis.calorieMax,
      proteinGrams: this.analysis.proteinGrams,
      carbsGrams: this.analysis.carbsGrams,
      fatGrams: this.analysis.fatGrams,
      confidence: this.analysis.confidence,
      notes: raw.notes.trim() || this.analysis.confidenceNote || null
    };

    this.saving = true;
    this.api.createNutritionEntry(payload)
      .pipe(finalize(() => {
        this.saving = false;
        this.changeDetector.markForCheck();
      }))
      .subscribe({
        next: () => {
          this.savedMessage = 'Added to today\'s nutrition log.';
          this.analysis = undefined;
          this.selectedFileName = '';
          this.confirmForm.reset({ calories: 0, notes: '' });
          this.revokePreview();
          this.loadToday(false);
        },
        error: (error: unknown) => {
          this.error = apiErrorMessage(error, 'Meal could not be saved.');
        }
      });
  }

  saveGoal(): void {
    this.error = '';
    this.savedMessage = '';

    if (this.goalForm.invalid) {
      this.goalForm.markAllAsTouched();
      return;
    }

    const raw = this.goalForm.getRawValue();
    const currentGoal = this.today?.goal;
    this.savingGoal = true;
    this.api.updateNutritionGoal({
      dailyCalories: Number(raw.dailyCalories),
      proteinGoalGrams: currentGoal?.proteinGoalGrams ?? null,
      carbsGoalGrams: currentGoal?.carbsGoalGrams ?? null,
      fatGoalGrams: currentGoal?.fatGoalGrams ?? null
    }).pipe(finalize(() => {
      this.savingGoal = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: () => {
        this.savedMessage = 'Daily calorie target updated.';
        this.loadToday(false);
      },
      error: (error: unknown) => {
        this.error = apiErrorMessage(error, 'Daily target could not be saved.');
      }
    });
  }

  confidenceLabel(confidence: NutritionConfidence): string {
    return confidence.charAt(0) + confidence.slice(1).toLowerCase();
  }

  retryToday(): void {
    this.todayError = '';
    this.loadToday();
  }

  clampedProgress(value: number, maximum: number): number {
    return Math.min(Math.max(value, 0), maximum);
  }

  controlInvalid(controlName: 'calories' | 'notes'): boolean {
    const control = this.confirmForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  trackFoodItem(index: number): number {
    return index;
  }

  trackEntry(_index: number, entry: NutritionEntry): string {
    return entry.id;
  }

  private loadToday(showLoading = true): void {
    const requestId = ++this.todayRequestId;
    this.todaySubscription?.unsubscribe();
    if (showLoading) {
      this.loadingToday = true;
    }
    this.todayError = '';

    this.todaySubscription = this.api.nutritionToday().subscribe({
      next: (today) => {
        if (requestId !== this.todayRequestId) {
          return;
        }

        this.today = today;
        this.goalForm.patchValue({ dailyCalories: today.goal.dailyCalories });
        this.loadingToday = false;
        this.changeDetector.markForCheck();
      },
      error: (error: unknown) => {
        if (requestId !== this.todayRequestId) {
          return;
        }

        this.todayError = apiErrorMessage(error, 'Nutrition data could not be loaded.');
        this.loadingToday = false;
        this.changeDetector.markForCheck();
      }
    });
  }

  private async prepareAndAnalyze(file: File): Promise<void> {
    const requestId = ++this.analysisRequestId;
    this.analysisSubscription?.unsubscribe();
    this.error = '';
    this.savedMessage = '';
    this.analysis = undefined;
    this.selectedFileName = '';
    this.revokePreview();

    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type.toLowerCase())) {
      this.error = 'Choose a JPEG, PNG, or WebP meal photo.';
      return;
    }

    if (file.size > this.maxSourceFileBytes) {
      this.error = 'Choose a source photo that is 20 MB or smaller.';
      return;
    }

    this.analyzing = true;

    try {
      const prepared = await this.compressImage(file);
      if (requestId !== this.analysisRequestId) {
        return;
      }

      if (prepared.size > this.maxFileBytes) {
        this.error = 'Meal photo must be 5 MB or smaller.';
        this.analyzing = false;
        this.changeDetector.markForCheck();
        return;
      }

      this.selectedFileName = file.name || 'Meal photo';
      this.setPreview(prepared);
      this.changeDetector.markForCheck();

      this.analysisSubscription = this.api.analyzeMealImage(prepared)
        .pipe(finalize(() => {
          if (requestId === this.analysisRequestId) {
            this.analyzing = false;
            this.changeDetector.markForCheck();
          }
        }))
        .subscribe({
          next: (analysis) => {
            if (requestId !== this.analysisRequestId) {
              return;
            }

            this.analysis = analysis;
            this.confirmForm.patchValue({
              calories: analysis.estimatedCalories,
              notes: analysis.confidenceNote
            });
          },
          error: (error: unknown) => {
            if (requestId === this.analysisRequestId) {
              this.error = apiErrorMessage(error, 'Meal photo could not be analyzed.');
            }
          }
        });
    } catch {
      if (requestId === this.analysisRequestId) {
        this.error = 'Meal photo could not be prepared. Try a different image.';
        this.analyzing = false;
        this.changeDetector.markForCheck();
      }
    }
  }

  private async compressImage(file: File): Promise<File> {
    const dimensions = await readImageDimensions(file);
    if (
      dimensions.width > 12000
      || dimensions.height > 12000
      || dimensions.width * dimensions.height > this.maxSourcePixels
    ) {
      throw new Error('Image dimensions are too large.');
    }

    return new Promise((resolve, reject) => {
      const sourceUrl = URL.createObjectURL(file);
      const image = new Image();
      const cleanup = (): void => URL.revokeObjectURL(sourceUrl);

      image.onerror = () => {
        cleanup();
        reject();
      };
      image.onload = () => {
        try {
          if (!image.width || !image.height) {
            reject();
            return;
          }

          const maxSide = 1600;
          const scale = Math.min(maxSide / Math.max(image.width, image.height), 1);
          const width = Math.round(image.width * scale);
          const height = Math.round(image.height * scale);
          const canvas = document.createElement('canvas');
          canvas.width = width;
          canvas.height = height;

          const context = canvas.getContext('2d');
          if (!context) {
            reject();
            return;
          }

          context.fillStyle = '#ffffff';
          context.fillRect(0, 0, width, height);
          context.drawImage(image, 0, 0, width, height);
          canvas.toBlob((blob) => {
            if (!blob) {
              reject();
              return;
            }

            resolve(new File([blob], 'meal-photo.jpg', { type: 'image/jpeg' }));
          }, 'image/jpeg', 0.86);
        } catch {
          reject();
        } finally {
          cleanup();
        }
      };
      image.src = sourceUrl;
    });
  }

  private setPreview(file: File): void {
    this.revokePreview();
    this.previewUrl = URL.createObjectURL(file);
  }

  private revokePreview(): void {
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
      this.previewUrl = '';
    }
  }
}
