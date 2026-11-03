import { Component, OnChanges, inject, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CasosApiService } from '../../core/casos-api.service';
import { ConOperaciones } from '../../core/con-operaciones';
import { CasoDetalle } from '../../core/modelos/caso';

/** RF-05: datos académicos del caso. */
@Component({
  selector: 'app-seccion-datos',
  imports: [FormsModule],
  template: `
    <form class="tarjeta pila" (ngSubmit)="guardar()">
      <label class="campo">Nombre del caso
        <input name="nombre" maxlength="150" [disabled]="!editable()" [(ngModel)]="nombre" />
      </label>
      <label class="campo">Tiempo estimado de participación (minutos)
        <input name="tiempo" type="number" min="1" [disabled]="!editable()" [(ngModel)]="tiempo" />
      </label>
      <label class="campo">Desarrollo situacional
        <textarea name="desarrollo" rows="4" [disabled]="!editable()" [(ngModel)]="desarrollo"></textarea>
      </label>
      <label class="campo">Contexto o historia
        <textarea name="contexto" rows="4" [disabled]="!editable()" [(ngModel)]="contexto"></textarea>
      </label>
      @if (error(); as mensaje) { <p class="alerta alerta-error" role="alert">{{ mensaje }}</p> }
      @if (aviso(); as mensaje) { <p class="alerta alerta-exito" role="status">{{ mensaje }}</p> }
      @if (editable()) {
        <div class="fila fin">
          <button type="submit" class="primario" [disabled]="ocupado()">
            {{ ocupado() ? 'Guardando…' : 'Guardar datos' }}
          </button>
        </div>
      }
    </form>
  `,
})
export class SeccionDatos extends ConOperaciones implements OnChanges {
  private readonly api = inject(CasosApiService);
  readonly caso = input.required<CasoDetalle>();
  readonly editable = input.required<boolean>();
  readonly cambio = output<void>();

  protected nombre: string | null = null;
  protected tiempo: number | null = null;
  protected desarrollo: string | null = null;
  protected contexto: string | null = null;

  ngOnChanges(): void {
    const caso = this.caso();
    this.nombre = caso.nombre;
    this.tiempo = caso.tiempoEstimadoMinutos;
    this.desarrollo = caso.desarrolloSituacional;
    this.contexto = caso.contextoHistoria;
  }

  protected guardar(): void {
    const datos = {
      nombre: this.nombre,
      tiempoEstimadoMinutos: this.tiempo,
      desarrolloSituacional: this.desarrollo,
      contextoHistoria: this.contexto,
    };
    this.operar(this.api.actualizar(this.caso().id, datos), () => this.cambio.emit(), 'Datos guardados.');
  }
}
