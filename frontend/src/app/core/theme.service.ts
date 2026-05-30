import { Injectable, effect, signal } from '@angular/core';

type Theme = 'dark' | 'light';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  readonly theme = signal<Theme>(this.initialTheme());

  constructor() {
    effect(() => {
      const nextTheme = this.theme();
      document.documentElement.setAttribute('data-theme', nextTheme);
      localStorage.setItem('gymius-theme', nextTheme);
    });
  }

  toggle(): void {
    this.theme.update((theme) => theme === 'dark' ? 'light' : 'dark');
  }

  private initialTheme(): Theme {
    const savedTheme = localStorage.getItem('gymius-theme');
    return savedTheme === 'light' ? 'light' : 'dark';
  }
}
