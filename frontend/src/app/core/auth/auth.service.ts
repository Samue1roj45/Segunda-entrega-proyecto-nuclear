import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, of, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { JwtPayload, Rol, TokenPair, Usuario } from '../models/usuario.model';

const ACCESS_KEY = 'simulador.access';
const REFRESH_KEY = 'simulador.refresh';
const USER_KEY = 'simulador.user';

/** Respuesta del backend para login/registro: tokens + usuario. */
interface AuthResponse extends TokenPair {
  usuario: Usuario;
}

/** Datos para el registro autónomo de un docente. */
export interface RegistroDocenteData {
  username: string;
  email: string;
  first_name: string;
  last_name: string;
  password: string;
  password_confirm: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  /** Usuario actualmente autenticado (signal reactiva). null si no hay sesión. */
  private readonly _usuario = signal<Usuario | null>(this.loadUserFromStorage());
  readonly usuario = this._usuario.asReadonly();

  /**
   * True si hay access token vigente.
   * Debe ser método (no computed): localStorage no es reactivo y un computed
   * quedaría cacheado en false tras la primera visita a rutas públicas.
   */
  isAuthenticated(): boolean {
    const token = this.getAccessToken();
    return token !== null && !this.isTokenExpired(token);
  }

  /** Rol del usuario actual (null si no hay sesión). */
  readonly rol = computed(() => this._usuario()?.rol ?? null);

  // ---------- Login docente/administrador (username + password) ----------

  loginDocente(username: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/token/`, { username, password })
      .pipe(tap((res) => this.handleAuthSuccess(res)));
  }

  // ---------- Registro autónomo de docente ----------

  registroDocente(data: RegistroDocenteData): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/registro-docente/`, data)
      .pipe(tap((res) => this.handleAuthSuccess(res)));
  }

  // ---------- Acceso estudiante (correo + código) — backend pendiente ----------

  loginEstudiante(correo: string, codigo: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/estudiante-acceso/`, { correo, codigo })
      .pipe(tap((res) => this.handleAuthSuccess(res)));
  }

  // ---------- Logout ----------

  logout(): Observable<unknown> {
    const refresh = this.getRefreshToken();
    const request$ = refresh
      ? this.http
          .post(`${environment.apiUrl}/auth/logout/`, { refresh })
          .pipe(catchError(() => of(null))) // si falla en el backend, igual hacemos logout local
      : of(null);

    return request$.pipe(
      tap(() => {
        this.clearLocalSession();
        this.router.navigate(['/auth/login']);
      }),
    );
  }

  /** Limpia tokens y usuario en memoria/storage sin llamar al backend. */
  clearLocalSession(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem('simulador.practica_activa');
    this._usuario.set(null);
    console.warn('[Auth] Sesión local eliminada');
  }

  /** Diagnóstico en consola (hotfix login estudiante). */
  logSessionState(context: string): void {
    const token = this.getAccessToken();
    const expired = token ? this.isTokenExpired(token) : null;
    console.info('[Auth]', context, {
      hasAccessToken: !!token,
      accessExpired: expired,
      rol: this.rol(),
      userId: this._usuario()?.id ?? null,
    });
  }

  /** Establece una sesión ya validada por otro flujo de autenticación. */
  setSession(access: string, refresh: string, usuario: Usuario): void {
    const normalized: Usuario = {
      ...usuario,
      rol: this.normalizeRol(usuario.rol),
    };
    localStorage.setItem(ACCESS_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
    localStorage.setItem(USER_KEY, JSON.stringify(normalized));
    this._usuario.set(normalized);
    this.logSessionState('setSession');
  }

  // ---------- Tokens ----------

  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  refreshAccessToken(): Observable<{ access: string; refresh?: string }> {
    const refresh = this.getRefreshToken();
    return this.http
      .post<{ access: string; refresh?: string }>(
        `${environment.apiUrl}/auth/token/refresh/`,
        { refresh },
      )
      .pipe(
        tap((res) => {
          localStorage.setItem(ACCESS_KEY, res.access);
          if (res.refresh) {
            // Con ROTATE_REFRESH_TOKENS=True el backend rota el refresh.
            localStorage.setItem(REFRESH_KEY, res.refresh);
          }
        }),
      );
  }

  // ---------- Helpers ----------

  hasRol(...roles: Rol[]): boolean {
    const actual = this.rol();
    return actual !== null && roles.includes(actual);
  }

  /** Dashboard inicial según rol — útil para el redirect post-login. */
  dashboardDeRol(rol: Rol | null): string {
    if (rol === Rol.Admin) return '/admin';
    if (rol === Rol.Estudiante) return '/estudiante';
    return '/docente'; // default y fallback
  }

  private handleAuthSuccess(res: AuthResponse): void {
    this.setSession(res.access, res.refresh, res.usuario);
  }

  private loadUserFromStorage(): Usuario | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      const user = JSON.parse(raw) as Usuario;
      return { ...user, rol: this.normalizeRol(user.rol) };
    } catch {
      return null;
    }
  }

  private normalizeRol(rol: Rol | string | undefined): Rol {
    const upper = String(rol ?? '').toUpperCase();
    if (upper === Rol.Admin) return Rol.Admin;
    if (upper === Rol.Docente) return Rol.Docente;
    if (upper === Rol.Estudiante) return Rol.Estudiante;
    return Rol.Estudiante;
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = this.decodeJwt(token);
      const now = Math.floor(Date.now() / 1000);
      return payload.exp < now;
    } catch {
      return true;
    }
  }

  private decodeJwt(token: string): JwtPayload {
    const parts = token.split('.');
    if (parts.length !== 3) throw new Error('Token JWT inválido');
    let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const pad = base64.length % 4;
    if (pad) base64 += '='.repeat(4 - pad);
    const payloadJson = atob(base64);
    return JSON.parse(payloadJson);
  }
}
