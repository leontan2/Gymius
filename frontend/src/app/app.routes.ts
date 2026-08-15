import { Routes } from '@angular/router';
import { authChildGuard, authGuard, guestGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component')
      .then((module) => module.LoginComponent),
    canActivate: [guestGuard],
    title: 'Sign in | Gymius'
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell.component')
      .then((module) => module.ShellComponent),
    canActivate: [authGuard],
    canActivateChild: [authChildGuard],
    children: [
      {
        path: 'dashboard',
        title: 'Dashboard | Gymius',
        loadComponent: () => import('./features/dashboard/dashboard.component')
          .then((module) => module.DashboardComponent)
      },
      {
        path: 'workouts',
        title: 'Workout history | Gymius',
        loadComponent: () => import('./features/workouts/workout-history.component')
          .then((module) => module.WorkoutHistoryComponent)
      },
      {
        path: 'workouts/new',
        title: 'New workout | Gymius',
        loadComponent: () => import('./features/workouts/workout-editor.component')
          .then((module) => module.WorkoutEditorComponent)
      },
      {
        path: 'workouts/:id/edit',
        title: 'Edit workout | Gymius',
        loadComponent: () => import('./features/workouts/workout-editor.component')
          .then((module) => module.WorkoutEditorComponent)
      },
      {
        path: 'progress',
        title: 'Progress | Gymius',
        loadComponent: () => import('./features/progress/progress.component')
          .then((module) => module.ProgressComponent)
      },
      {
        path: 'records',
        title: 'Personal records | Gymius',
        loadComponent: () => import('./features/records/personal-records.component')
          .then((module) => module.PersonalRecordsComponent)
      },
      {
        path: 'nutrition',
        title: 'Nutrition | Gymius',
        loadComponent: () => import('./features/nutrition/nutrition.component')
          .then((module) => module.NutritionComponent)
      },
      {
        path: 'profile',
        title: 'Profile | Gymius',
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
