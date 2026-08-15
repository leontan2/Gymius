import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideRepeat2, LucideTrophy, LucideWeight } from '@lucide/angular';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { apiErrorMessage } from '../../core/http-error';
import { PersonalRecord } from '../../core/models';

@Component({
  selector: 'app-personal-records',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    RouterLink,
    LucideRepeat2,
    LucideTrophy,
    LucideWeight
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './personal-records.component.html'
})
export class PersonalRecordsComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  records: PersonalRecord[] = [];
  loading = true;
  error = '';

  ngOnInit(): void {
    this.loadRecords();
  }

  loadRecords(): void {
    this.loading = true;
    this.error = '';
    this.api.personalRecords().pipe(finalize(() => {
      this.loading = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: (records) => {
        this.records = records;
      },
      error: (error: unknown) => {
        this.error = apiErrorMessage(error, 'Personal records could not be loaded.');
      }
    });
  }

  trackRecord(_index: number, record: PersonalRecord): string {
    return record.exerciseName;
  }
}
