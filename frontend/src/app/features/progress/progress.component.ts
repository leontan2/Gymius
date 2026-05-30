import { CommonModule, DecimalPipe } from '@angular/common';
import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { Chart, ChartData, ChartOptions, registerables } from 'chart.js';
import {
  LucideActivity,
  LucideBarChart3,
  LucideDynamicIcon,
  LucideTrendingUp,
  provideLucideIcons
} from '@lucide/angular';
import { ApiService } from '../../core/api.service';
import { ProgressSeries } from '../../core/models';

Chart.register(...registerables);

@Component({
  selector: 'app-progress',
  standalone: true,
  imports: [
    CommonModule,
    DecimalPipe,
    LucideDynamicIcon
  ],
  providers: [
    provideLucideIcons(
      LucideActivity,
      LucideBarChart3,
      LucideTrendingUp
    )
  ],
  templateUrl: './progress.component.html'
})
export class ProgressComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly colors = ['#3ddc97', '#f9b233', '#4cc9f0', '#f15bb5', '#a78bfa', '#ef4444'];
  private chart?: Chart<'line'>;
  private viewReady = false;

  @ViewChild('progressCanvas') private progressCanvas?: ElementRef<HTMLCanvasElement>;

  series: ProgressSeries[] = [];
  loading = true;
  error = '';

  chartData: ChartData<'line'> = {
    labels: [],
    datasets: []
  };

  chartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      mode: 'index',
      intersect: false
    },
    plugins: {
      legend: {
        labels: {
          color: '#a8b5b0',
          boxWidth: 10,
          usePointStyle: true
        }
      },
      tooltip: {
        callbacks: {
          label: (context) => `${context.dataset.label}: ${context.parsed.y} lb`
        }
      }
    },
    scales: {
      x: {
        ticks: { color: '#a8b5b0' },
        grid: { color: 'rgba(168, 181, 176, 0.12)' }
      },
      y: {
        ticks: { color: '#a8b5b0' },
        grid: { color: 'rgba(168, 181, 176, 0.12)' }
      }
    }
  };

  ngOnInit(): void {
    this.api.progress().subscribe({
      next: (series) => {
        this.series = series;
        this.chartData = this.buildChartData(series);
        this.renderChart();
        this.loading = false;
      },
      error: () => {
        this.error = 'Progress data could not be loaded.';
        this.loading = false;
      }
    });
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.renderChart();
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  get totalTrackedExercises(): number {
    return this.series.length;
  }

  get strongestLatestLift(): number {
    return this.series
      .flatMap((item) => item.points)
      .reduce((highest, point) => Math.max(highest, Number(point.maxWeight)), 0);
  }

  private buildChartData(series: ProgressSeries[]): ChartData<'line'> {
    const selected = series
      .filter((item) => item.points.length > 0)
      .sort((a, b) => b.points.length - a.points.length)
      .slice(0, 6);

    const labels = Array.from(new Set(selected.flatMap((item) => item.points.map((point) => point.date))))
      .sort();

    return {
      labels,
      datasets: selected.map((item, index) => ({
        label: item.exerciseName,
        data: labels.map((label) => item.points.find((point) => point.date === label)?.maxWeight ?? null),
        borderColor: this.colors[index % this.colors.length],
        backgroundColor: this.colors[index % this.colors.length],
        borderWidth: 3,
        tension: 0.35,
        pointRadius: 4,
        pointHoverRadius: 6,
        spanGaps: true
      }))
    };
  }

  private renderChart(): void {
    if (!this.viewReady || !this.progressCanvas || !this.chartData.datasets.length) {
      return;
    }

    this.chart?.destroy();
    this.chart = new Chart(this.progressCanvas.nativeElement, {
      type: 'line',
      data: this.chartData,
      options: this.chartOptions
    });
  }
}
