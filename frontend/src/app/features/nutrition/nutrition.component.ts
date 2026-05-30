import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import {
  LucideBadgeCheck,
  LucideCamera,
  LucideCircleAlert,
  LucideClock,
  LucideDynamicIcon,
  LucideFlame,
  LucideImageUp,
  LucideRefreshCcw,
  LucideSalad,
  LucideSparkles,
  LucideTarget,
  LucideUpload,
  provideLucideIcons
} from '@lucide/angular';
import { ApiService } from '../../core/api.service';
import { MealAnalysis, NutritionConfidence, NutritionEntryRequest, NutritionToday } from '../../core/models';

@Component({
  selector: 'app-nutrition',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    LucideDynamicIcon
  ],
  providers: [
    provideLucideIcons(
      LucideBadgeCheck,
      LucideCamera,
      LucideCircleAlert,
      LucideClock,
      LucideFlame,
      LucideImageUp,
      LucideRefreshCcw,
      LucideSalad,
      LucideSparkles,
      LucideTarget,
      LucideUpload
    )
  ],
  templateUrl: './nutrition.component.html'
})
export class NutritionComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly fb = inject(UntypedFormBuilder);

  readonly maxFileBytes = 5 * 1024 * 1024;
  readonly todayInputValue = this.toDateInputValue(new Date());

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
  savedMessage = '';

  ngOnInit(): void {
    this.loadToday();
  }

  ngOnDestroy(): void {
    this.revokePreview();
  }

  get calorieProgress(): number {
    if (!this.today?.goal.dailyCalories) {
      return 0;
    }

    return Math.min((this.today.caloriesConsumed / this.today.goal.dailyCalories) * 100, 100);
  }

  get detectedFoodText(): string {
    return this.analysis?.foodItems.map((item) => item.name).join(', ') || 'Meal photo';
  }

  openFilePicker(input: HTMLInputElement): void {
    input.value = '';
    input.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      return;
    }

    this.prepareAndAnalyze(file);
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

    const raw = this.confirmForm.getRawValue() as { calories: number; notes: string };
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
      notes: raw.notes?.trim() || this.analysis.confidenceNote || null
    };

    this.saving = true;
    this.api.createNutritionEntry(payload).subscribe({
      next: () => {
        this.saving = false;
        this.savedMessage = 'Added to today\'s nutrition log.';
        this.analysis = undefined;
        this.selectedFileName = '';
        this.confirmForm.reset({ calories: 0, notes: '' });
        this.revokePreview();
        this.loadToday(false);
      },
      error: (error) => {
        this.error = this.errorMessage(error, 'Meal could not be saved.');
        this.saving = false;
      }
    });
  }

  saveGoal(): void {
    this.error = '';

    if (this.goalForm.invalid) {
      this.goalForm.markAllAsTouched();
      return;
    }

    const raw = this.goalForm.getRawValue() as { dailyCalories: number };
    this.savingGoal = true;
    this.api.updateNutritionGoal({
      dailyCalories: Number(raw.dailyCalories),
      proteinGoalGrams: null,
      carbsGoalGrams: null,
      fatGoalGrams: null
    }).subscribe({
      next: () => {
        this.savingGoal = false;
        this.loadToday(false);
      },
      error: (error) => {
        this.error = this.errorMessage(error, 'Daily target could not be saved.');
        this.savingGoal = false;
      }
    });
  }

  confidenceLabel(confidence: NutritionConfidence): string {
    return confidence.charAt(0) + confidence.slice(1).toLowerCase();
  }

  private loadToday(showLoading = true): void {
    if (showLoading) {
      this.loadingToday = true;
    }

    this.api.nutritionToday().subscribe({
      next: (today) => {
        this.today = today;
        this.goalForm.patchValue({ dailyCalories: today.goal.dailyCalories });
        this.loadingToday = false;
      },
      error: (error) => {
        this.error = this.errorMessage(error, 'Nutrition data could not be loaded.');
        this.loadingToday = false;
      }
    });
  }

  private async prepareAndAnalyze(file: File): Promise<void> {
    this.error = '';
    this.savedMessage = '';
    this.analysis = undefined;

    if (!file.type.startsWith('image/')) {
      this.error = 'Choose a JPEG, PNG, or WebP meal photo.';
      return;
    }

    this.analyzing = true;

    try {
      const prepared = await this.compressImage(file);
      if (prepared.size > this.maxFileBytes) {
        this.error = 'Meal photo must be 5 MB or smaller.';
        this.analyzing = false;
        return;
      }

      this.selectedFileName = file.name || 'Meal photo';
      this.setPreview(prepared);

      this.api.analyzeMealImage(prepared).subscribe({
        next: (analysis) => {
          this.analysis = analysis;
          this.confirmForm.patchValue({
            calories: analysis.estimatedCalories,
            notes: analysis.confidenceNote
          });
          this.analyzing = false;
        },
        error: (error) => {
          this.error = this.errorMessage(error, 'Meal photo could not be analyzed.');
          this.analyzing = false;
        }
      });
    } catch {
      this.error = 'Meal photo could not be prepared. Try a different image.';
      this.analyzing = false;
    }
  }

  private compressImage(file: File): Promise<File> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onerror = () => reject();
      reader.onload = () => {
        const image = new Image();
        image.onerror = () => reject();
        image.onload = () => {
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

          context.drawImage(image, 0, 0, width, height);
          canvas.toBlob((blob) => {
            if (!blob) {
              reject();
              return;
            }

            resolve(new File([blob], 'meal-photo.jpg', { type: 'image/jpeg' }));
          }, 'image/jpeg', 0.86);
        };
        image.src = String(reader.result);
      };
      reader.readAsDataURL(file);
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

  private toDateInputValue(date: Date): string {
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 10);
  }

  private errorMessage(error: unknown, fallback: string): string {
    if (typeof error === 'object' && error !== null && 'error' in error) {
      const body = (error as { error?: { message?: string; detail?: string } }).error;
      return body?.message || body?.detail || fallback;
    }

    return fallback;
  }
}
