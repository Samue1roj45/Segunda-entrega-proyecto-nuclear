import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReportesService } from '../../core/reportes.service';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { claseEstado, nombreEstado, relojDe } from '../../compartido/formato';
import { mensajeDeError } from '../../core/con-operaciones';
import { ParticipacionLista } from '../../core/modelos/dominio';
import { SomaApiService } from '../../core/soma-api.service';

/** Módulo 5: participaciones de los estudiantes en las sesiones del docente, con filtros. */
@Component({
  selector: 'app-participaciones-docente',
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2 class="sr-only">Participaciones</h2></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }

      <section class="cristal bloque">
        <form class="filtros" (ngSubmit)="buscar()">
          <label class="campo-f">ID de sesión<input name="p" type="number" min="1" [(ngModel)]="f.practica" /></label>
          <label class="campo-f">ID de estudiante<input name="e" type="number" min="1" [(ngModel)]="f.estudiante" /></label>
          <label class="campo-f">Estado
            <select name="es" [(ngModel)]="f.estado">
              <option value="">Todos</option><option value="NO_INICIADA">No iniciada</option><option value="EN_CURSO">En curso</option>
              <option value="FINALIZADA">Finalizada</option><option value="INCOMPLETA">Incompleta</option>
            </select>
          </label>
          <label class="campo-f">Intento<input name="i" type="number" min="1" [(ngModel)]="f.numeroIntento" /></label>
          <label class="campo-f">Desde<input name="d" type="date" [(ngModel)]="f.fechaDesde" /></label>
          <label class="campo-f">Hasta<input name="h" type="date" [(ngModel)]="f.fechaHasta" /></label>
          <button class="boton-primario" type="submit">Filtrar</button>
          <button class="boton-suave" type="button" (click)="limpiar()">Limpiar</button>
          @if (f.practica) {
            <button class="boton-suave" type="button" [disabled]="reportes.generando('xlsx', f.practica)" (click)="reportes.descargar('xlsx', f.practica, f.estudiante)">{{ reportes.generando('xlsx', f.practica) ? 'Generando…' : 'Exportar Excel' }}</button>
          }
        </form>
      </section>

      <section class="cristal bloque">
        @if (cargando()) { <div class="esqueleto" role="status" aria-label="Cargando"><i></i><i></i><i></i></div> }
        @else if (lista().length === 0) { <p class="vacio">No hay participaciones con esos filtros.</p> }
        @else {
          <div class="tabla-envoltorio">
            <table class="tabla">
              <thead><tr><th>Estudiante</th><th>Sesión</th><th class="num">Intento</th><th>Inicio</th><th class="num">Tiempo</th><th>Estado</th><th></th></tr></thead>
              <tbody>
                @for (p of lista(); track p.id) {
                  <tr>
                    <td><strong>{{ p.estudianteNombre }}</strong><br /><span class="ayuda">{{ p.estudianteCorreo }}</span></td>
                    <td>{{ p.practicaNombre }}</td>
                    <td class="num">{{ p.numeroIntento }}</td>
                    <td>{{ p.inicio ? (p.inicio | date: 'd MMM, HH:mm') : '—' }}</td>
                    <td class="num">{{ reloj(p.tiempoUsadoSeg) }}</td>
                    <td><span [class]="'etiqueta ' + clase(p.estado)">{{ nombre(p.estado) }}</span></td>
                    <td><a [routerLink]="['/resultados']" [queryParams]="{ practica: p.practicaId, estudiante: p.estudianteId }" class="boton-suave boton-chico enlace-boton">Ver resultado</a></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <p class="suave">{{ lista().length }} participación(es)</p>
        }
      </section>
    </div>
  `,
})
export class ParticipacionesDocente implements OnInit {
  protected readonly reportes = inject(ReportesService);
  private readonly api = inject(SomaApiService);
  private readonly ruta = inject(ActivatedRoute);
  protected readonly lista = signal<ParticipacionLista[]>([]);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly nombre = nombreEstado;
  protected readonly clase = claseEstado;
  protected readonly reloj = relojDe;
  protected f = { practica: null as number | null, estudiante: null as number | null, estado: '', numeroIntento: null as number | null, fechaDesde: '', fechaHasta: '' };

  ngOnInit(): void {
    const q = this.ruta.snapshot.queryParamMap;
    if (q.get('practica')) { this.f.practica = Number(q.get('practica')); }
    if (q.get('estudiante')) { this.f.estudiante = Number(q.get('estudiante')); }
    this.buscar();
  }

  protected limpiar(): void { this.f = { practica: null, estudiante: null, estado: '', numeroIntento: null, fechaDesde: '', fechaHasta: '' }; this.buscar(); }

  protected buscar(): void {
    this.cargando.set(true); this.error.set(null);
    this.api.participacionesFiltradas(this.f).subscribe({
      next: (l) => { this.lista.set(l); this.cargando.set(false); },
      error: (e: HttpErrorResponse) => { this.cargando.set(false); this.error.set(mensajeDeError(e)); },
    });
  }
}
