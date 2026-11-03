import { Component, OnChanges, inject, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CasosApiService } from '../../core/casos-api.service';
import { ConOperaciones } from '../../core/con-operaciones';
import { CasoDetalle } from '../../core/modelos/caso';
import { EditorCriterio } from './editor-criterio';

/** RF-17: rúbrica asociada al caso y sus criterios. */
@Component({
  selector: 'app-seccion-rubrica',
  imports: [FormsModule, EditorCriterio],
  template: `
    <section class="tarjeta pila">
      <h2>Rúbrica del caso</h2>
      @if (!caso().rubrica && !editable()) {
        <p class="vacio">Este caso no tiene rúbrica.</p>
      } @else {
        <label class="campo">Nombre
          <input name="nombre" maxlength="150" [disabled]="!editable()" [(ngModel)]="nombre" />
        </label>
        <label class="campo">Descripción
          <textarea name="descripcion" rows="3" [disabled]="!editable()" [(ngModel)]="descripcion"></textarea>
        </label>
      }
      @if (error(); as mensaje) { <p class="alerta alerta-error" role="alert">{{ mensaje }}</p> }
      @if (aviso(); as mensaje) { <p class="alerta alerta-exito" role="status">{{ mensaje }}</p> }
      @if (editable()) {
        <div class="fila fin">
          @if (caso().rubrica) {
            <button type="button" class="peligro" [disabled]="ocupado()" (click)="retirar()">Retirar rúbrica</button>
          }
          <button type="button" class="primario" [disabled]="ocupado()" (click)="registrar()">
            {{ caso().rubrica ? 'Guardar rúbrica' : 'Crear y asociar rúbrica' }}
          </button>
        </div>
      }

      @if (caso().rubrica; as rubrica) {
        <h3>Criterios de evaluación</h3>
        @for (criterio of rubrica.criteriosEvaluacion; track criterio.id) {
          <app-editor-criterio [idCaso]="caso().id" [criterio]="criterio" [editable]="editable()" (cambio)="cambio.emit()" />
        } @empty {
          <p class="vacio">La rúbrica todavía no tiene criterios.</p>
        }
        @if (editable()) {
          <div><button type="button" [disabled]="ocupado()" (click)="agregarCriterio()">Agregar criterio</button></div>
        }
      }
    </section>
  `,
})
export class SeccionRubrica extends ConOperaciones implements OnChanges {
  private readonly api = inject(CasosApiService);
  readonly caso = input.required<CasoDetalle>();
  readonly editable = input.required<boolean>();
  readonly cambio = output<void>();

  protected nombre: string | null = null;
  protected descripcion: string | null = null;

  ngOnChanges(): void {
    this.nombre = this.caso().rubrica?.nombre ?? null;
    this.descripcion = this.caso().rubrica?.descripcion ?? null;
  }

  protected registrar(): void {
    this.operar(this.api.registrarRubrica(this.caso().id, this.nombre, this.descripcion),
      () => this.cambio.emit(), 'Rúbrica guardada y asociada al caso.');
  }

  protected retirar(): void {
    if (confirm('¿Retirar la rúbrica del caso con todos sus criterios?')) {
      this.operar(this.api.retirarRubrica(this.caso().id), () => this.cambio.emit());
    }
  }

  protected agregarCriterio(): void {
    const datos = { criterio: null, parametros: [], rango: { min: null, max: null } };
    this.operar(this.api.agregarCriterio(this.caso().id, datos), () => this.cambio.emit());
  }
}
