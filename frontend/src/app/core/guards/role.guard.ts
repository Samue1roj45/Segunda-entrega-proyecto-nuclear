import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { Rol } from '../models/usuario.model';

/**
 * Permite el acceso a la ruta solo si el rol del usuario está incluido en
 * `route.data['roles']`. Si no, redirige al dashboard que corresponda a su rol
 * (o a /auth si no hay sesión).
 *
 * Ejemplo de uso en una ruta:
 *   { path: 'admin', loadChildren: ..., canActivate: [authGuard, roleGuard], data: { roles: [Rol.Admin] } }
 */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const allowed = (route.data['roles'] as Rol[] | undefined) ?? [];

  if (allowed.length === 0) {
    return true;
  }

  if (auth.hasRol(...allowed)) {
    return true;
  }

  // Si está autenticado pero con rol equivocado, lo enviamos a su dashboard natural.
  const rol = auth.rol();
  if (rol) {
    console.warn('[RoleGuard] Rol no permitido en ruta; redirige a dashboard', {
      rol,
      allowed,
    });
    return router.createUrlTree([auth.dashboardDeRol(rol)]);
  }

  console.warn('[RoleGuard] Sin rol en sesión → /auth/login', { allowed });
  return router.createUrlTree(['/auth/login']);
};
