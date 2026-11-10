import { Component, OnChanges, inject, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CasosApiService } from '../../core/casos-api.service';
import { ConOperaciones } from '../../core/con-operaciones';
import { Respuesta } from '../../core/modelos/caso';

/** RF-07 y RF-08: una opción de respuesta, si es la correcta y su justificación. */
@Component({
  selector: 'app-editor-respuesta',
  imports: [FormsModule],
  template: `
    <div class="opcion pila-corta">
      <div class="fila">
        <label class="campo codigo">Identificador
          <input name="codigo" maxlength="2" [disabled]="!editable()" [(ngModel)]="codigo" />
        </label>
        <label class="campo">Texto de la opción
          <input name="texto" [disabled]="!editable()" [(ngModel)]="texto" />
        </label>
        <label class="campo porcentaje">Porcentaje
          <input name="porcentaje" type="number" min="0" max="100" step="0.01" [disabled]="!editable()" [(ngModel)]="porcentaje" />
        </label>
        <label class="fila correcta">
          <input type="radio" [name]="'correcta-' + idPregunta()" [checked]="respuesta().esCorrecta === true"
                 [disabled]="!editable() || ocupado()" (change)="marcarCorrecta()" />
          Correcta
        </label>
      </div>
      <label class="campo">Justificación: por qué es correcta o incorrecta
        <textarea name="justificacion" rows="2" [disabled]="!editable()" [(ngModel)]="justificacion"></textarea>
      </label>
      @if (error(); as mensaje) { <p class="alerta alerta-error" role="alert">{{ mensaje }}</p> }
      @if (aviso(); as mensaje) { <p class="alerta alerta-exito" role="status">{{ mensaje }}</p> }
      @if (editable()) {
        <div class="fila fin">
          <button type="button" class="peligro" [disabled]="ocupado()" (click)="eliminar()">Eliminar opción</button>
          <button type="button" [disabled]="ocupado()" (click)="guardar()">Guardar opción</button>
        </div>
      }
    </div>
  `,
  styles: `
    .codigo { flex: 0 0 6rem; }
    .porcentaje { flex: 0 0 7rem; }
    .correcta { font-size: .875rem; font-weight: 600; flex-wrap: nowrap; }
  `,
})
export class EditorRespuesta extends ConOperaciones implements OnChanges {
  private readonly api = inject(CasosApiService);
  readonly idCaso = input.required<number>();
  readonly idPregunta = input.required<number>();
  readonly respuesta = input.required<Respuesta>();
  readonly editable = input.required<boolean>();
  readonly cambio = output<void>();

  protected codigo: string | null = null;
  protected texto: string | null = null;
  protected porcentaje: number | null = null;
  protected justificacion: string | null = null;

  ngOnChanges(): void {
    const respuesta = this.respuesta();
    this.codigo = respuesta.codigo;
    this.texto = respuesta.texto;
    this.porcentaje = respuesta.porcentaje;
    this.justificacion = respuesta.justificacion;
  }

  protected guardar(): void {
    const datos = {
      codigo: this.codigo,
      texto: this.texto,
      esCorrecta: this.respuesta().esCorrecta,
      porcentaje: this.porcentaje,
      justificacion: this.justificacion,
    };
    this.operar(this.api.actualizarRespuesta(this.idCaso(), this.respuesta().id, datos),
      () => this.cambio.emit(), 'Opción guardada.');
  }

  protected marcarCorrecta(): void {
    this.operar(this.api.marcarCorrecta(this.idCaso(), this.idPregunta(), this.respuesta().id),
      () => this.cambio.emit());
  }

  protected eliminar(): void {
    if (confirm('¿Eliminar esta opción de respuesta?')) {
      this.operar(this.api.eliminarRespuesta(this.idCaso(), this.respuesta().id), () => this.cambio.emit());
    }
  }
}
