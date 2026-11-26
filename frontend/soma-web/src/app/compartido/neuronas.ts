import { Component, ElementRef, NgZone, OnDestroy, afterNextRender, inject, viewChild } from '@angular/core';

interface Nodo { x: number; y: number; vx: number; vy: number; r: number }
interface Pulso { a: number; b: number; t: number }

/**
 * Red de neuronas viva: puntos que flotan, se conectan cuando se acercan, mandan pulsos de luz por las conexiones
 * y reaccionan al cursor (se encienden las conexiones cercanas). Se pausa fuera de pantalla y con «reducir animaciones».
 */
@Component({
  selector: 'soma-neuronas',
  template: `<canvas #lienzo aria-hidden="true"></canvas>`,
  styles: `:host { position: absolute; inset: 0; pointer-events: none; display: block; } canvas { width: 100%; height: 100%; display: block; }`,
})
export class Neuronas implements OnDestroy {
  private readonly lienzo = viewChild.required<ElementRef<HTMLCanvasElement>>('lienzo');
  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly zona = inject(NgZone);
  private nodos: Nodo[] = [];
  private pulsos: Pulso[] = [];
  private raf = 0;
  private visible = true;
  private w = 0; private h = 0;
  private cursor: { x: number; y: number } | null = null;
  private limpiar: (() => void)[] = [];

  constructor() {
    afterNextRender(() => this.zona.runOutsideAngular(() => this.iniciar()));
  }

  ngOnDestroy(): void { cancelAnimationFrame(this.raf); this.limpiar.forEach((f) => f()); }

  private quieto(): boolean { return document.documentElement.hasAttribute('data-quieto') || !!matchMedia('(prefers-reduced-motion: reduce)').matches; }

  private iniciar(): void {
    const c = this.lienzo().nativeElement, ctx = c.getContext('2d');
    const padre = this.el.nativeElement.parentElement;
    if (!ctx || !padre) return;
    const ajustar = (): void => {
      const dpr = Math.min(devicePixelRatio || 1, 2), r = padre.getBoundingClientRect();
      this.w = r.width; this.h = r.height; c.width = this.w * dpr; c.height = this.h * dpr; ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      const n = Math.round(Math.min(70, Math.max(18, (this.w * this.h) / 9000)));
      while (this.nodos.length < n) this.nodos.push({ x: Math.random() * this.w, y: Math.random() * this.h, vx: (Math.random() - .5) * .35, vy: (Math.random() - .5) * .35, r: 1.2 + Math.random() * 2 });
      this.nodos.length = n;
      if (this.quieto()) this.dibujar(ctx);
    };
    const ro = new ResizeObserver(ajustar); ro.observe(padre); this.limpiar.push(() => ro.disconnect());
    const io = new IntersectionObserver(([e]) => { this.visible = e.isIntersecting; }); io.observe(padre); this.limpiar.push(() => io.disconnect());
    const mover = (e: PointerEvent): void => { const r = padre.getBoundingClientRect(); this.cursor = { x: e.clientX - r.left, y: e.clientY - r.top }; };
    const salir = (): void => { this.cursor = null; };
    padre.addEventListener('pointermove', mover); padre.addEventListener('pointerleave', salir);
    this.limpiar.push(() => { padre.removeEventListener('pointermove', mover); padre.removeEventListener('pointerleave', salir); });
    ajustar();
    const paso = (): void => {
      this.raf = requestAnimationFrame(paso);
      if (!this.visible || document.hidden || this.quieto()) return;
      this.mover(); this.dibujar(ctx);
    };
    paso();
  }

  private mover(): void {
    for (const n of this.nodos) {
      n.x += n.vx; n.y += n.vy;
      if (n.x < 0 || n.x > this.w) n.vx *= -1;
      if (n.y < 0 || n.y > this.h) n.vy *= -1;
      if (this.cursor) { const dx = n.x - this.cursor.x, dy = n.y - this.cursor.y, d = Math.hypot(dx, dy); if (d < 110 && d > 1) { n.x += (dx / d) * .6; n.y += (dy / d) * .6; } }
    }
    if (Math.random() < .06 && this.nodos.length > 2) {
      const a = Math.floor(Math.random() * this.nodos.length), A = this.nodos[a];
      const b = this.nodos.findIndex((B, i) => i !== a && Math.hypot(A.x - B.x, A.y - B.y) < 140);
      if (b >= 0) this.pulsos.push({ a, b, t: 0 });
    }
    this.pulsos = this.pulsos.filter((p) => (p.t += .025) < 1);
  }

  private dibujar(ctx: CanvasRenderingContext2D): void {
    const noche = document.documentElement.dataset['tema'] === 'noche';
    const tinta = noche ? (getComputedStyle(document.documentElement).getPropertyValue('--n-borde').trim() || '190,170,240') : (getComputedStyle(document.documentElement).getPropertyValue('--d-94_74_138').trim() || '94,74,138');
    // Canvas no resuelve var(--...): se lee el valor RGB real de la variable (igual que `tinta`).
    const pulso = getComputedStyle(document.documentElement).getPropertyValue('--d-139_111_224').trim() || '139,111,224';
    ctx.clearRect(0, 0, this.w, this.h);
    const N = this.nodos;
    for (let i = 0; i < N.length; i++) for (let j = i + 1; j < N.length; j++) {
      const d = Math.hypot(N[i].x - N[j].x, N[i].y - N[j].y);
      if (d > 140) continue;
      let k = (1 - d / 140) * .35;
      if (this.cursor) { const m = Math.hypot((N[i].x + N[j].x) / 2 - this.cursor.x, (N[i].y + N[j].y) / 2 - this.cursor.y); if (m < 130) k += (1 - m / 130) * .5; }
      ctx.strokeStyle = `rgba(${tinta},${k})`; ctx.lineWidth = 1; ctx.beginPath(); ctx.moveTo(N[i].x, N[i].y); ctx.lineTo(N[j].x, N[j].y); ctx.stroke();
    }
    for (const n of N) { ctx.fillStyle = `rgba(${tinta},.55)`; ctx.beginPath(); ctx.arc(n.x, n.y, n.r, 0, 6.283); ctx.fill(); }
    for (const p of this.pulsos) {
      const A = N[p.a], B = N[p.b]; if (!A || !B) continue;
      const x = A.x + (B.x - A.x) * p.t, y = A.y + (B.y - A.y) * p.t;
      const g = ctx.createRadialGradient(x, y, 0, x, y, 12); g.addColorStop(0, noche ? 'rgba(220,205,255,.95)' : `rgba(${pulso},.95)`); g.addColorStop(1, `rgba(${pulso},0)`);
      ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, 12, 0, 6.283); ctx.fill();
    }
  }
}
