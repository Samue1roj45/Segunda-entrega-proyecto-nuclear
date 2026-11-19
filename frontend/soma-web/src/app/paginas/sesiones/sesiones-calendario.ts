import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SesionDetalle } from '../../core/modelos/dominio';
import { SomaApiService } from '../../core/soma-api.service';

interface Dia { fecha: Date; clave: string; delMes: boolean; hoy: boolean; sesiones: (SesionDetalle & { choque: boolean })[] }

const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
const clave = (d: Date): string => `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
const hora = (iso: string): string => iso.slice(11, 16);

/** Calendario mensual de sesiones; marca en ámbar las que se cruzan en el tiempo (mismo docente). */
@Component({
  selector: 'app-sesiones-calendario',
  imports: [RouterLink],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina">
        <h2 class="sr-only">Calendario de sesiones</h2>
        <a routerLink="/sesiones" class="boton-suave enlace-boton">← Lista de sesiones</a>
        <a routerLink="/sesiones/nueva" class="boton-primario enlace-boton">Programar sesión</a>
      </div>
      <section class="cristal bloque">
        <div class="mes-nav">
          <button type="button" class="boton-suave boton-chico" (click)="mover(-1)" aria-label="Mes anterior">←</button>
          <h3>{{ titulo() }}</h3>
          <button type="button" class="boton-suave boton-chico" (click)="mover(1)" aria-label="Mes siguiente">→</button>
          <button type="button" class="boton-suave boton-chico" (click)="hoy()">Hoy</button>
          @if (choques() > 0) { <span class="etiqueta aviso-choque">{{ choques() }} cruce(s) de horario este mes</span> }
        </div>
        <div class="rejilla" role="grid" aria-label="Calendario del mes">
          @for (d of ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']; track d) { <div class="cab" role="columnheader">{{ d }}</div> }
          @for (d of dias(); track d.clave) {
            <div class="dia" role="gridcell" [class.fuera]="!d.delMes" [class.hoy]="d.hoy">
              <span class="n">{{ d.fecha.getDate() }}</span>
              @for (s of d.sesiones; track s.idPractica) {
                <a class="ev" [class.choque]="s.choque" [class.en-curso]="s.estado === 'EN_CURSO'" [routerLink]="['/sesiones', s.idPractica]"
                   [title]="s.nombreCaso + ' · ' + hora(s.fechaInicio) + '–' + hora(s.fechaFin) + (s.choque ? ' (se cruza con otra sesión)' : '')">
                  <b>{{ hora(s.fechaInicio) }}</b> {{ s.nombreCaso }}</a>
              }
            </div>
          }
        </div>
        <p class="ayuda">En ámbar: sesiones cuyo horario se cruza con otra tuya. Toca una sesión para abrirla.</p>
      </section>
    </div>
  `,
  styles: `
    .mes-nav { display: flex; align-items: center; gap: .8rem; flex-wrap: wrap; margin-bottom: 1rem; } .mes-nav h3 { margin: 0; min-width: 11rem; text-transform: capitalize; }
    .aviso-choque { background: #fbe9c8; color: #7a4d00; }
    .rejilla { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 4px; }
    .cab { text-align: center; font-size: .78rem; color: var(--tinta-suave); padding: .3rem 0; }
    .dia { min-height: 5.6rem; padding: .3rem; border-radius: 10px; background: rgba(255,255,255,.45); display: grid; align-content: start; gap: 2px; overflow: hidden; }
    .dia.fuera { opacity: .45; } .dia.hoy { outline: 2px solid var(--lila-600, #7a5fd0); } .n { font-size: .8rem; color: var(--tinta-suave); }
    .ev { font-size: .72rem; line-height: 1.2; padding: .15rem .35rem; border-radius: 6px; background: var(--lila-700); color: #fff; text-decoration: none; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .ev.en-curso { background: #2d6a4f; } .ev.choque { background: #c78a1d; color: #1c1300; }
    :host-context([data-tema='noche']) .dia { background: rgba(255,255,255,.06); }
    @media (max-width: 640px) { .dia { min-height: 4rem; } .ev b { display: none; } }
  `,
})
export class SesionesCalendario implements OnInit {
  private readonly api = inject(SomaApiService);
  protected readonly hora = hora;
  private readonly sesiones = signal<SesionDetalle[]>([]);
  private readonly mesActual = signal(new Date(new Date().getFullYear(), new Date().getMonth(), 1));
  protected readonly titulo = computed(() => `${MESES[this.mesActual().getMonth()]} ${this.mesActual().getFullYear()}`);

  /** Pares de sesiones cuyo intervalo se solapa. */
  private readonly enChoque = computed(() => {
    const l = this.sesiones().filter((s) => s.estado !== 'FINALIZADO').map((s) => ({ id: s.idPractica, a: +new Date(s.fechaInicio), b: +new Date(s.fechaFin) }));
    const ids = new Set<number>();
    for (let i = 0; i < l.length; i++) for (let j = i + 1; j < l.length; j++) if (l[i].a < l[j].b && l[j].a < l[i].b) { ids.add(l[i].id); ids.add(l[j].id); }
    return ids;
  });

  protected readonly dias = computed<Dia[]>(() => {
    const m = this.mesActual(), hoy = clave(new Date());
    const inicio = new Date(m.getFullYear(), m.getMonth(), 1 - ((m.getDay() + 6) % 7));
    const porDia = new Map<string, (SesionDetalle & { choque: boolean })[]>();
    for (const s of this.sesiones()) {
      const k = clave(new Date(s.fechaInicio));
      porDia.set(k, [...(porDia.get(k) ?? []), { ...s, choque: this.enChoque().has(s.idPractica) }].sort((x, y) => x.fechaInicio.localeCompare(y.fechaInicio)));
    }
    return Array.from({ length: 42 }, (_, i) => {
      const f = new Date(inicio.getFullYear(), inicio.getMonth(), inicio.getDate() + i);
      return { fecha: f, clave: clave(f), delMes: f.getMonth() === m.getMonth(), hoy: clave(f) === hoy, sesiones: porDia.get(clave(f)) ?? [] };
    });
  });
  protected readonly choques = computed(() => new Set(this.dias().filter((d) => d.delMes).flatMap((d) => d.sesiones).filter((s) => s.choque).map((s) => s.idPractica)).size);

  ngOnInit(): void { this.api.sesionesDetalle().subscribe({ next: (s) => this.sesiones.set(s), error: () => this.sesiones.set([]) }); }
  protected mover(n: number): void { const m = this.mesActual(); this.mesActual.set(new Date(m.getFullYear(), m.getMonth() + n, 1)); }
  protected hoy(): void { const d = new Date(); this.mesActual.set(new Date(d.getFullYear(), d.getMonth(), 1)); }
}
