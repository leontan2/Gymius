import { Component, inject } from '@angular/core';
import {
  LucideActivity,
  LucideArrowRight,
  LucideBarChart3,
  LucideDumbbell,
  LucideDynamicIcon,
  LucideLogIn,
  LucideShieldCheck,
  provideLucideIcons
} from '@lucide/angular';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    LucideDynamicIcon
  ],
  providers: [
    provideLucideIcons(
      LucideActivity,
      LucideArrowRight,
      LucideBarChart3,
      LucideDumbbell,
      LucideLogIn,
      LucideShieldCheck
    )
  ],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  private readonly auth = inject(AuthService);

  login(): void {
    this.auth.login();
  }
}
