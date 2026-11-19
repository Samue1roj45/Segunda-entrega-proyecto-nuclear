import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { guardarArchivo } from '../compartido/formato';
import { SomaApiService } from './soma-api.service';
import { ToastService } from './toast.service';

/** Descarga de reportes con indicador de progreso por botón y avisos de éxito o error. */
@Injectable({ providedIn: 'root' })
export class ReportesService {
  private readonly api = inject(SomaApiService);
  private readonly toasts = inject(ToastService);
  /** Claves «formato:sesion» que se están generando ahora. */
  readonly enCurso = signal<ReadonlySet<string>>(new Set());

  clave(formato: 'pdf' | 'xlsx', practica?: number | null): string { return `${formato}:${practica ?? ''}`; }
  generando(formato: 'pdf' | 'xlsx', practica?: number | null): boolean { return this.enCurso().has(this.clave(formato, practica)); }

  descargar(formato: 'pdf' | 'xlsx', practica?: number | null, estudiante?: number | null): void {
    const k = this.clave(formato, practica);
    if (this.enCurso().has(k)) return;
    this.enCurso.update((s) => new Set(s).add(k));
    const fin = (): void => this.enCurso.update((s) => { const n = new Set(s); n.delete(k); return n; });
    this.api.reporte(formato, practica, estudiante).subscribe({
      next: (b) => { fin(); guardarArchivo(b, `evaluacion${practica ? '-sesion-' + practica : ''}.${formato}`); this.toasts.ok(`Reporte ${formato === 'pdf' ? 'PDF' : 'Excel'} listo.`); },
      error: (e: HttpErrorResponse) => { fin(); this.toasts.error(e.status === 403 ? 'No tienes permiso para descargar reportes.' : 'No se pudo generar el reporte.'); },
    });
  }
}
