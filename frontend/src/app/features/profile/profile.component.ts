import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { LucideMail, LucideShieldCheck, LucideUser } from '@lucide/angular';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    LucideMail,
    LucideShieldCheck,
    LucideUser
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit {
  readonly auth = inject(AuthService);

  ngOnInit(): void {
    if (!this.auth.hasLoaded()) {
      this.auth.loadMe().subscribe();
    }
  }
}
