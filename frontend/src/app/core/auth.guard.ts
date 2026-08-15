import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateChildFn, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

const checkAuth = (state: RouterStateSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  if (auth.hasLoaded()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }

  return auth.loadMe().pipe(
    map((user) => {
      if (!user) {
        return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
      }

      const returnUrl = auth.consumeReturnUrl();
      return returnUrl && returnUrl !== state.url ? router.parseUrl(returnUrl) : true;
    }),
    catchError(() => of(router.createUrlTree(['/login'], {
      queryParams: { returnUrl: state.url, sessionError: '1' }
    })))
  );
};

export const authGuard: CanActivateFn = (_route, state) => checkAuth(state);
export const authChildGuard: CanActivateChildFn = (_route, state) => checkAuth(state);

export const guestGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const returnUrl = safeReturnUrl(route.queryParamMap.get('returnUrl'));

  if (auth.isAuthenticated()) {
    return router.parseUrl(returnUrl);
  }

  if (auth.hasLoaded()) {
    return true;
  }

  return auth.loadMe().pipe(
    map((user) => user ? router.parseUrl(returnUrl) : true),
    catchError(() => of(true))
  );
};

function safeReturnUrl(value: string | null): string {
  return value?.startsWith('/') && !value.startsWith('//') && !value.startsWith('/login')
    ? value
    : '/dashboard';
}
