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
      document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')
        ?.setAttribute('content', nextTheme === 'dark' ? '#07110f' : '#f4f7f5');

      try {
        localStorage.setItem('gymius-theme', nextTheme);
      } catch {
        // Theme persistence can be unavailable in privacy-restricted browser contexts.
      }
    });
  }

  toggle(): void {
    this.theme.update((theme) => theme === 'dark' ? 'light' : 'dark');
  }

  private initialTheme(): Theme {
    try {
      const savedTheme = localStorage.getItem('gymius-theme');
      if (savedTheme === 'light' || savedTheme === 'dark') {
        return savedTheme;
      }
    } catch {
      // Fall through to the operating-system preference.
    }

    return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  }
}
