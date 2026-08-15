
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  HostListener,
  ViewChild,
  effect,
  inject,
  signal
} from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import {
  LucideDumbbell,
  LucideDynamicIcon,
  LucideHistory,
  LucideLayoutDashboard,
  LucideLogOut,
  LucideMenu,
  LucideMoon,
  LucidePlus,
  LucideSun,
  LucideTrendingUp,
  LucideTrophy,
  LucideUtensils,
  LucideUser,
  LucideX,
  provideLucideIcons
} from '@lucide/angular';
import { AuthService } from '../core/auth.service';
import { ThemeService } from '../core/theme.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    LucideDynamicIcon
],
  providers: [
    provideLucideIcons(
      LucideDumbbell,
      LucideHistory,
      LucideLayoutDashboard,
      LucideLogOut,
      LucideMenu,
      LucideMoon,
      LucidePlus,
      LucideSun,
      LucideTrendingUp,
      LucideTrophy,
      LucideUtensils,
      LucideUser,
      LucideX
    )
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.component.html'
})
export class ShellComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly mobileQuery = window.matchMedia('(max-width: 860px)');

  @ViewChild('sidebar') private sidebar?: ElementRef<HTMLElement>;
  @ViewChild('navToggle') private navToggle?: ElementRef<HTMLButtonElement>;
  @ViewChild('mainContent') private mainContent?: ElementRef<HTMLElement>;

  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);
  readonly navOpen = signal(false);
  readonly isMobile = signal(this.mobileQuery.matches);

  readonly navItems = [
    { label: 'Dashboard', path: '/dashboard', icon: 'layout-dashboard' },
    { label: 'Workouts', path: '/workouts', icon: 'history' },
    { label: 'Progress', path: '/progress', icon: 'trending-up' },
    { label: 'Records', path: '/records', icon: 'trophy' },
    { label: 'Nutrition', path: '/nutrition', icon: 'utensils' },
    { label: 'Profile', path: '/profile', icon: 'user' }
  ] as const;

  constructor() {
    const mediaListener = (event: MediaQueryListEvent): void => {
      this.isMobile.set(event.matches);
      this.navOpen.set(false);
    };
    this.mobileQuery.addEventListener('change', mediaListener);
    this.destroyRef.onDestroy(() => this.mobileQuery.removeEventListener('change', mediaListener));

    effect((onCleanup) => {
      if (this.navOpen() && this.isMobile()) {
        document.body.classList.add('nav-lock');
      }
      onCleanup(() => document.body.classList.remove('nav-lock'));
    });

    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.navOpen.set(false);
        queueMicrotask(() => this.mainContent?.nativeElement.focus({ preventScroll: true }));
      });
  }

  closeNav(restoreFocus = false): void {
    this.navOpen.set(false);
    if (restoreFocus) {
      queueMicrotask(() => this.navToggle?.nativeElement.focus());
    }
  }

  toggleNav(): void {
    const opening = !this.navOpen();
    this.navOpen.set(opening);
    if (opening) {
      queueMicrotask(() => this.sidebar?.nativeElement.querySelector<HTMLElement>('.new-workout')?.focus());
    }
  }

  @HostListener('document:keydown', ['$event'])
  handleDocumentKeydown(event: KeyboardEvent): void {
    if (!this.isMobile() || !this.navOpen()) {
      return;
    }

    if (event.key === 'Escape') {
      event.preventDefault();
      this.closeNav(true);
      return;
    }

    if (event.key !== 'Tab' || !this.sidebar) {
      return;
    }

    const focusable = Array.from(this.sidebar.nativeElement.querySelectorAll<HTMLElement>(
      'a[href], button:not(:disabled)'
    ));
    const first = focusable[0];
    const last = focusable.at(-1);

    if (!first || !last) {
      return;
    }

    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }
}
