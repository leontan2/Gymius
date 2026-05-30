import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Dashboard,
  ExerciseLog,
  PersonalRecord,
  ProgressSeries,
  UserProfile,
  Workout,
  WorkoutRequest
} from './models';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl.replace(/\/$/, '');

  googleLoginUrl(): string {
    return `${this.baseUrl}/oauth2/authorization/google`;
  }

  me(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.baseUrl}/api/me`, { withCredentials: true });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/api/logout`, {}, { withCredentials: true });
  }

  dashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${this.baseUrl}/api/dashboard`, { withCredentials: true });
  }

  workouts(): Observable<Workout[]> {
    return this.http.get<Workout[]>(`${this.baseUrl}/api/workouts`, { withCredentials: true });
  }

  workout(id: string): Observable<Workout> {
    return this.http.get<Workout>(`${this.baseUrl}/api/workouts/${id}`, { withCredentials: true });
  }

  createWorkout(payload: WorkoutRequest): Observable<Workout> {
    return this.http.post<Workout>(`${this.baseUrl}/api/workouts`, payload, { withCredentials: true });
  }

  updateWorkout(id: string, payload: WorkoutRequest): Observable<Workout> {
    return this.http.put<Workout>(`${this.baseUrl}/api/workouts/${id}`, payload, { withCredentials: true });
  }

  deleteWorkout(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/workouts/${id}`, { withCredentials: true });
  }

  addExercise(workoutId: string, payload: ExerciseLog): Observable<ExerciseLog> {
    return this.http.post<ExerciseLog>(`${this.baseUrl}/api/workouts/${workoutId}/exercises`, payload, {
      withCredentials: true
    });
  }

  updateExercise(workoutId: string, exerciseId: string, payload: ExerciseLog): Observable<ExerciseLog> {
    return this.http.put<ExerciseLog>(`${this.baseUrl}/api/workouts/${workoutId}/exercises/${exerciseId}`, payload, {
      withCredentials: true
    });
  }

  deleteExercise(workoutId: string, exerciseId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/workouts/${workoutId}/exercises/${exerciseId}`, {
      withCredentials: true
    });
  }

  progress(): Observable<ProgressSeries[]> {
    return this.http.get<ProgressSeries[]>(`${this.baseUrl}/api/progress`, { withCredentials: true });
  }

  personalRecords(): Observable<PersonalRecord[]> {
    return this.http.get<PersonalRecord[]>(`${this.baseUrl}/api/personal-records`, { withCredentials: true });
  }
}
