import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
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
    CommonModule,
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
      LucideUser,
      LucideX
    )
  ],
  templateUrl: './shell.component.html'
})
export class ShellComponent {
  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);
  readonly navOpen = signal(false);

  readonly navItems = [
    { label: 'Dashboard', path: '/dashboard', icon: 'layout-dashboard' },
    { label: 'Workouts', path: '/workouts', icon: 'history' },
    { label: 'Progress', path: '/progress', icon: 'trending-up' },
    { label: 'Records', path: '/records', icon: 'trophy' },
    { label: 'Profile', path: '/profile', icon: 'user' }
  ];

  closeNav(): void {
    this.navOpen.set(false);
  }

  toggleNav(): void {
    this.navOpen.update((open) => !open);
  }
}
