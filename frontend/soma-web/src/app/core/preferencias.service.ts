import { Injectable, signal } from '@angular/core';

/** Paletas de acento: cambian los tonos de toda la plataforma (valor 0 = lila original). */
export const ACENTOS: readonly { id: string; nombre: string; muestra: string }[] = [
  { id: 'lila', nombre: 'Lila', muestra: 'var(--d-7a5fd0)' }, { id: 'oceano', nombre: 'Océano', muestra: '#2a6aa3' }, { id: 'bosque', nombre: 'Bosque', muestra: '#2f7d56' },
  { id: 'atardecer', nombre: 'Atardecer', muestra: '#a23e70' }, { id: 'grafito', nombre: 'Grafito', muestra: '#52576a' },
];

export const COLORES_AVATAR: readonly (readonly [string, string])[] = [
  ['#c6b9e2', '#4d3b75'], ['#aebcf0', '#3b4f9a'], ['#e8b8dd', '#8a3f7d'], ['#b7e0d0', '#2f7a63'], ['#f0cfa0', '#9a6a2c'],
];

/** Preferencias de interfaz que se guardan solo en este navegador. */
@Injectable({ providedIn: 'root' })
export class PreferenciasService {
  readonly avatar = signal(this.leer('avatar', 0));
  readonly bienvenida = signal(this.leer('bienvenida', 1) === 1);
  readonly campana = signal(this.leer('campana', 1) === 1);
  /** 0 = normal, 1 = grande, 2 = muy grande. */
  readonly letra = signal(this.leer('letra', 0));
  readonly contraste = signal(this.leer('contraste', 0) === 1);
  readonly acento = signal(this.leer('acento', 0));
  readonly sonido = signal(this.leer('sonido', 0) === 1);
  readonly sinMovimiento = signal(this.leer('quieto', 0) === 1);

  constructor() { this.aplicar(); }

  fijarLetra(n: number): void { this.letra.set(n); this.guardar('letra', n); this.aplicar(); }
  fijarContraste(v: boolean): void { this.contraste.set(v); this.guardar('contraste', v ? 1 : 0); this.aplicar(); }
  fijarAcento(i: number): void { this.acento.set(i); this.guardar('acento', i); this.aplicar(); }
  fijarSonido(v: boolean): void { this.sonido.set(v); this.guardar('sonido', v ? 1 : 0); }
  fijarSinMovimiento(v: boolean): void { this.sinMovimiento.set(v); this.guardar('quieto', v ? 1 : 0); this.aplicar(); }

  /** Las preferencias de accesibilidad valen en todo el sitio, también en el inicio de sesión. */
  private aplicar(): void {
    const d = document.documentElement;
    d.style.fontSize = ['', '112.5%', '125%'][this.letra()] ?? '';
    d.toggleAttribute('data-contraste', this.contraste());
    d.toggleAttribute('data-quieto', this.sinMovimiento());
    const a = ACENTOS[this.acento()]?.id ?? 'lila';
    if (a === 'lila') delete d.dataset['acento']; else d.dataset['acento'] = a;
  }

  fijarAvatar(i: number): void { this.avatar.set(i); this.guardar('avatar', i); }
  fijarBienvenida(v: boolean): void { this.bienvenida.set(v); this.guardar('bienvenida', v ? 1 : 0); }
  fijarCampana(v: boolean): void { this.campana.set(v); this.guardar('campana', v ? 1 : 0); }

  private leer(clave: string, defecto: number): number {
    try { const v = localStorage.getItem('soma.pref.' + clave); return v === null ? defecto : Number(v); } catch { return defecto; }
  }
  private guardar(clave: string, valor: number): void { try { localStorage.setItem('soma.pref.' + clave, String(valor)); } catch { /* sin almacenamiento */ } }
}
