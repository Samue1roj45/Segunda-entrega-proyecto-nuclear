import { Injectable, signal } from '@angular/core';

export const COLORES_AVATAR: readonly (readonly [string, string])[] = [
  ['#c6b9e2', '#4d3b75'], ['#aebcf0', '#3b4f9a'], ['#e8b8dd', '#8a3f7d'], ['#b7e0d0', '#2f7a63'], ['#f0cfa0', '#9a6a2c'],
];

/** Preferencias de interfaz que se guardan solo en este navegador. */
@Injectable({ providedIn: 'root' })
export class PreferenciasService {
  readonly avatar = signal(this.leer('avatar', 0));
  readonly bienvenida = signal(this.leer('bienvenida', 1) === 1);
  readonly campana = signal(this.leer('campana', 1) === 1);

  fijarAvatar(i: number): void { this.avatar.set(i); this.guardar('avatar', i); }
  fijarBienvenida(v: boolean): void { this.bienvenida.set(v); this.guardar('bienvenida', v ? 1 : 0); }
  fijarCampana(v: boolean): void { this.campana.set(v); this.guardar('campana', v ? 1 : 0); }

  private leer(clave: string, defecto: number): number {
    try { const v = localStorage.getItem('soma.pref.' + clave); return v === null ? defecto : Number(v); } catch { return defecto; }
  }
  private guardar(clave: string, valor: number): void { try { localStorage.setItem('soma.pref.' + clave, String(valor)); } catch { /* sin almacenamiento */ } }
}
