import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { mensajeDeError } from '../../core/con-operaciones';
import { EcosService, EnVivoEcos } from '../../core/ecos.service';

const ACTOS = 6;

/** Tablero en vivo para el docente: quién tiene código, quién está jugando (y en qué acto va) y quién acaba de terminar. */
@Component({
  selector: 'app-ecos-en-vivo',
  imports: [DecimalPipe],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2>Ecos de una decisión · en vivo</h2>
        <p class="suave">Se actualiza solo cada 5 segundos. Aparecen tus estudiantes con código generado, en juego o que terminaron hace menos de 15 minutos.</p></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (lista(); as l) {
        @if (l.length === 0) { <p class="cristal vacio">Nadie está jugando ahora mismo.</p> }
        @else {
          <div class="tarjetas">
            @for (j of l; track j.sesionId) {
              <article class="cristal tarjeta-item">
                <span [class]="'etiqueta ' + clase(j)">{{ texto(j) }}</span>
                <h3>{{ j.estudiante }}</h3>
                <p class="meta"><span>Intento {{ j.intento }}</span>@if (j.estado === 'CANJEADA') { <span>{{ hace(j) }}</span> }</p>
                @if (j.estado === 'CANJEADA') {
                  <div class="progreso" role="progressbar" [attr.aria-valuenow]="j.ultimoActo ?? 0" aria-valuemin="0" [attr.aria-valuemax]="actos"
                       [attr.aria-label]="'Acto ' + (j.ultimoActo ?? 0) + ' de ' + actos"><i [style.width.%]="porcentaje(j)"></i></div>
                  <p class="ayuda">{{ j.ultimoActo ? 'Acto ' + j.ultimoActo + ' de ' + actos : 'Recién empezó' }}@if (j.ultimoPunto) { · {{ j.ultimoPunto }} }</p>
                } @else if (j.estado === 'CERRADA') {
                  <p class="nota-grande">{{ j.nota | number: '1.1-1' }} <small>/ 5.0 · {{ j.nivel }}</small></p>
                } @else { <p class="ayuda">Esperando que abra el juego…</p> }
              </article>
            }
          </div>
        }
      } @else if (!error()) { <div class="esqueleto" role="status" aria-label="Cargando"><i></i><i></i><i></i></div> }
    </div>
  `,
  styles: `
    .nota-grande { font-size: 2.2rem; font-weight: 600; margin: 0; } .nota-grande small { font-size: .9rem; font-weight: 400; color: var(--tinta-suave); }
    .progreso { height: 8px; border-radius: 99px; background: rgba(0,0,0,.08); overflow: hidden; margin: .5rem 0 .3rem; }
    .progreso i { display: block; height: 100%; background: var(--lila-700); border-radius: 99px; transition: width .4s ease; }
  `,
})
export class EcosEnVivo implements OnInit, OnDestroy {
  private readonly ecos = inject(EcosService);
  protected readonly lista = signal<EnVivoEcos[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly actos = ACTOS;
  private readonly ahora = signal(Date.now());
  private temporizador?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    this.cargar();
    this.temporizador = setInterval(() => { this.ahora.set(Date.now()); this.cargar(); }, 5000);
  }

  ngOnDestroy(): void { clearInterval(this.temporizador); }

  private cargar(): void {
    this.ecos.enVivo().subscribe({
      next: (l) => { this.lista.set(l); this.error.set(null); },
      error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)),
    });
  }

  protected texto(j: EnVivoEcos): string { return j.estado === 'CANJEADA' ? 'Jugando' : j.estado === 'CERRADA' ? 'Terminó' : 'Código generado'; }
  protected clase(j: EnVivoEcos): string { return j.estado === 'CANJEADA' ? 'en-curso' : j.estado === 'CERRADA' ? 'ok' : 'borrador'; }
  protected porcentaje(j: EnVivoEcos): number { return Math.min(100, Math.round(((j.ultimoActo ?? 0) / ACTOS) * 100)); }
  protected hace(j: EnVivoEcos): string {
    const ref = j.ultimoEventoEn ?? j.canjeadaEn;
    if (!ref) { return ''; }
    const s = Math.max(0, Math.floor((this.ahora() - new Date(ref).getTime()) / 1000));
    return s < 60 ? 'hace unos segundos' : s < 3600 ? `hace ${Math.floor(s / 60)} min` : `hace ${Math.floor(s / 3600)} h`;
  }
}
