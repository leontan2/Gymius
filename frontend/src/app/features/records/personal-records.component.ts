import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideDynamicIcon,
  LucideRepeat2,
  LucideTrophy,
  LucideWeight,
  provideLucideIcons
} from '@lucide/angular';
import { ApiService } from '../../core/api.service';
import { PersonalRecord } from '../../core/models';

@Component({
  selector: 'app-personal-records',
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
      LucideRepeat2,
      LucideTrophy,
      LucideWeight
    )
  ],
  templateUrl: './personal-records.component.html'
})
export class PersonalRecordsComponent implements OnInit {
  private readonly api = inject(ApiService);

  records: PersonalRecord[] = [];
  loading = true;
  error = '';

  ngOnInit(): void {
    this.api.personalRecords().subscribe({
      next: (records) => {
        this.records = records;
        this.loading = false;
      },
      error: () => {
        this.error = 'Personal records could not be loaded.';
        this.loading = false;
      }
    });
  }
}
