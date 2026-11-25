import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { mensajeDeError } from '../../core/con-operaciones';
import { EcosService, ResultadoEcosDocente } from '../../core/ecos.service';

/** Resultados de "Ecos de una decisión" de los estudiantes convocados por el docente. La nota es la que calculó SOMA. */
@Component({
  selector: 'app-ecos-docente',
  imports: [DatePipe, DecimalPipe],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2>Ecos de una decisión · resultados</h2>
        <p class="suave">Estudiantes de tus sesiones que han terminado el videojuego.</p></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (lista(); as l) {
        @if (l.length === 0) { <p class="cristal vacio">Todavía no hay resultados. Aparecerán cuando tus estudiantes terminen el juego.</p> }
        @else {
          <section class="cristal bloque resumen">
            <p><strong>{{ l.length }}</strong> resultados · promedio <strong>{{ promedio() | number: '1.1-1' }}</strong></p>
            <p class="meta">@for (n of niveles(); track n[0]) { <span>{{ n[0] }}: {{ n[1] }}</span> }</p>
          </section>
          <div class="tabla-envoltura"><table class="tabla">
            <thead><tr><th>Estudiante</th><th>Intento</th><th>Nota</th><th>Nivel</th><th>Final</th><th>Fortalezas</th><th>Por mejorar</th><th>Fecha</th></tr></thead>
            <tbody>
              @for (r of l; track r.sesionId) {
                <tr>
                  <td>{{ r.estudiante }} @if (r.revision) { <span class="etiqueta mal" title="La nota del juego no coincide con la calculada por SOMA">Revisar</span> }</td>
                  <td>{{ r.intento }}</td>
                  <td><strong>{{ r.nota | number: '1.1-1' }}</strong> <small>({{ r.puntaje | number: '1.0-2' }} pts)</small></td>
                  <td>{{ r.nivel }}</td>
                  <td>{{ r.desenlace.replace('FIN_', '').toLowerCase() }} · {{ r.protagonista }}</td>
                  <td>{{ r.fortalezas.join(', ') || '—' }}</td>
                  <td>{{ r.mejoras.join(', ') || '—' }}</td>
                  <td>{{ r.fin | date: 'd MMM y, HH:mm' }}</td>
                </tr>
              }
            </tbody>
          </table></div>
        }
      } @else if (!error()) { <div class="esqueleto" role="status" aria-label="Cargando"><i></i><i></i><i></i></div> }
    </div>
  `,
  styles: `.resumen { display: flex; gap: 1.5rem; align-items: center; flex-wrap: wrap; } .tabla-envoltura { overflow-x: auto; }`,
})
export class EcosDocente implements OnInit {
  private readonly ecos = inject(EcosService);
  protected readonly lista = signal<ResultadoEcosDocente[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly promedio = computed(() => {
    const l = this.lista() ?? [];
    return l.length ? l.reduce((s, r) => s + r.nota, 0) / l.length : 0;
  });
  protected readonly niveles = computed(() => {
    const m = new Map<string, number>();
    for (const r of this.lista() ?? []) { m.set(r.nivel, (m.get(r.nivel) ?? 0) + 1); }
    return [...m.entries()];
  });

  ngOnInit(): void {
    this.ecos.resultadosDocente().subscribe({
      next: (l) => this.lista.set(l),
      error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)),
    });
  }
}
