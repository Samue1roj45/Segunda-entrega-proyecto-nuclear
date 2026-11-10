import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { SesionService } from './core/sesion.service';

/** Marco de la aplicación según la Figura 13. Las demás secciones del panel son de otros módulos. */
@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterOutlet],
  template: `
    <header class="cabecera">
      <a class="marca" routerLink="/inicio">HUMBOLDT &nbsp;/&nbsp; SIMULADOR PSICOSOCIAL</a>
      @if (sesiones.sesion(); as sesion) {
        <div class="fila">
          <span>{{ sesion.nombreCompleto }}</span>
          <button type="button" class="salir" (click)="salir()">Cerrar sesión</button>
        </div>
      }
    </header>
    <main class="contenido"><router-outlet /></main>
  `,
  styles: `
    .cabecera {
      background: var(--marino); color: #fff; padding: .9rem 1.5rem; display: flex;
      justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap; font-size: .9rem;
    }
    .marca { color: #fff; font-weight: 700; text-decoration: none; }
    .contenido { max-width: 1060px; margin: 0 auto; padding: 1.5rem 1rem 3rem; }
    .salir { background: transparent; color: #fff; border-color: #ffffff66; }
    .salir:hover:not(:disabled) { background: #ffffff1a; }
  `,
})
export class App {
  protected readonly sesiones = inject(SesionService);
  private readonly router = inject(Router);

  protected salir(): void {
    this.sesiones.cerrar();
    void this.router.navigate(['/login']);
  }
}
