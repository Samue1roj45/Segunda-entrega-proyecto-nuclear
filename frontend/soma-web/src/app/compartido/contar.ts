import { Directive, ElementRef, effect, inject, input } from '@angular/core';

/** Muestra un número contando desde 0 hasta su valor (respeta "reducir movimiento"; los no numéricos se ponen tal cual). */
@Directive({ selector: '[somaContar]' })
export class Contar {
  readonly somaContar = input.required<number | string>();
  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private cuadro = 0;

  constructor() {
    effect(() => {
      const v = this.somaContar();
      cancelAnimationFrame(this.cuadro);
      const n = typeof v === 'number' ? v : Number(v);
      const reducir = typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches;
      if (!Number.isFinite(n) || reducir || typeof requestAnimationFrame === 'undefined') { this.el.textContent = String(v); return; }
      const dec = String(v).includes('.') ? 1 : 0;
      const t0 = performance.now();
      const paso = (t: number): void => {
        const p = Math.min((t - t0) / 700, 1);
        this.el.textContent = (n * (1 - Math.pow(1 - p, 3))).toFixed(dec);
        if (p < 1) this.cuadro = requestAnimationFrame(paso);
      };
      this.cuadro = requestAnimationFrame(paso);
    });
  }
}
