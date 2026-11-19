import { Injectable, signal } from '@angular/core';

export type Tema = 'dia' | 'noche';

/** Tema claro/oscuro del panel interno. Recuerda la elección y, si no hay, sigue al sistema. */
@Injectable({ providedIn: 'root' })
export class TemaService {
  readonly tema = signal<Tema>(this.inicial());

  private inicial(): Tema {
    try {
      const guardado = localStorage.getItem('soma.tema');
      if (guardado === 'dia' || guardado === 'noche') return guardado;
    } catch { /* sin almacenamiento */ }
    return typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)')?.matches ? 'noche' : 'dia';
  }

  /** Aplica el tema al documento (solo mientras el panel está abierto). */
  aplicar(): void { document.documentElement.dataset['tema'] = this.tema(); }
  quitar(): void { delete document.documentElement.dataset['tema']; }

  /** Cambia de tema con un "revelado" circular desde el punto indicado (si el navegador lo permite). */
  alternar(origen?: { x: number; y: number }): void {
    const siguiente: Tema = this.tema() === 'dia' ? 'noche' : 'dia';
    const cambiar = (): void => {
      this.tema.set(siguiente);
      try { localStorage.setItem('soma.tema', siguiente); } catch { /* nada */ }
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
