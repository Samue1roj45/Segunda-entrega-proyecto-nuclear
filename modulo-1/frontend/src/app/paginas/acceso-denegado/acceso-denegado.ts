import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Se muestra cuando la sesión no tiene el permiso de la pantalla solicitada (CP-RF02-02, 04 y 05). */
@Component({
  selector: 'app-acceso-denegado',
  imports: [RouterLink],
  template: `
    <section class="tarjeta pila aviso" role="alert">
      <h1>Acceso denegado</h1>
      <p>No tiene permiso para acceder a esta función.</p>
      <a routerLink="/inicio">Volver al inicio</a>
    </section>
  `,
  styles: `.aviso { max-width: 28rem; margin: 3rem auto; text-align: center; }`,
})
export class AccesoDenegado {}
