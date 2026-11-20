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

  /** Valores que viajan con la cuenta (el modo de color los completa en PreferenciasRemotasService). */
  exportar(): Record<string, number> {
    return {
      avatar: this.avatar(), bienvenida: this.bienvenida() ? 1 : 0, campana: this.campana() ? 1 : 0, letra: this.letra(),
      contraste: this.contraste() ? 1 : 0, acento: this.acento(), sonido: this.sonido() ? 1 : 0, quieto: this.sinMovimiento() ? 1 : 0,
    };
  }

  /** Aplica lo guardado en la cuenta; ignora claves o valores que no reconoce. */
  importar(datos: Record<string, unknown>): void {
    const num = (k: string): number | null => { const v = datos[k]; return typeof v === 'number' && Number.isFinite(v) ? v : null; };
    const poner = (k: string, destino: (n: number) => void): void => { const v = num(k); if (v !== null) destino(v); };
    poner('avatar', (n) => { if (n >= 0 && n < COLORES_AVATAR.length) { this.avatar.set(n); this.guardar('avatar', n); } });
    poner('bienvenida', (n) => { this.bienvenida.set(n === 1); this.guardar('bienvenida', n); });
    poner('campana', (n) => { this.campana.set(n === 1); this.guardar('campana', n); });
    poner('letra', (n) => { if (n >= 0 && n <= 2) { this.letra.set(n); this.guardar('letra', n); } });
    poner('contraste', (n) => { this.contraste.set(n === 1); this.guardar('contraste', n); });
    poner('acento', (n) => { if (n >= 0 && n < ACENTOS.length) { this.acento.set(n); this.guardar('acento', n); } });
    poner('sonido', (n) => { this.sonido.set(n === 1); this.guardar('sonido', n); });
    poner('quieto', (n) => { this.sinMovimiento.set(n === 1); this.guardar('quieto', n); });
    this.aplicar();
  }

  fijarAvatar(i: number): void { this.avatar.set(i); this.guardar('avatar', i); }
  fijarBienvenida(v: boolean): void { this.bienvenida.set(v); this.guardar('bienvenida', v ? 1 : 0); }
  fijarCampana(v: boolean): void { this.campana.set(v); this.guardar('campana', v ? 1 : 0); }

  private leer(clave: string, defecto: number): number {
    try { const v = localStorage.getItem('soma.pref.' + clave); return v === null ? defecto : Number(v); } catch { return defecto; }
  }
  private guardar(clave: string, valor: number): void { try { localStorage.setItem('soma.pref.' + clave, String(valor)); } catch { /* sin almacenamiento */ } }
}
