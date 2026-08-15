import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SessionSecurityService {
  private readonly csrfTokenState = signal<string | null>(null);
  private readonly sessionExpiredState = signal(false);

  readonly csrfToken = this.csrfTokenState.asReadonly();
  readonly sessionExpired = this.sessionExpiredState.asReadonly();

  setCsrfToken(token: string): void {
    this.csrfTokenState.set(token);
    this.sessionExpiredState.set(false);
  }

  markExpired(): void {
    this.csrfTokenState.set(null);
    this.sessionExpiredState.set(true);
  }

  clear(): void {
    this.csrfTokenState.set(null);
    this.sessionExpiredState.set(false);
  }
}
