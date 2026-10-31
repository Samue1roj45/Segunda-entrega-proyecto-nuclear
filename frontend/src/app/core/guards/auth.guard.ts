import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';

/**
 * Permite el acceso a la ruta solo si hay un usuario autenticado.
 * Si no, redirige a /auth/login.
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  console.warn('[AuthGuard] Acceso denegado → /auth/login', {
    hasToken: !!auth.getAccessToken(),
    rol: auth.rol(),
  });
  return router.createUrlTree(['/auth/login']);
};
