import { Injectable, signal } from '@angular/core';

export type Tema = 'dia' | 'noche';
export type ModoTema = 'auto' | 'dia' | 'noche';

/** Tema claro/oscuro del panel interno. Recuerda la elección y, si no hay, sigue al sistema. */
@Injectable({ providedIn: 'root' })
export class TemaService {
  /** Elección de la persona: 'auto' sigue al sistema y cambia solo cuando el sistema cambia. */
  readonly modo = signal<ModoTema>(this.modoGuardado());
  readonly tema = signal<Tema>(this.resolver(this.modo()));

  constructor() {
    try {
      window.matchMedia?.('(prefers-color-scheme: dark)')?.addEventListener?.('change', () => {
        if (this.modo() === 'auto') { this.tema.set(this.resolver('auto')); if (document.documentElement.dataset['tema']) this.aplicar(); }
      });
    } catch { /* sin matchMedia */ }
  }

  private modoGuardado(): ModoTema {
    try {
      const guardado = localStorage.getItem('soma.tema');
      if (guardado === 'dia' || guardado === 'noche' || guardado === 'auto') return guardado;
    } catch { /* sin almacenamiento */ }
    return 'auto';
  }

  private resolver(modo: ModoTema): Tema {
    if (modo !== 'auto') return modo;
    return typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)')?.matches ? 'noche' : 'dia';
  }

  /** Fija el modo (por ejemplo al traerlo desde las preferencias del usuario) sin animación. */
  fijarModo(modo: ModoTema): void {
    this.modo.set(modo);
    this.tema.set(this.resolver(modo));
    try { localStorage.setItem('soma.tema', modo); } catch { /* nada */ }
    if (document.documentElement.dataset['tema']) this.aplicar();
  }

  /** Aplica el tema al documento (solo mientras el panel está abierto). */
  aplicar(): void { document.documentElement.dataset['tema'] = this.tema(); }
  quitar(): void { delete document.documentElement.dataset['tema']; }

  /** Cambia de tema con un "revelado" circular desde el punto indicado (si el navegador lo permite). */
  alternar(origen?: { x: number; y: number }, destino?: ModoTema): void {
    const modo: ModoTema = destino ?? (this.tema() === 'dia' ? 'noche' : 'dia');
    const cambiar = (): void => {
      this.modo.set(modo);
      this.tema.set(this.resolver(modo));
      try { localStorage.setItem('soma.tema', modo); } catch { /* nada */ }
      this.aplicar();
    };
    const doc = document as Document & { startViewTransition?: (f: () => void) => { ready: Promise<void> } };
    const reducir = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches;
    if (!doc.startViewTransition || reducir || !origen) { cambiar(); return; }
    document.documentElement.classList.add('cambiando-tema');
    const vt = doc.startViewTransition(cambiar);
    const radio = Math.hypot(Math.max(origen.x, innerWidth - origen.x), Math.max(origen.y, innerHeight - origen.y));
    vt.ready.then(() => {
      document.documentElement.animate(
        { clipPath: [`circle(0px at ${origen.x}px ${origen.y}px)`, `circle(${radio}px at ${origen.x}px ${origen.y}px)`] },
        { duration: 650, easing: 'cubic-bezier(.2,.7,.2,1)', pseudoElement: '::view-transition-new(root)' },
      );
    }).catch(() => undefined);
    (vt as unknown as { finished: Promise<void> }).finished.finally(() => document.documentElement.classList.remove('cambiando-tema'));
  }
}
