import { Component, inject, input, output } from '@angular/core';
import { CasosApiService } from '../../core/casos-api.service';
import { ConOperaciones } from '../../core/con-operaciones';
import { CasoDetalle } from '../../core/modelos/caso';
import { EditorEscenario } from './editor-escenario';

/** RF-06: escenarios del caso. */
@Component({
  selector: 'app-seccion-escenarios',
  imports: [EditorEscenario],
  template: `
    <div class="pila">
      @for (escenario of caso().escenarios; track escenario.id) {
        <app-editor-escenario [idCaso]="caso().id" [escenario]="escenario" [editable]="editable()" (cambio)="cambio.emit()" />
      } @empty {
        <p class="vacio">El caso todavía no tiene escenarios.</p>
      }
      @if (error(); as mensaje) { <p class="alerta alerta-error" role="alert">{{ mensaje }}</p> }
      @if (editable()) {
        <div><button type="button" class="primario" [disabled]="ocupado()" (click)="agregar()">Agregar escenario</button></div>
      }
    </div>
  `,
})
export class SeccionEscenarios extends ConOperaciones {
  private readonly api = inject(CasosApiService);
  readonly caso = input.required<CasoDetalle>();
  readonly editable = input.required<boolean>();
  readonly cambio = output<void>();

  protected agregar(): void {
    this.operar(this.api.agregarEscenario(this.caso().id, null, null), () => this.cambio.emit());
  }
}
