import { DatePipe } from '@angular/common';
import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { Notificacion } from '../core/modelos/dominio';
import { SomaApiService } from '../core/soma-api.service';

/** Campana con las últimas notificaciones enviadas por el sistema (convocatorias y cambios de sesión). */
@Component({
  selector: 'soma-campana',
  imports: [DatePipe],
  template: `
    <button type="button" class="campana" (click)="alternar()" [attr.aria-expanded]="abierta()" aria-label="Notificaciones">
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 17V11a6 6 0 1 1 12 0v6l1.5 2h-15zM10 21h4" /></svg>
      @if (datos().length) { <i>{{ datos().length > 9 ? '9+' : datos().length }}</i> }
    </button>
    @if (abierta()) {
      <div class="fondo" (click)="abierta.set(false)"></div>
      <section class="menu" aria-label="Notificaciones recientes">
        <h3>Notificaciones</h3>
        @for (n of datos(); track n.id) {
          <article>
            <span class="punto" [class.mal]="n.estado !== 'ENVIADA'"></span>
            <div><strong>{{ n.asunto }}</strong><small>{{ n.destinatarioCorreo }} · {{ n.fechaCreacion | date: 'd MMM, h:mm a' }}</small></div>
          </article>
        } @empty { <p class="vacio">No hay notificaciones todavía.</p> }
      </section>
    }
  `,
  styles: `
    :host { position: relative; display: inline-block; }
    .campana { position: relative; width: 2.5rem; height: 2.5rem; border-radius: 50%; display: grid; place-items: center; border: 1px solid rgba(255,255,255,.8);
      background: rgba(255,255,255,.55); box-shadow: inset 0 1px 0 #fff, 0 8px 18px -14px rgba(59,45,92,.5); transition: background .2s, transform .2s; }
    .campana:hover { background: #fff; transform: translateY(-1px); }
    .campana svg { width: 19px; height: 19px; fill: none; stroke: var(--lila-800); stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
    .campana i { position: absolute; top: -3px; right: -3px; min-width: 1.1rem; height: 1.1rem; border-radius: 999px; padding: 0 .25rem; display: grid; place-items: center;
      font-style: normal; font-size: .65rem; font-weight: 600; color: #fff; background: linear-gradient(135deg, #b78be0, #6b4fb0); box-shadow: 0 0 0 2px #efeaf8; }
    .fondo { position: fixed; inset: 0; z-index: 70; }
    .menu { position: absolute; right: 0; top: calc(100% + .6rem); z-index: 71; width: min(360px, 90vw); max-height: 60vh; overflow-y: auto; padding: 1rem; border-radius: 22px; display: grid; gap: .5rem;
      background: linear-gradient(160deg, rgba(255,255,255,.9), rgba(240,234,252,.82)); border: 1px solid rgba(255,255,255,.9); animation: abre .25s cubic-bezier(.2,.8,.2,1) both;
      -webkit-backdrop-filter: blur(26px) saturate(1.7); backdrop-filter: blur(26px) saturate(1.7); box-shadow: 0 40px 70px -28px rgba(42,31,66,.55), inset 0 1.5px 0 #fff; }
    h3 { font-size: 1.3rem; margin-bottom: .2rem; }
    article { display: flex; gap: .65rem; align-items: flex-start; padding: .6rem .7rem; border-radius: 14px; background: rgba(255,255,255,.6); }
    article div { display: grid; gap: .1rem; min-width: 0; } strong { font-weight: 500; font-size: .88rem; color: var(--lila-950); }
    small { color: var(--tinta-suave); font-size: .75rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .punto { flex: none; width: 8px; height: 8px; border-radius: 50%; margin-top: .4rem; background: #5cb88f; box-shadow: 0 0 0 3px rgba(92,184,143,.22); }
    .punto.mal { background: #d98a5b; box-shadow: 0 0 0 3px rgba(217,138,91,.25); }
    .vacio { padding: 1rem; text-align: center; color: var(--tinta-suave); font-size: .9rem; }
    @keyframes abre { from { opacity: 0; transform: translateY(-8px) scale(.98); } }
    :host-context([data-tema='noche']) .campana { background: rgba(255,255,255,.09); border-color: rgba(var(--n-borde),.22); box-shadow: inset 0 1px 0 rgba(255,255,255,.12); }
    :host-context([data-tema='noche']) .campana:hover { background: rgba(255,255,255,.18); }
    :host-context([data-tema='noche']) .campana svg { stroke: var(--n-d6cbf5); }
    :host-context([data-tema='noche']) .campana i { box-shadow: 0 0 0 2px var(--n-1a1438); }
    :host-context([data-tema='noche']) .menu { background: linear-gradient(160deg, rgba(60,48,110,.88), rgba(26,20,54,.9)); border-color: rgba(var(--n-borde),.25); box-shadow: 0 40px 70px -28px #000, inset 0 1.5px 0 rgba(255,255,255,.14); }
    :host-context([data-tema='noche']) article { background: rgba(255,255,255,.07); }
    :host-context([data-tema='noche']) strong { color: var(--n-f1ecff); }

  `,
})
export class Campana implements OnInit {
  private readonly api = inject(SomaApiService);
  protected readonly datos = signal<Notificacion[]>([]);
  protected readonly abierta = signal(false);

  ngOnInit(): void { this.api.notificaciones().subscribe({ next: (n) => this.datos.set(n.slice(0, 8)), error: () => this.datos.set([]) }); }
  protected alternar(): void { this.abierta.update((v) => !v); }
  @HostListener('document:keydown.escape') protected cerrar(): void { this.abierta.set(false); }
}
