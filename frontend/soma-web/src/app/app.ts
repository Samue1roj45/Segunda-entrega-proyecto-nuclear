import { Component, inject } from '@angular/core';
import { ConexionService } from './core/red';
import { PreferenciasService } from './core/preferencias.service';
import { RouterOutlet } from '@angular/router';
import { Dialogos } from './compartido/dialogos';
import { Toasts } from './compartido/toasts';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toasts, Dialogos],
  template: `<a class="saltar" href="#contenido" (click)="saltar($event)">Saltar al contenido</a><router-outlet /><soma-toasts /><soma-dialogos />
    @if (!conexion.enLinea()) { <div class="sin-red" role="status">Sin conexión · mostrando los últimos datos guardados</div> }`,
  styles: `.sin-red { position: fixed; left: 50%; bottom: 1rem; transform: translateX(-50%); z-index: 500; padding: .5rem 1.1rem; border-radius: 999px; background: #3b2f5c; color: #fff; font-size: .85rem; box-shadow: 0 8px 24px rgba(0,0,0,.25); }`,
})
export class App {
  protected readonly conexion = inject(ConexionService);
  constructor() { inject(PreferenciasService); }
  protected saltar(e: Event): void { e.preventDefault(); const m = document.getElementById('contenido'); m?.focus(); m?.scrollIntoView(); }
}
