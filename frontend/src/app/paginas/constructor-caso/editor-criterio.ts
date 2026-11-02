import { Component, OnChanges, inject, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CasosApiService } from '../../core/casos-api.service';
import { ConOperaciones } from '../../core/con-operaciones';
import { Criterio } from '../../core/modelos/caso';

/** RF-17: criterio de evaluación, sus parámetros y su rango. */
@Component({
  selector: 'app-editor-criterio',
  imports: [FormsModule],
  template: `
    <div class="bloque-interno pila-corta">
      <div class="fila">
        <label class="campo">Criterio
          <input name="criterio" maxlength="150" [disabled]="!editable()" [(ngModel)]="nombre" />
        </label>
        <label class="campo rango">Mínimo
          <input name="min" type="number" step="0.01" [disabled]="!editable()" [(ngModel)]="min" />
        </label>
        <label class="campo rango">Máximo
          <input name="max" type="number" step="0.01" [disabled]="!editable()" [(ngModel)]="max" />
        </label>
      </div>
      <label class="campo">Parámetros o descriptores (uno por línea)
        <textarea name="parametros" rows="2" [disabled]="!editable()" [(ngModel)]="parametros"></textarea>
      </label>
      @if (error(); as mensaje) { <p class="alerta alerta-error" role="alert">{{ mensaje }}</p> }
      @if (aviso(); as mensaje) { <p class="alerta alerta-exito" role="status">{{ mensaje }}</p> }
      @if (editable()) {
        <div class="fila fin">
          <button type="button" class="peligro" [disabled]="ocupado()" (click)="eliminar()">Eliminar criterio</button>
          <button type="button" [disabled]="ocupado()" (click)="guardar()">Guardar criterio</button>
        </div>
      }
    </div>
  `,
  styles: `.rango { flex: 0 0 7rem; }`,
})
export class EditorCriterio extends ConOperaciones implements OnChanges {
  private readonly api = inject(CasosApiService);
  readonly idCaso = input.required<number>();
  readonly criterio = input.required<Criterio>();
  readonly editable = input.required<boolean>();
  readonly cambio = output<void>();

  protected nombre: string | null = null;
  protected parametros = '';
  protected min: number | null = null;
  protected max: number | null = null;

  ngOnChanges(): void {
    const criterio = this.criterio();
    this.nombre = criterio.criterio;
    this.parametros = criterio.parametros.join('\n');
    this.min = criterio.rango.min;
    this.max = criterio.rango.max;
  }

  protected guardar(): void {
    const datos = {
      criterio: this.nombre,
      parametros: this.parametros.split('\n'),
      rango: { min: this.min, max: this.max },
    };
    this.operar(this.api.actualizarCriterio(this.idCaso(), this.criterio().id, datos),
      () => this.cambio.emit(), 'Criterio guardado.');
  }

  protected eliminar(): void {
    if (confirm('¿Eliminar este criterio?')) {
      this.operar(this.api.eliminarCriterio(this.idCaso(), this.criterio().id), () => this.cambio.emit());
    }
  }
}
