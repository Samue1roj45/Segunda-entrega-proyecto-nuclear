import { Component, computed, input } from '@angular/core';

/** Histograma de notas: cuenta cuántos valores (0 a 1) caen en cada tramo. SVG propio, sin librerías. */
@Component({
  selector: 'soma-histograma',
  template: `
    <figure>
      <svg [attr.viewBox]="'0 0 ' + ancho + ' 150'" role="img" [attr.aria-label]="'Distribución de notas: ' + descripcion()">
        <defs><linearGradient id="gh" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#a68ae8" /><stop offset="1" stop-color="#5e4a8a" /></linearGradient></defs>
        @for (b of barras(); track $index) {
          <g [style.animation-delay]="$index * 70 + 'ms'" class="barra">
            <rect [attr.x]="b.x" [attr.y]="130 - b.alto" [attr.width]="b.ancho" [attr.height]="b.alto" rx="8" fill="url(#gh)" />
            <text [attr.x]="b.x + b.ancho / 2" [attr.y]="124 - b.alto" text-anchor="middle" class="n">{{ b.cuenta }}</text>
            <text [attr.x]="b.x + b.ancho / 2" y="146" text-anchor="middle" class="e">{{ b.etiqueta }}</text>
          </g>
        }
      </svg>
    </figure>
  `,
  styles: `
    figure { margin: 0; } svg { width: 100%; height: auto; max-height: 190px; overflow: visible; }
    .barra { transform-origin: bottom; animation: crece .7s cubic-bezier(.2,.8,.2,1) both; transform-box: fill-box; }
    @keyframes crece { from { transform: scaleY(.05); opacity: 0; } }
    .n { font-size: 13px; font-weight: 600; fill: var(--lila-900); } .e { font-size: 11px; fill: var(--tinta-suave); }
    :host-context([data-tema='noche']) .n { fill: #efe9ff; }
    @media (prefers-reduced-motion: reduce) { .barra { animation: none; } }
  `,
})
export class Histograma {
  /** Valores normalizados entre 0 y 1 (nota / escala). */
  readonly valores = input.required<readonly number[]>();
  protected readonly ancho = 400;
  private readonly tramos = ['0–20 %', '20–40 %', '40–60 %', '60–80 %', '80–100 %'];
  protected readonly barras = computed(() => {
    const cuentas = [0, 0, 0, 0, 0];
    for (const v of this.valores()) cuentas[Math.min(4, Math.max(0, Math.floor(v * 5)))]++;
    const max = Math.max(1, ...cuentas);
    const ancho = (this.ancho - 4 * 14) / 5;
    return cuentas.map((cuenta, i) => ({ cuenta, x: i * (ancho + 14), ancho, alto: Math.max(cuenta ? 6 : 2, (cuenta / max) * 96), etiqueta: this.tramos[i] }));
  });
  protected readonly descripcion = computed(() => this.barras().map((b) => `${b.etiqueta}: ${b.cuenta}`).join(', '));
}

export interface Dato { readonly etiqueta: string; readonly valor: number; readonly detalle?: string }

/** Barras horizontales con valores entre 0 y 1 (por ejemplo, porcentaje de acierto por pregunta). */
@Component({
  selector: 'soma-barras',
  template: `
    <ul>
      @for (d of datos(); track d.etiqueta) {
        <li>
          <span class="et" [title]="d.etiqueta">{{ d.etiqueta }}</span>
          <span class="pista" role="img" [attr.aria-label]="d.etiqueta + ': ' + (d.valor * 100).toFixed(0) + ' %'"><i [style.width.%]="d.valor * 100" [class]="clase(d.valor)"></i></span>
          <span class="v">{{ (d.valor * 100).toFixed(0) }} %</span>
        </li>
      }
    </ul>
  `,
  styles: `
    ul { list-style: none; margin: 0; padding: 0; display: grid; gap: .55rem; }
    li { display: grid; grid-template-columns: minmax(80px, 38%) 1fr 3rem; align-items: center; gap: .7rem; font-size: .85rem; }
    .et { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--tinta); } .v { text-align: right; font-variant-numeric: tabular-nums; color: var(--tinta-suave); }
    .pista { height: 10px; border-radius: 999px; background: rgba(94,74,138,.14); overflow: hidden; }
    i { display: block; height: 100%; border-radius: 999px; animation: llena .9s cubic-bezier(.2,.8,.2,1) both; background: linear-gradient(90deg, #8b6fe0, #5e4a8a); }
    i.bien { background: linear-gradient(90deg, #7fd1ae, #3f8f6b); } i.mal { background: linear-gradient(90deg, #f0a58d, #b5523b); }
    @keyframes llena { from { width: 0; } }
    :host-context([data-tema='noche']) .pista { background: rgba(255,255,255,.12); } :host-context([data-tema='noche']) .et { color: #e6e0f5; }
    @media (prefers-reduced-motion: reduce) { i { animation: none; } }
  `,
})
export class Barras {
  readonly datos = input.required<readonly Dato[]>();
  protected clase(v: number): string { return v >= 0.7 ? 'bien' : v < 0.4 ? 'mal' : ''; }
}
