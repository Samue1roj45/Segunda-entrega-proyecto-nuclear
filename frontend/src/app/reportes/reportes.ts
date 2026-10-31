import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { ExtrasService } from '../core/services/extras.service';

@Component({
  selector: 'app-reportes',
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
  ],
  template: `
    <section class="page">
      <h1>Reportes</h1>
      <p class="subtitle">Descarga PDF o Excel de una práctica (incluye incompletos y ausencias).</p>
      <mat-form-field appearance="outline">
        <mat-label>ID de práctica</mat-label>
        <input matInput type="number" [(ngModel)]="practicaId" />
      </mat-form-field>
      <div class="acciones">
        <button mat-stroked-button (click)="descargar('pdf')">PDF</button>
        <button mat-flat-button color="primary" (click)="descargar('excel')">Excel</button>
      </div>
    </section>
  `,
  styles: [`
    .page { display: flex; flex-direction: column; gap: 1rem; max-width: 480px; }
    h1 { margin: 0; font-size: 1.5rem; font-weight: 500; }
    .subtitle { margin: 0; color: var(--mat-sys-on-surface-variant); }
    .acciones { display: flex; gap: 0.75rem; }
  `],
})
export class Reportes {
  private readonly extras = inject(ExtrasService);
  private readonly snack = inject(MatSnackBar);
  practicaId = '';

  descargar(tipo: 'pdf' | 'excel') {
    const id = Number(this.practicaId);
    if (!id) {
      this.snack.open('Indica un ID de práctica válido.', 'Cerrar', { duration: 3000 });
      return;
    }
    const req$ = tipo === 'pdf'
      ? this.extras.descargarReportePracticaPDF(id)
      : this.extras.descargarReportePracticaExcel(id);
    req$.subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = tipo === 'pdf' ? `reporte-practica-${id}.pdf` : `reporte-practica-${id}.xlsx`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.snack.open('No se pudo descargar el reporte.', 'Cerrar', { duration: 4000 }),
    });
  }
}
