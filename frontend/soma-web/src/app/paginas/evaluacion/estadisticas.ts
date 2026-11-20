import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { Barras, Dato, Histograma } from '../../compartido/graficas';
import { mensajeDeError } from '../../core/con-operaciones';
import { Resultado, Sesion, SomaApiService } from '../../core/soma-api.service';

/** Panel de estadísticas del docente: resumen de sus resultados calculado con lo que el servidor ya le permite ver. */
@Component({
  selector: 'app-estadisticas',
  imports: [Barras, Histograma, DecimalPipe],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2 class="sr-only">Estadísticas</h2></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (cargando()) { <div class="esqueleto" role="status" aria-label="Cargando estadísticas"><i></i><i></i><i></i></div> }
      @else if (resultados().length === 0) { <p class="cristal vacio">Aún no hay resultados para mostrar estadísticas.</p> }
      @else {
        <div class="tarjetas-resumen" id="resumen-estadisticas">
          <article class="cristal bloque"><span class="suave">Sesiones</span><strong>{{ sesiones().length }}</strong></article>
          <article class="cristal bloque"><span class="suave">Resultados</span><strong>{{ resultados().length }}</strong></article>
          <article class="cristal bloque"><span class="suave">Promedio</span><strong>{{ promedio() * 100 | number: '1.0-0' }} %</strong></article>
          <article class="cristal bloque"><span class="suave">Aprobación (≥ 60 %)</span><strong>{{ aprobacion() * 100 | number: '1.0-0' }} %</strong></article>
        </div>
        <section class="cristal bloque"><h3>Distribución de notas</h3><soma-histograma [valores]="notas()" /></section>
        <section class="cristal bloque"><h3>Promedio por sesión</h3><soma-barras [datos]="porSesion()" /></section>
      }
    </div>
  `,
  styles: `
    .tarjetas-resumen { display: grid; grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr)); gap: 1rem; margin-bottom: 1rem; }
    .tarjetas-resumen article { display: grid; gap: .25rem; } .tarjetas-resumen strong { font-size: 1.8rem; }
    section.bloque { margin-bottom: 1rem; }
  `,
})
export class Estadisticas implements OnInit {
  private readonly api = inject(SomaApiService);
  protected readonly resultados = signal<Resultado[]>([]);
  protected readonly sesiones = signal<Sesion[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  /** Nota normalizada entre 0 y 1 según la escala de cada resultado. */
  protected readonly notas = computed(() => this.resultados().map((r) => normalizar(r)));
  protected readonly promedio = computed(() => media(this.notas()));
  protected readonly aprobacion = computed(() => (this.notas().length ? this.notas().filter((n) => n >= 0.6).length / this.notas().length : 0));
  protected readonly porSesion = computed<Dato[]>(() => {
    const grupos = new Map<string, number[]>();
    for (const r of this.resultados()) { grupos.set(r.practicaNombre, [...(grupos.get(r.practicaNombre) ?? []), normalizar(r)]); }
    return [...grupos].map(([etiqueta, v]) => ({ etiqueta, valor: media(v), detalle: `${v.length} resultado(s)` }));
  });

  ngOnInit(): void {
    forkJoin({ resultados: this.api.resultados(), sesiones: this.api.sesiones() }).subscribe({
      next: ({ resultados, sesiones }) => { this.resultados.set(resultados); this.sesiones.set(sesiones); this.cargando.set(false); },
      error: (e: HttpErrorResponse) => { this.error.set(mensajeDeError(e)); this.cargando.set(false); },
    });
  }
}

function normalizar(r: Resultado): number { return r.escalaMaxima > 0 ? Math.min(1, Math.max(0, r.notaFinal / r.escalaMaxima)) : 0; }
function media(v: readonly number[]): number { return v.length ? v.reduce((a, b) => a + b, 0) / v.length : 0; }
