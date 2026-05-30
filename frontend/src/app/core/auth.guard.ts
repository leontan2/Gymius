import { inject } from '@angular/core';
import { CanActivateChildFn, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { map } from 'rxjs';
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
    map((user) => user ? true : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } }))
  );
};

export const authGuard: CanActivateFn = (_route, state) => checkAuth(state);
export const authChildGuard: CanActivateChildFn = (_route, state) => checkAuth(state);
