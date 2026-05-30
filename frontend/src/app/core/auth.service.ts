import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, map, of, tap } from 'rxjs';
import { ApiService } from './api.service';
import { UserProfile } from './models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly userState = signal<UserProfile | null | undefined>(undefined);

  readonly user = computed(() => this.userState());
  readonly isAuthenticated = computed(() => Boolean(this.userState()));

  loadMe() {
    return this.api.me().pipe(
      tap((user) => this.userState.set(user)),
      map((user) => user as UserProfile | null),
      catchError(() => {
        this.userState.set(null);
        return of(null);
      })
    );
  }

  hasLoaded(): boolean {
    return this.userState() !== undefined;
  }

  login(): void {
    window.location.href = this.api.googleLoginUrl();
  }

  logout(): void {
    this.api.logout()
      .pipe(finalize(() => this.userState.set(null)))
      .subscribe({
        next: () => this.router.navigateByUrl('/login'),
        error: () => this.router.navigateByUrl('/login')
      });
  }
}
