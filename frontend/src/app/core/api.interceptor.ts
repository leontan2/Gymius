import {
  HttpBackend,
  HttpClient,
  HttpContextToken,
  HttpErrorResponse,
  HttpInterceptorFn
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { CsrfTokenResponse } from './models';
import { SessionSecurityService } from './session-security.service';

const apiBaseUrl = environment.apiUrl.replace(/\/$/, '');
const safeMethods = new Set(['GET', 'HEAD', 'OPTIONS']);
const csrfRetried = new HttpContextToken(() => false);
const authBootstrapUrls = new Set([
  `${apiBaseUrl}/api/me`,
  `${apiBaseUrl}/api/csrf`,
  `${apiBaseUrl}/api/logout`
]);

export const apiInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.url !== apiBaseUrl && !request.url.startsWith(`${apiBaseUrl}/`)) {
    return next(request);
  }

  const sessionSecurity = inject(SessionSecurityService);
  const router = inject(Router, { optional: true });
  const httpBackend = inject(HttpBackend, { optional: true });
  const csrfToken = sessionSecurity.csrfToken();
  const setHeaders: Record<string, string> = {};
  const isUnsafe = !safeMethods.has(request.method.toUpperCase());

  if (csrfToken && isUnsafe) {
    setHeaders['X-CSRF-TOKEN'] = csrfToken;
  }

  const apiRequest = request.clone({
    withCredentials: true,
    setHeaders
  });

  return next(apiRequest).pipe(catchError((error: unknown) => {
    if (!(error instanceof HttpErrorResponse)) {
      return throwError(() => error);
    }

    if (error.status === 401 && !authBootstrapUrls.has(request.url)) {
      expireSession(sessionSecurity, router);
    }

    if (
      error.status === 403
      && isUnsafe
      && !request.context.get(csrfRetried)
      && httpBackend
    ) {
      const directHttp = new HttpClient(httpBackend);
      return directHttp.get<CsrfTokenResponse>(`${apiBaseUrl}/api/csrf`, {
        withCredentials: true
      }).pipe(
        catchError((refreshError: unknown) => {
          if (refreshError instanceof HttpErrorResponse && refreshError.status === 401) {
            expireSession(sessionSecurity, router);
          }
          return throwError(() => refreshError);
        }),
        tap(({ token }) => sessionSecurity.setCsrfToken(token)),
        switchMap(({ token }) => next(request.clone({
          withCredentials: true,
          context: request.context.set(csrfRetried, true),
          setHeaders: { 'X-CSRF-TOKEN': token }
        })).pipe(catchError((retryError: unknown) => {
          if (retryError instanceof HttpErrorResponse && retryError.status === 401) {
            expireSession(sessionSecurity, router);
          }
          return throwError(() => retryError);
        })))
      );
    }

    return throwError(() => error);
  }));
};

function expireSession(sessionSecurity: SessionSecurityService, router: Router | null): void {
  sessionSecurity.markExpired();
  if (!router || router.url.startsWith('/login')) {
    return;
  }

  const returnUrl = router.url.startsWith('/') && !router.url.startsWith('//')
    ? router.url
    : '/dashboard';
  void router.navigate(['/login'], {
    queryParams: { returnUrl, sessionError: '1' }
  });
}
