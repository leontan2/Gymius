import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { ApiService } from './api.service';
import { apiErrorMessage } from './http-error';
import { UserProfile } from './models';
import { SessionSecurityService } from './session-security.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly sessionSecurity = inject(SessionSecurityService);
  private readonly userState = signal<UserProfile | null | undefined>(undefined);
  private loadRequest: Observable<UserProfile | null> | null = null;

  readonly user = computed(() => this.sessionSecurity.sessionExpired() ? null : this.userState());
  readonly isAuthenticated = computed(() => Boolean(this.user()));
  readonly loggingOut = signal(false);
  readonly logoutError = signal('');

  loadMe(): Observable<UserProfile | null> {
    if (this.hasLoaded()) {
      return of(this.userState() ?? null);
    }

    if (this.loadRequest) {
      return this.loadRequest;
    }

    this.loadRequest = this.api.me().pipe(
      switchMap((user) => this.api.csrfToken().pipe(
        tap(({ token }) => this.sessionSecurity.setCsrfToken(token)),
        map(() => user)
      )),
      tap((user) => this.userState.set(user)),
      map((user) => user as UserProfile | null),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403)) {
          this.userState.set(null);
          this.sessionSecurity.clear();
          return of(null);
        }

        return throwError(() => error);
      }),
      finalize(() => {
        this.loadRequest = null;
      }),
      shareReplay({ bufferSize: 1, refCount: false })
    );

    return this.loadRequest;
  }

  hasLoaded(): boolean {
    return this.userState() !== undefined && !this.sessionSecurity.sessionExpired();
  }

  login(): void {
    this.logoutError.set('');
    this.sessionSecurity.clear();
    window.location.assign(this.api.googleLoginUrl());
  }

  rememberReturnUrl(url: string): void {
    if (!this.isSafeReturnUrl(url)) {
      return;
    }

    try {
      sessionStorage.setItem('gymius-return-url', url);
    } catch {
      // Navigation still falls back to the dashboard when storage is unavailable.
    }
  }

  consumeReturnUrl(): string | null {
    try {
      const url = sessionStorage.getItem('gymius-return-url');
      sessionStorage.removeItem('gymius-return-url');
      return url && this.isSafeReturnUrl(url) ? url : null;
    } catch {
      return null;
    }
  }

  logout(): void {
    if (this.loggingOut()) {
      return;
    }

    this.loggingOut.set(true);
    this.logoutError.set('');
    this.api.logout()
      .pipe(finalize(() => this.loggingOut.set(false)))
      .subscribe({
        next: () => this.finishLogout(),
        error: (error: unknown) => {
          if (error instanceof HttpErrorResponse && error.status === 401) {
            this.finishLogout();
            return;
          }

          this.logoutError.set(apiErrorMessage(error, 'Could not log out. Please try again.'));
        }
      });
  }

  private finishLogout(): void {
    this.userState.set(null);
    this.sessionSecurity.clear();
    void this.router.navigateByUrl('/login');
  }

  private isSafeReturnUrl(url: string): boolean {
    return url.startsWith('/') && !url.startsWith('//') && !url.startsWith('/login');
  }
}
