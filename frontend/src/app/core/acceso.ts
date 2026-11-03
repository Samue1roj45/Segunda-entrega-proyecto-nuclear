import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
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

/** Solo muestra u oculta pantallas; la autorización real la aplica el servidor (RF-02). */
export const guardiaDocente: CanActivateFn = () => {
  const sesiones = inject(SesionService);
  return sesiones.puedeGestionarCasos() || inject(Router).createUrlTree(['/login']);
};
