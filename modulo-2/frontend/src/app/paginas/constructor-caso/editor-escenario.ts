import { Component, OnChanges, inject, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CasosApiService } from '../../core/casos-api.service';
import { ConOperaciones } from '../../core/con-operaciones';
import { Escenario } from '../../core/modelos/caso';
import { EditorPregunta } from './editor-pregunta';

/** RF-06: nombre, contexto situacional y preguntas de un escenario. */
@Component({
  selector: 'app-editor-escenario',
  imports: [FormsModule, EditorPregunta],
  template: `
    <section class="bloque pila">
      <h2>Escenario {{ escenario().orden }}</h2>
      <label class="campo">Nombre
        <input name="nombre" maxlength="150" [disabled]="!editable()" [(ngModel)]="nombre" />
      </label>
      <label class="campo">Contexto situacional
        <textarea name="contexto" rows="3" [disabled]="!editable()" [(ngModel)]="contexto"></textarea>
      </label>
      @if (error(); as mensaje) { <p class="alerta alerta-error" role="alert">{{ mensaje }}</p> }
      @if (aviso(); as mensaje) { <p class="alerta alerta-exito" role="status">{{ mensaje }}</p> }
      @if (editable()) {
        <div class="fila fin">
          <button type="button" class="peligro" [disabled]="ocupado()" (click)="eliminar()">Eliminar escenario</button>
          <button type="button" class="primario" [disabled]="ocupado()" (click)="guardar()">Guardar escenario</button>
        </div>
      }

      @for (pregunta of escenario().preguntas; track pregunta.id) {
        <app-editor-pregunta [idCaso]="idCaso()" [pregunta]="pregunta" [editable]="editable()" (cambio)="cambio.emit()" />
      } @empty {
        <p class="vacio">Este escenario todavía no tiene preguntas.</p>
      }
      @if (editable()) {
        <div><button type="button" [disabled]="ocupado()" (click)="agregarPregunta()">Agregar pregunta</button></div>
      }
    </section>
  `,
})
export class EditorEscenario extends ConOperaciones implements OnChanges {
  private readonly api = inject(CasosApiService);
  readonly idCaso = input.required<number>();
  readonly escenario = input.required<Escenario>();
  readonly editable = input.required<boolean>();
  readonly cambio = output<void>();

  protected nombre: string | null = null;
  protected contexto: string | null = null;

  ngOnChanges(): void {
    this.nombre = this.escenario().nombre;
    this.contexto = this.escenario().contexto;
  }

  protected guardar(): void {
    this.operar(this.api.actualizarEscenario(this.idCaso(), this.escenario().id, this.nombre, this.contexto),
      () => this.cambio.emit(), 'Escenario guardado.');
  }

  protected eliminar(): void {
    if (confirm('¿Eliminar este escenario con todas sus preguntas y opciones?')) {
      this.operar(this.api.eliminarEscenario(this.idCaso(), this.escenario().id), () => this.cambio.emit());
    }
  }

  protected agregarPregunta(): void {
    this.operar(this.api.agregarPregunta(this.idCaso(), this.escenario().id, null), () => this.cambio.emit());
  }
}
