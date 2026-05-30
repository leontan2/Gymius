import { Routes } from '@angular/router';
import { authChildGuard, authGuard } from './core/auth.guard';
import { LoginComponent } from './features/login/login.component';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    canActivateChild: [authChildGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component')
          .then((module) => module.DashboardComponent)
      },
      {
        path: 'workouts',
        loadComponent: () => import('./features/workouts/workout-history.component')
          .then((module) => module.WorkoutHistoryComponent)
      },
      {
        path: 'workouts/new',
        loadComponent: () => import('./features/workouts/workout-editor.component')
          .then((module) => module.WorkoutEditorComponent)
      },
      {
        path: 'workouts/:id/edit',
        loadComponent: () => import('./features/workouts/workout-editor.component')
          .then((module) => module.WorkoutEditorComponent)
      },
      {
        path: 'progress',
        loadComponent: () => import('./features/progress/progress.component')
          .then((module) => module.ProgressComponent)
      },
      {
        path: 'records',
        loadComponent: () => import('./features/records/personal-records.component')
          .then((module) => module.PersonalRecordsComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/profile/profile.component')
          .then((module) => module.ProfileComponent)
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
