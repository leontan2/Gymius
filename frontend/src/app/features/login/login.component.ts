import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import {
  LucideActivity,
  LucideArrowRight,
  LucideBarChart3,
  LucideDumbbell,
  LucideLogIn,
  LucideShieldCheck
} from '@lucide/angular';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    LucideActivity,
    LucideArrowRight,
    LucideBarChart3,
    LucideDumbbell,
    LucideLogIn,
    LucideShieldCheck
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.component.html'
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly devAuthBypassEnabled = environment.devAuthBypassEnabled;
  readonly returnUrl = signal('/dashboard');
  readonly sessionError = signal(false);

  constructor() {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed())
      .subscribe((params) => {
        this.returnUrl.set(this.safeReturnUrl(params.get('returnUrl')));
        this.sessionError.set(params.get('sessionError') === '1');
      });
  }

  login(): void {
    this.auth.rememberReturnUrl(this.returnUrl());
    this.auth.login();
  }

  continueLocally(): void {
    void this.router.navigateByUrl(this.returnUrl());
  }

  retrySession(): void {
    void this.router.navigateByUrl(this.returnUrl());
  }

  private safeReturnUrl(value: string | null): string {
    return value?.startsWith('/') && !value.startsWith('//') && !value.startsWith('/login')
      ? value
      : '/dashboard';
  }
}
