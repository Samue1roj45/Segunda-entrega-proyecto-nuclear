import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { mensajeDeError } from '../../core/con-operaciones';
import { ResultadoDetalle, Retroalimentacion } from '../../core/modelos/dominio';
import { SomaApiService } from '../../core/soma-api.service';

/** Resultados propios del estudiante (módulo 5): nota de cada intento, detalle por pregunta y retroalimentación del docente. */
@Component({
  selector: 'app-mis-resultados',
  imports: [DatePipe, DecimalPipe],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2 class="sr-only">Mis resultados</h2></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (cargando()) { <p class="suave">Cargando…</p> }
      @else if (lista().length === 0) { <p class="cristal vacio">Aún no tienes resultados. Aparecerán cuando termines un caso y se evalúe.</p> }
      @else {
        <div class="tarjetas">
          @for (r of lista(); track r.id) {
            <article class="cristal tarjeta-item">
              @if (r.oficial) { <span class="etiqueta ok">Nota oficial</span> }
              <h3>{{ r.practicaNombre }}</h3>
              <p class="meta"><span>Intento {{ r.numeroIntento }}</span><span>{{ r.fechaCalculo | date: 'd MMM y' }}</span></p>
              <p class="nota-grande">{{ r.notaFinal | number: '1.1-2' }} <small>/ {{ r.escalaMaxima }}</small></p>
              <p class="meta"><span>{{ r.correctas }} correctas</span><span>{{ r.incorrectas }} incorrectas</span><span>{{ r.noRespondidas }} sin responder</span></p>
              <div class="acciones"><button type="button" class="boton-suave boton-chico" (click)="abrir(r)">{{ abierto()?.id === r.id ? 'Ocultar detalle' : 'Ver detalle' }}</button></div>
            </article>
          }
        </div>
      }

      @if (abierto(); as r) {
        <section class="cristal bloque">
          <h3>Detalle · {{ r.practicaNombre }} (intento {{ r.numeroIntento }})</h3>
          <ul class="lista-simple">
            @for (d of r.detallePreguntas; track d.preguntaId) {
              <li class="fila-item"><span class="texto">{{ d.enunciado }}</span>
                <span [class]="'etiqueta ' + (!d.respondida ? '' : d.correcta ? 'ok' : 'mal')">{{ !d.respondida ? 'Sin responder' : d.correcta ? 'Correcta' : 'Incorrecta' }}</span></li>
            }
          </ul>
          <h4>Retroalimentación de tu docente</h4>
          @for (t of retros(); track t.id) { <div class="bloque-interno"><p style="white-space:pre-line">{{ t.texto }}</p><p class="ayuda">{{ t.fechaCreacion | date: 'd MMM y, HH:mm' }}</p></div> }
          @empty { <p class="suave">Todavía no hay retroalimentación para este intento.</p> }
        </section>
      }
    </div>
  `,
  styles: `.nota-grande { font-family: var(--serif); font-size: 2.8rem; line-height: 1; color: var(--lila-900); } .nota-grande small { font-size: 1.1rem; color: var(--tinta-suave); }`,
})
export class MisResultados implements OnInit {
  private readonly api = inject(SomaApiService);
  protected readonly lista = signal<ResultadoDetalle[]>([]);
  protected readonly abierto = signal<ResultadoDetalle | null>(null);
  protected readonly retros = signal<Retroalimentacion[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.api.resultadosFiltrados().subscribe({
      next: (r) => { this.lista.set(r); this.cargando.set(false); },
      error: (e: HttpErrorResponse) => { this.cargando.set(false); this.error.set(mensajeDeError(e)); },
    });
  }

  protected abrir(r: ResultadoDetalle): void {
    if (this.abierto()?.id === r.id) { this.abierto.set(null); return; }
    this.abierto.set(r);
    this.api.retroalimentaciones(r.practicaId, r.participacionId).subscribe({ next: (t) => this.retros.set(t), error: () => this.retros.set([]) });
  }
}
