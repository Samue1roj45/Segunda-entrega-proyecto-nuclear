import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, output, signal } from '@angular/core';
import { CasosApiService } from '../../core/casos-api.service';
import { ConOperaciones } from '../../core/con-operaciones';
import { CasoDetalle, Incumplimiento, SeccionCaso } from '../../core/modelos/caso';

/** RF-09: validación, publicación y archivo. Las reglas las decide el servidor; aquí se muestran. */
@Component({
  selector: 'app-seccion-publicacion',
  imports: [DatePipe],
  template: `
    <section class="tarjeta pila">
      <h2>Validar y publicar</h2>
      <div class="pila-corta suave">
        <span>Estado actual: <span class="estado estado-{{ caso().estado }}">{{ caso().estado }}</span></span>
        @if (caso().fechaPublicacion) { <span>Publicado el {{ caso().fechaPublicacion | date: 'dd/MM/yyyy HH:mm' }}</span> }
        @if (caso().fechaArchivado) { <span>Archivado el {{ caso().fechaArchivado | date: 'dd/MM/yyyy HH:mm' }}</span> }
      </div>

      @if (error(); as mensaje) { <p class="alerta alerta-error" role="alert">{{ mensaje }}</p> }
      @if (aviso(); as mensaje) { <p class="alerta alerta-exito" role="status">{{ mensaje }}</p> }

      @if (incumplimientos().length) {
        <div class="pila-corta">
          <h3>Debe corregir {{ incumplimientos().length }} punto(s)</h3>
          @for (item of incumplimientos(); track $index) {
            <div class="incumplimiento fila separada">
              <div class="pila-corta detalle">
                <span><strong>{{ item.regla }}</strong> · {{ item.mensaje }}</span>
                <span class="suave">{{ item.ubicacion }}</span>
              </div>
              <button type="button" (click)="irA.emit(item.seccion)">Ir a corregir</button>
            </div>
          }
        </div>
      }

      <div class="fila fin">
        @if (caso().estado === 'BORRADOR') {
          <button type="button" [disabled]="ocupado()" (click)="validar()">{{ ocupado() ? 'Procesando…' : 'Validar caso' }}</button>
          <button type="button" class="exito" [disabled]="ocupado()" (click)="publicar()">Publicar caso</button>
        }
        @if (caso().estado === 'PUBLICADO') {
          <button type="button" class="peligro" [disabled]="ocupado()" (click)="archivar()">Archivar caso</button>
        }
      </div>
    </section>
  `,
  styles: `
    .incumplimiento { border-top: 1px solid var(--borde); padding: .6rem 0; }
    .detalle { flex: 1 1 16rem; min-width: 0; }
  `,
})
export class SeccionPublicacion extends ConOperaciones {
  private readonly api = inject(CasosApiService);
  readonly caso = input.required<CasoDetalle>();
  readonly cambio = output<void>();
  readonly irA = output<SeccionCaso>();

  protected readonly incumplimientos = signal<Incumplimiento[]>([]);

  protected validar(): void {
    this.incumplimientos.set([]);
    this.operar(this.api.validar(this.caso().id), (informe) => {
      this.incumplimientos.set(informe.incumplimientos);
      if (informe.valido) {
        this.aviso.set('El caso cumple todas las reglas y se puede publicar.');
      }
    });
  }

  protected publicar(): void {
    if (!confirm('Al publicar, el contenido del caso ya no se podrá editar. ¿Desea continuar?')) {
      return;
    }
    this.incumplimientos.set([]);
    this.operar(this.api.publicar(this.caso().id), () => this.cambio.emit(), 'Caso publicado: ya está disponible para programar.',
      (error: HttpErrorResponse) => this.incumplimientos.set(error.error?.incumplimientos ?? []));
  }

  protected archivar(): void {
    if (confirm('El caso dejará de estar disponible para nuevas programaciones. Sus datos se conservan. ¿Desea continuar?')) {
      this.operar(this.api.archivar(this.caso().id), () => this.cambio.emit(), 'Caso archivado.');
    }
  }
}
