import { HttpBackend, HttpErrorResponse, HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { apiInterceptor } from './api.interceptor';
import { SessionSecurityService } from './session-security.service';

describe('apiInterceptor', () => {
  let security: SessionSecurityService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{
        provide: HttpBackend,
        useValue: {
          handle: () => of(new HttpResponse({ body: { token: 'refreshed-csrf-token' } }))
        }
      }]
    });
    security = TestBed.inject(SessionSecurityService);
    security.setCsrfToken('test-csrf-token');
  });

  it('adds credentials and the CSRF token to unsafe API requests', () => {
    const request = intercept(new HttpRequest('POST', `${environment.apiUrl}/api/workouts`, {}));

    expect(request.withCredentials).toBe(true);
    expect(request.headers.get('X-CSRF-TOKEN')).toBe('test-csrf-token');
  });

  it('does not add a CSRF header to safe API requests', () => {
    const request = intercept(new HttpRequest('GET', `${environment.apiUrl}/api/workouts`));

    expect(request.withCredentials).toBe(true);
    expect(request.headers.has('X-CSRF-TOKEN')).toBe(false);
  });

  it('does not modify requests to other origins', () => {
    const original = new HttpRequest('POST', 'https://example.com/metrics', {});

    expect(intercept(original)).toBe(original);
  });

  it('marks the session expired after a protected API request returns 401', () => {
    const request = new HttpRequest('GET', `${environment.apiUrl}/api/workouts`);

    TestBed.runInInjectionContext(() => {
      apiInterceptor(request, () => throwError(() => new HttpErrorResponse({ status: 401 })))
        .subscribe({ error: () => undefined });
    });

    expect(security.sessionExpired()).toBe(true);
    expect(security.csrfToken()).toBeNull();
  });

  it('refreshes the CSRF token and retries one rejected mutation', () => {
    const forwarded: HttpRequest<unknown>[] = [];
    const request = new HttpRequest('POST', `${environment.apiUrl}/api/workouts`, {});

    TestBed.runInInjectionContext(() => {
      apiInterceptor(request, forwardedRequest => {
        forwarded.push(forwardedRequest);
        return forwarded.length === 1
          ? throwError(() => new HttpErrorResponse({ status: 403 }))
          : of(new HttpResponse());
      }).subscribe();
    });

    expect(forwarded).toHaveLength(2);
    expect(forwarded[1].headers.get('X-CSRF-TOKEN')).toBe('refreshed-csrf-token');
    expect(security.csrfToken()).toBe('refreshed-csrf-token');
  });

  it('returns the retried mutation error after refreshing CSRF', () => {
    let attempts = 0;
    let receivedError: HttpErrorResponse | undefined;
    const request = new HttpRequest('POST', `${environment.apiUrl}/api/workouts`, {});

    TestBed.runInInjectionContext(() => {
      apiInterceptor(request, () => {
        attempts++;
        return throwError(() => new HttpErrorResponse({ status: attempts === 1 ? 403 : 422 }));
      }).subscribe({
        error: (error: HttpErrorResponse) => {
          receivedError = error;
        }
      });
    });

    expect(attempts).toBe(2);
    expect(receivedError?.status).toBe(422);
  });
});

function intercept(original: HttpRequest<unknown>): HttpRequest<unknown> {
  let captured: HttpRequest<unknown> | undefined;

  TestBed.runInInjectionContext(() => {
    apiInterceptor(original, request => {
      captured = request;
      return of(new HttpResponse());
    }).subscribe();
  });

  if (!captured) {
    throw new Error('The interceptor did not forward the request.');
  }

  return captured;
}
