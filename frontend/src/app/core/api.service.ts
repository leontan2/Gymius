import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Dashboard,
  CsrfTokenResponse,
  MealAnalysis,
  NutritionEntry,
  NutritionEntryRequest,
  NutritionGoal,
  NutritionToday,
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
    return this.http.get<UserProfile>(`${this.baseUrl}/api/me`);
  }

  csrfToken(): Observable<CsrfTokenResponse> {
    return this.http.get<CsrfTokenResponse>(`${this.baseUrl}/api/csrf`);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/api/logout`, {});
  }

  dashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${this.baseUrl}/api/dashboard`);
  }

  workouts(): Observable<Workout[]> {
    return this.http.get<Workout[]>(`${this.baseUrl}/api/workouts`);
  }

  workout(id: string): Observable<Workout> {
    return this.http.get<Workout>(`${this.baseUrl}/api/workouts/${id}`);
  }

  createWorkout(payload: WorkoutRequest): Observable<Workout> {
    return this.http.post<Workout>(`${this.baseUrl}/api/workouts`, payload);
  }

  updateWorkout(id: string, payload: WorkoutRequest): Observable<Workout> {
    return this.http.put<Workout>(`${this.baseUrl}/api/workouts/${id}`, payload);
  }

  deleteWorkout(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/workouts/${id}`);
  }

  progress(): Observable<ProgressSeries[]> {
    return this.http.get<ProgressSeries[]>(`${this.baseUrl}/api/progress`);
  }

  personalRecords(): Observable<PersonalRecord[]> {
    return this.http.get<PersonalRecord[]>(`${this.baseUrl}/api/personal-records`);
  }

  nutritionToday(): Observable<NutritionToday> {
    return this.http.get<NutritionToday>(`${this.baseUrl}/api/nutrition/today`);
  }

  analyzeMealImage(image: File): Observable<MealAnalysis> {
    const body = new FormData();
    body.append('image', image);
    return this.http.post<MealAnalysis>(`${this.baseUrl}/api/nutrition/analyze-image`, body);
  }

  createNutritionEntry(payload: NutritionEntryRequest): Observable<NutritionEntry> {
    return this.http.post<NutritionEntry>(`${this.baseUrl}/api/nutrition/entries`, payload);
  }

  updateNutritionGoal(payload: NutritionGoal): Observable<NutritionGoal> {
    return this.http.put<NutritionGoal>(`${this.baseUrl}/api/nutrition/goals`, payload);
  }
}
