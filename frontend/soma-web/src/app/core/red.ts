import { HttpErrorResponse, HttpEvent, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { concat, of, retry, tap, timer } from 'rxjs';
import { RegistroErroresService } from './registro-errores.service';
import { ToastService } from './toast.service';

/** Estado de la conexión del navegador, con aviso al perderla y al recuperarla. */
@Injectable({ providedIn: 'root' })
export class ConexionService {
  private readonly toasts = inject(ToastService);
  readonly enLinea = signal(typeof navigator === 'undefined' ? true : navigator.onLine);
  constructor() {
    if (typeof window === 'undefined') return;
    window.addEventListener('offline', () => { this.enLinea.set(false); this.toasts.error('Sin conexión. Verás los últimos datos guardados.'); });
    window.addEventListener('online', () => { this.enLinea.set(true); this.toasts.ok('Conexión restablecida.'); });
  }
}

/** Las lecturas se reintentan solas (hasta 2 veces) si el servidor no responde o está arrancando. */
export const reintentoInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.method !== 'GET') return next(req);
  return next(req).pipe(retry({ count: 2, delay: (e: HttpErrorResponse, n) => ([0, 502, 503, 504].includes(e.status) ? timer(500 * n) : (() => { throw e; })()) }));
};

const LISTAS = [/^\/api\/casos(\?|$)/, /^\/api\/sesiones(\?|$)/, /^\/api\/estudiantes$/, /^\/api\/autorizaciones$/, /^\/api\/notificaciones(\?|$)/];
const memoria = new Map<string, HttpResponse<unknown>>();
export function limpiarCache(): void { memoria.clear(); }

/**
 * «Primero lo guardado, luego lo nuevo»: en las listas, si ya se pidieron antes, se muestran al instante
 * y se refrescan por detrás. Cualquier cambio (POST, PUT, DELETE) vacía la memoria para no mostrar datos viejos.
 */
export const cacheInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.method !== 'GET') return next(req).pipe(tap((ev) => { if (ev instanceof HttpResponse) memoria.clear(); }));
  const ruta = req.urlWithParams.replace(/^https?:\/\/[^/]+/, '');
  if (!LISTAS.some((r) => r.test(ruta))) return next(req);
  const clave = `${req.headers.get('Authorization') ?? ''}|${ruta}`;
  const red$ = next(req).pipe(tap((ev: HttpEvent<unknown>) => { if (ev instanceof HttpResponse) memoria.set(clave, ev); }));
  const guardada = memoria.get(clave);
  return guardada ? concat(of(guardada), red$) : red$;
};

/** Deja constancia de los fallos del servidor (5xx) para el informe de soporte. */
export const registroHttpInterceptor: HttpInterceptorFn = (req, next) => {
  const registro = inject(RegistroErroresService);
  return next(req).pipe(tap({ error: (e: HttpErrorResponse) => { if (e.status >= 500 || e.status === 0) registro.registrar('servidor', `${req.method} ${req.url} → ${e.status}`, e.error?.mensaje); } }));
};
