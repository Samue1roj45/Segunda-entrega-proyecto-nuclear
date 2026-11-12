import { Component, OnChanges, inject, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CasosApiService } from '../../core/casos-api.service';
import { ConOperaciones } from '../../core/con-operaciones';
import { Pregunta } from '../../core/modelos/caso';
import { EditorRespuesta } from './editor-respuesta';

const CODIGO_LETRA_A = 65;

/** RF-07: enunciado y opciones de respuesta de una pregunta. */
@Component({
  selector: 'app-editor-pregunta',
  imports: [FormsModule, EditorRespuesta],
  template: `
    <div class="bloque-interno pila-corta">
      <h3>Pregunta {{ pregunta().orden }}</h3>
      <label class="campo">Enunciado
        <textarea name="enunciado" rows="2" [disabled]="!editable()" [(ngModel)]="enunciado"></textarea>
      </label>
      @if (error(); as mensaje) { <p class="alerta alerta-error" role="alert">{{ mensaje }}</p> }
      @if (aviso(); as mensaje) { <p class="alerta alerta-exito" role="status">{{ mensaje }}</p> }
      @if (editable()) {
        <div class="fila fin">
          <button type="button" class="peligro" [disabled]="ocupado()" (click)="eliminar()">Eliminar pregunta</button>
          <button type="button" [disabled]="ocupado()" (click)="guardar()">Guardar enunciado</button>
        </div>
      }

      @for (respuesta of pregunta().respuestas; track respuesta.id) {
        <app-editor-respuesta [idCaso]="idCaso()" [idPregunta]="pregunta().id" [respuesta]="respuesta"
                              [editable]="editable()" (cambio)="cambio.emit()" />
      } @empty {
        <p class="vacio">Esta pregunta todavía no tiene opciones de respuesta.</p>
      }
      @if (editable()) {
        <div><button type="button" [disabled]="ocupado()" (click)="agregarOpcion()">Agregar opción</button></div>
      }
    </div>
  `,
})
export class EditorPregunta extends ConOperaciones implements OnChanges {
  private readonly api = inject(CasosApiService);
  readonly idCaso = input.required<number>();
  readonly pregunta = input.required<Pregunta>();
  readonly editable = input.required<boolean>();
  readonly cambio = output<void>();

  protected enunciado: string | null = null;

  ngOnChanges(): void {
    this.enunciado = this.pregunta().enunciado;
  }

  protected guardar(): void {
    this.operar(this.api.actualizarPregunta(this.idCaso(), this.pregunta().id, this.enunciado),
      () => this.cambio.emit(), 'Enunciado guardado.');
  }

  protected eliminar(): void {
    if (confirm('¿Eliminar esta pregunta con todas sus opciones?')) {
      this.operar(this.api.eliminarPregunta(this.idCaso(), this.pregunta().id), () => this.cambio.emit());
    }
  }

  /** La opción nueva nace incorrecta y con la siguiente letra libre; el docente puede cambiarlas. */
  protected agregarOpcion(): void {
    const codigo = String.fromCharCode(CODIGO_LETRA_A + this.pregunta().respuestas.length);
    const datos = { codigo, texto: null, esCorrecta: false, porcentaje: null, justificacion: null };
    this.operar(this.api.agregarRespuesta(this.idCaso(), this.pregunta().id, datos), () => this.cambio.emit());
  }
}
