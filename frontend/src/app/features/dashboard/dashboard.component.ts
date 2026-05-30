import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideArrowUpRight,
  LucideCalendarDays,
  LucideDumbbell,
  LucideDynamicIcon,
  LucidePlus,
  LucideScale,
  LucideTrophy,
  provideLucideIcons
} from '@lucide/angular';
import { ApiService } from '../../core/api.service';
import { Dashboard } from '../../core/models';

@Component({
  selector: 'app-dashboard',
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
      LucideArrowUpRight,
      LucideCalendarDays,
      LucideDumbbell,
      LucidePlus,
      LucideScale,
      LucideTrophy
    )
  ],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  private readonly api = inject(ApiService);

  dashboard?: Dashboard;
  loading = true;
  error = '';

  ngOnInit(): void {
    this.api.dashboard().subscribe({
      next: (dashboard) => {
        this.dashboard = dashboard;
        this.loading = false;
      },
      error: () => {
        this.error = 'Dashboard data could not be loaded.';
        this.loading = false;
      }
    });
  }
}
