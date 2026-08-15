import { provideHttpClient, withInterceptors, withXhr } from '@angular/common/http';
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { apiInterceptor } from './core/api.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection(),
    provideHttpClient(withXhr(), withInterceptors([apiInterceptor])),
    provideRouter(routes)
  ]
};
