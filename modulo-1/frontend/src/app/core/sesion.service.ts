import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { Permiso } from './modelos/permiso';
import { Rol } from './modelos/rol';
import { SesionUsuario } from './modelos/sesion-usuario';

const CLAVE_ALMACEN = 'soma.sesion';

/**
 * Sesión del usuario (Módulo 1). Consume los inicios de sesión del backend; no autentica por su cuenta:
 * la decisión es del servidor (RF-01) y cada permiso se vuelve a verificar en el servidor (RF-02).
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly http = inject(HttpClient);
  private readonly actual = signal<SesionUsuario | null>(leerAlmacen());

  readonly sesion = this.actual.asReadonly();
  readonly autenticado = computed(() => this.actual() !== null);
  readonly rol = computed(() => this.actual()?.rol ?? null);
  readonly esAdministrador = computed(() => this.rol() === Rol.Administrador);
  readonly puedeGestionarCasos = computed(() => this.tienePermiso(Permiso.GestionarCasos));

  /** RF-01: ingreso del estudiante con su correo institucional y el código de autorización. */
  iniciarEstudiante(correoInstitucional: string, codigoAutorizacion: string): Observable<SesionUsuario> {
    return this.http
      .post<SesionUsuario>('/api/auth/estudiantes/login', { correoInstitucional, codigoAutorizacion })
      .pipe(tap((sesion) => this.guardar(sesion)));
  }

  /** Ingreso de docentes y administrador con correo y clave. */
  iniciar(correoInstitucional: string, clave: string): Observable<SesionUsuario> {
    return this.http
      .post<SesionUsuario>('/api/auth/personal/login', { correoInstitucional, clave })
      .pipe(tap((sesion) => this.guardar(sesion)));
  }

  /** Solo decide qué se muestra; el servidor responde 403 si el permiso no es válido. */
  tienePermiso(permiso: Permiso): boolean {
    return this.actual()?.permisos.includes(permiso) ?? false;
  }

  cerrar(): void {
    try {
      sessionStorage.removeItem(CLAVE_ALMACEN);
    } catch {
      // Sin almacenamiento disponible: basta con olvidar la sesión en memoria.
    }
    this.actual.set(null);
  }

  private guardar(sesion: SesionUsuario): void {
    try {
      sessionStorage.setItem(CLAVE_ALMACEN, JSON.stringify(sesion));
    } catch {
      // La sesión sigue activa en memoria aunque no se pueda guardar.
    }
    this.actual.set(sesion);
  }
}

function leerAlmacen(): SesionUsuario | null {
  try {
    const guardada = sessionStorage.getItem(CLAVE_ALMACEN);
    if (!guardada) {
      return null;
    }
    const sesion = JSON.parse(guardada) as SesionUsuario;
    return vencida(sesion) ? null : sesion;
  } catch {
    return null;
  }
}

function vencida(sesion: SesionUsuario): boolean {
  return !!sesion.expiraEn && new Date(sesion.expiraEn).getTime() <= Date.now();
}
