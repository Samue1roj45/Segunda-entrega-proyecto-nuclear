import { Component } from '@angular/core';

import { SimulacionPlayComponent } from './simulacion-play/simulacion-play.component';

/**
 * Contenedor de ruta /estudiante/simulacion.
 * La lógica de API y el motor canvas viven en SimulacionPlayComponent.
 * La UI clásica (quiz) permanece en simulacion.html por si se necesita referencia.
 */
@Component({
  selector: 'app-simulacion',
  imports: [SimulacionPlayComponent],
  template: `<app-simulacion-play />`,
  styles: [
    `
      :host {
        display: flex;
        flex-direction: column;
        flex: 1;
        min-height: calc(100dvh - 64px);
        margin: -1.25rem;
        width: calc(100% + 2.5rem);
        max-width: none;
      }
      @media (max-width: 600px) {
        :host {
          margin: -0.75rem;
          width: calc(100% + 1.5rem);
        }
      }
    `,
  ],
})
export class Simulacion {}
