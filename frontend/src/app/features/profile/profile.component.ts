import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import {
  LucideDynamicIcon,
  LucideMail,
  LucideShieldCheck,
  LucideUser,
  provideLucideIcons
} from '@lucide/angular';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    LucideDynamicIcon
  ],
  providers: [
    provideLucideIcons(
      LucideMail,
      LucideShieldCheck,
      LucideUser
    )
  ],
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
