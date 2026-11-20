import { Component, inject } from '@angular/core';
import { ToastService } from '../core/toast.service';

@Component({
  selector: 'soma-toasts',
  template: `
    <div class="pila" aria-live="polite" role="status">
      @for (a of toasts.avisos(); track a.id) {
        <div class="toast" [class]="'toast ' + a.tipo">
          <span class="ic">
            @if (a.tipo === 'ok') { <svg viewBox="0 0 24 24"><path d="m5 12.5 4.5 4.5L19 7.5" /></svg> }
            @else if (a.tipo === 'error') { <svg viewBox="0 0 24 24"><path d="M12 7v6m0 4h.01" /></svg> }
            @else { <svg viewBox="0 0 24 24"><path d="M12 11v6m0-10h.01" /></svg> }
          </span>
          <span class="tx">{{ a.texto }}</span>
          <button type="button" (click)="toasts.cerrar(a.id)" aria-label="Cerrar aviso">×</button>
        </div>
      }
    </div>
  `,
  styles: `
    .pila { position: fixed; z-index: 200; right: 1.2rem; bottom: 1.2rem; display: grid; gap: .6rem; width: min(380px, calc(100vw - 2rem)); pointer-events: none; }
    .toast { pointer-events: auto; display: flex; align-items: center; gap: .7rem; padding: .75rem .9rem; border-radius: 18px; animation: sube .38s cubic-bezier(.2,.9,.25,1.1) both;
      background: linear-gradient(160deg, rgba(255,255,255,.92), rgba(var(--d-240_234_252),.86)); border: 1px solid rgba(255,255,255,.95); color: var(--lila-950);
      -webkit-backdrop-filter: blur(24px) saturate(1.7); backdrop-filter: blur(24px) saturate(1.7); box-shadow: 0 24px 44px -20px rgba(var(--d-42_31_66),.55), inset 0 1.5px 0 #fff; }
    .ic { flex: none; width: 1.9rem; height: 1.9rem; border-radius: 50%; display: grid; place-items: center; background: #d8efe4; color: #2d6a4f; }
    .error .ic { background: rgba(181,82,59,.16); color: #b5523b; } .info .ic { background: #e3e8f6; color: #36498a; }
    svg { width: 17px; height: 17px; fill: none; stroke: currentColor; stroke-width: 2.2; stroke-linecap: round; stroke-linejoin: round; }
    .tx { flex: 1; font-size: .92rem; line-height: 1.35; }
    button { border: 0; background: none; font-size: 1.3rem; line-height: 1; color: var(--tinta-suave); padding: 0 .2rem; }
    @keyframes sube { from { opacity: 0; transform: translateY(14px) scale(.96); } }
    :host-context([data-tema='noche']) .toast { background: linear-gradient(160deg, rgba(var(--n-s6),.92), rgba(var(--n-s4),.92)); border-color: rgba(var(--n-borde),.28); color: var(--n-f1ecff); box-shadow: 0 24px 44px -20px #000, inset 0 1.5px 0 rgba(255,255,255,.14); }
    :host-context([data-tema='noche']) .ic { background: rgba(92,184,143,.22); color: #8fe0bb; }
    :host-context([data-tema='noche']) .error .ic { background: rgba(224,110,90,.22); color: #ffab9a; }
    :host-context([data-tema='noche']) .info .ic { background: rgba(110,140,230,.24); color: var(--n-b4c6ff); }
    @media (max-width: 1000px) { .pila { bottom: 5.8rem; right: .7rem; } }
    @media (prefers-reduced-motion: reduce) { .toast { animation: none; } }
  `,
})
export class Toasts { protected readonly toasts = inject(ToastService); }
