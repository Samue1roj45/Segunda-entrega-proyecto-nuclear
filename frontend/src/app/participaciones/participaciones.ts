import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';

import { AuthService } from '../core/auth/auth.service';
import { Paginated } from '../core/models/academico.model';
import { EstadoParticipacion, Participacion } from '../core/models/practicas.model';
import { Rol } from '../core/models/usuario.model';
import { SimulacionService } from '../core/services/simulacion.service';

@Component({
  selector: 'app-participaciones',
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressBarModule,
  ],
  template: `
    <section class="page">
      <header>
        <h1>Participaciones</h1>
        <p class="subtitle">
          Historial de intentos. Cada intento conserva sus tiempos, estado y calificación.
        </p>
      </header>

      <form class="filtros" (submit)="$event.preventDefault(); cargar()">
        <mat-form-field appearance="outline">
          <mat-label>Práctica (id)</mat-label>
          <input matInput type="number" [(ngModel)]="practicaId" name="practica" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Estado</mat-label>
          <mat-select [(ngModel)]="estado" name="estado">
            <mat-option value="">Todos</mat-option>
            @for (e of estados; track e) {
              <mat-option [value]="e">{{ e }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <button mat-flat-button color="primary" type="submit">Filtrar</button>
      </form>

      @if (loading()) {
        <mat-progress-bar mode="indeterminate" />
      }

      <table mat-table [dataSource]="filas()" class="tabla">
        <ng-container matColumnDef="estudiante">
          <th mat-header-cell *matHeaderCellDef>Estudiante</th>
          <td mat-cell *matCellDef="let p">{{ p.estudiante_nombre }}</td>
        </ng-container>
        <ng-container matColumnDef="practica">
          <th mat-header-cell *matHeaderCellDef>Práctica</th>
          <td mat-cell *matCellDef="let p">{{ p.practica_nombre }}</td>
        </ng-container>
        <ng-container matColumnDef="intento">
          <th mat-header-cell *matHeaderCellDef>Intento</th>
          <td mat-cell *matCellDef="let p">{{ p.numero_intento }}</td>
        </ng-container>
        <ng-container matColumnDef="estado">
          <th mat-header-cell *matHeaderCellDef>Estado</th>
          <td mat-cell *matCellDef="let p">{{ p.estado_display }}</td>
        </ng-container>
        <ng-container matColumnDef="inicio">
          <th mat-header-cell *matHeaderCellDef>Inicio</th>
          <td mat-cell *matCellDef="let p">{{ p.inicio | date:'short' }}</td>
        </ng-container>
        <ng-container matColumnDef="fin">
          <th mat-header-cell *matHeaderCellDef>Fin</th>
          <td mat-cell *matCellDef="let p">{{ p.fin | date:'short' }}</td>
        </ng-container>
        <ng-container matColumnDef="nota">
          <th mat-header-cell *matHeaderCellDef>Nota</th>
          <td mat-cell *matCellDef="let p">{{ p.nota_final ?? '—' }}</td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="columnas"></tr>
        <tr mat-row *matRowDef="let row; columns: columnas"></tr>
      </table>
    </section>
  `,
  styles: [`
    .page { display: flex; flex-direction: column; gap: 1rem; }
    h1 { margin: 0; font-size: 1.5rem; font-weight: 500; }
    .subtitle { margin: 0.25rem 0 0; color: var(--mat-sys-on-surface-variant); }
    .filtros { display: flex; gap: 0.75rem; flex-wrap: wrap; align-items: center; }
    .tabla { width: 100%; }
  `],
})
export class Participaciones implements OnInit {
  private readonly servicio = inject(SimulacionService);
  private readonly auth = inject(AuthService);

  readonly loading = signal(false);
  readonly filas = signal<Participacion[]>([]);
  readonly estados = Object.values(EstadoParticipacion);
  readonly columnas = ['estudiante', 'practica', 'intento', 'estado', 'inicio', 'fin', 'nota'];

  practicaId = '';
  estado = '';

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.loading.set(true);
    const params: Record<string, string | number> = {};
    if (this.practicaId) params['practica'] = this.practicaId;
    if (this.estado) params['estado'] = this.estado;
    this.servicio.listarParticipaciones(params).subscribe({
      next: (page: Paginated<Participacion>) => {
        this.filas.set(page.results);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  get esDocente() {
    return this.auth.usuario()?.rol === Rol.Docente || this.auth.usuario()?.rol === Rol.Admin;
  }
}
