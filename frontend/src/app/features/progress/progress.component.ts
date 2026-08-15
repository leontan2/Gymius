import { DecimalPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  effect,
  inject
} from '@angular/core';
import {
  CategoryScale,
  Chart,
  ChartData,
  ChartOptions,
  Legend,
  LinearScale,
  LineController,
  LineElement,
  PointElement,
  Tooltip
} from 'chart.js';
import { finalize } from 'rxjs';
import {
  LucideActivity,
  LucideBarChart3,
  LucideTrendingUp
} from '@lucide/angular';
import { ApiService } from '../../core/api.service';
import { apiErrorMessage } from '../../core/http-error';
import { ProgressPoint, ProgressSeries } from '../../core/models';
import { ThemeService } from '../../core/theme.service';

Chart.register(CategoryScale, LinearScale, LineController, LineElement, PointElement, Tooltip, Legend);

@Component({
  selector: 'app-progress',
  standalone: true,
  imports: [
    DecimalPipe,
    LucideActivity,
    LucideBarChart3,
    LucideTrendingUp
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './progress.component.html'
})
export class ProgressComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly theme = inject(ThemeService);
  private readonly colors = ['#3ddc97', '#f9b233', '#4cc9f0', '#f15bb5', '#a78bfa', '#ef4444'];
  private chart?: Chart<'line'>;
  private progressCanvas?: ElementRef<HTMLCanvasElement>;

  @ViewChild('progressCanvas')
  set progressCanvasRef(element: ElementRef<HTMLCanvasElement> | undefined) {
    this.progressCanvas = element;
    if (element) {
      queueMicrotask(() => this.renderChart());
    }
  }

  series: ProgressSeries[] = [];
  displayedSeries: ProgressSeries[] = [];
  loading = true;
  error = '';

  chartData: ChartData<'line'> = {
    labels: [],
    datasets: []
  };

  private readonly updateChartForTheme = effect(() => {
    this.theme.theme();
    if (this.chart) {
      this.chart.options = this.buildChartOptions();
      this.chart.update('none');
    }
  });

  ngOnInit(): void {
    this.loadProgress();
  }

  loadProgress(): void {
    this.loading = true;
    this.error = '';
    this.api.progress().pipe(finalize(() => {
      this.loading = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: (series) => {
        this.series = series;
        this.chartData = this.buildChartData(series);
        this.renderChart();
      },
      error: (error: unknown) => {
        this.error = apiErrorMessage(error, 'Progress data could not be loaded.');
      }
    });
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

  latestPoint(item: ProgressSeries): ProgressPoint | undefined {
    return item.points.at(-1);
  }

  firstPoint(item: ProgressSeries): ProgressPoint | undefined {
    return item.points[0];
  }

  weightChange(item: ProgressSeries): number {
    const first = this.firstPoint(item);
    const latest = this.latestPoint(item);
    return first && latest ? Number(latest.maxWeight) - Number(first.maxWeight) : 0;
  }

  trackSeries(_index: number, item: ProgressSeries): string {
    return item.exerciseName;
  }

  private buildChartData(series: ProgressSeries[]): ChartData<'line'> {
    const selected = series
      .filter((item) => item.points.length > 0)
      .sort((a, b) => b.points.length - a.points.length)
      .slice(0, 6);
    this.displayedSeries = selected;

    const labels = Array.from(new Set(selected.flatMap((item) => item.points.map((point) => point.date))))
      .sort();

    const weightsByDate = selected.map((item) => new Map(
      item.points.map((point) => [point.date, point.maxWeight])
    ));

    return {
      labels,
      datasets: selected.map((item, index) => ({
        label: item.exerciseName,
        data: labels.map((label) => weightsByDate[index].get(label) ?? null),
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
    if (!this.progressCanvas || !this.chartData.datasets.length) {
      return;
    }

    this.chart?.destroy();
    this.chart = new Chart(this.progressCanvas.nativeElement, {
      type: 'line',
      data: this.chartData,
      options: this.buildChartOptions()
    });
  }

  private buildChartOptions(): ChartOptions<'line'> {
    const styles = getComputedStyle(document.documentElement);
    const muted = styles.getPropertyValue('--muted').trim() || '#a8b5b0';
    const grid = styles.getPropertyValue('--border').trim() || 'rgba(168, 181, 176, 0.14)';

    return {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'index',
        intersect: false
      },
      plugins: {
        legend: {
          labels: {
            color: muted,
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
          ticks: { color: muted },
          grid: { color: grid }
        },
        y: {
          ticks: { color: muted },
          grid: { color: grid }
        }
      }
    };
  }
}
