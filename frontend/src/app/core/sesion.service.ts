import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { Permiso } from './modelos/permiso';
import { SesionUsuario } from './modelos/sesion-usuario';

const CLAVE_ALMACEN = 'simulapsi.sesion';

/** Consume el inicio de sesión del Módulo 1. No autentica por su cuenta: la decisión es del servidor. */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly http = inject(HttpClient);
  private readonly actual = signal<SesionUsuario | null>(leerAlmacen());

  readonly sesion = this.actual.asReadonly();
  readonly puedeGestionarCasos = computed(
    () => this.actual()?.permisos.includes(Permiso.GestionarCasos) ?? false,
  );

  iniciar(correoInstitucional: string, clave: string): Observable<SesionUsuario> {
    return this.http
      .post<SesionUsuario>('/api/auth/personal/login', { correoInstitucional, clave })
      .pipe(
        tap((sesion) => {
          sessionStorage.setItem(CLAVE_ALMACEN, JSON.stringify(sesion));
          this.actual.set(sesion);
        }),
      );
  }

  cerrar(): void {
    sessionStorage.removeItem(CLAVE_ALMACEN);
    this.actual.set(null);
  }
}

function leerAlmacen(): SesionUsuario | null {
  try {
    const guardada = sessionStorage.getItem(CLAVE_ALMACEN);
    return guardada ? (JSON.parse(guardada) as SesionUsuario) : null;
  } catch {
    return null;
  }
}
