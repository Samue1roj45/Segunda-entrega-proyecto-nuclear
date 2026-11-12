import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { Permiso } from './modelos/permiso';
import { SesionService } from './sesion.service';

/** Envía el token a la API y, si el servidor ya no reconoce la sesión, vuelve al inicio de sesión. */
export const autenticacionInterceptor: HttpInterceptorFn = (solicitud, siguiente) => {
  const sesiones = inject(SesionService);
  const router = inject(Router);
  const token = sesiones.sesion()?.token;
  const conToken = token
    ? solicitud.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : solicitud;

  return siguiente(conToken).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && token) {
        sesiones.cerrar();
        void router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};

/** Exige una sesión iniciada; si no la hay, lleva al inicio de sesión (CP-RF02-06). */
export const guardiaSesion: CanActivateFn = () =>
  inject(SesionService).autenticado() || inject(Router).createUrlTree(['/login']);

/**
 * Exige un permiso para abrir la pantalla (RF-02). Sin sesión lleva al inicio de sesión y, con sesión
 * pero sin el permiso, a la página de acceso denegado. Solo muestra u oculta pantallas: la autorización
 * real la aplica el servidor, que responde 403 aunque alguien salte esta guardia.
 */
export function guardiaPermiso(permiso: Permiso): CanActivateFn {
  return () => {
    const sesiones = inject(SesionService);
    const router = inject(Router);
    if (!sesiones.autenticado()) {
      return router.createUrlTree(['/login']);
    }
    return sesiones.tienePermiso(permiso) || router.createUrlTree(['/acceso-denegado']);
  };
}
