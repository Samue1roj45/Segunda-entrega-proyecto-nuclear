import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Se muestra cuando el usuario abre una pantalla para la que su rol no tiene permiso (RF-02). */
@Component({
  selector: 'app-acceso-denegado',
  imports: [RouterLink],
  template: `
    <section class="cristal bloque" style="text-align:center; padding:3rem 1.5rem; display:grid; gap:1rem; justify-items:center">
      <h2>No tienes acceso a esta pantalla</h2>
      <p class="suave">Tu rol no incluye este permiso. Si crees que es un error, comunícate con el administrador de SOMA.</p>
      <a routerLink="/inicio" class="boton-primario enlace-boton">Volver al inicio</a>
    </section>
  `,
})
export class AccesoDenegado {}
