import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ConOperaciones } from '../../core/con-operaciones';
import { SesionService } from '../../core/sesion.service';

/** Formulario sobre el inicio de sesión de personal del Módulo 1. */
@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <form class="tarjeta pila acceso" (ngSubmit)="ingresar()">
      <div>
        <h1>Iniciar sesión</h1>
        <p class="suave">Acceso para docentes con permiso de gestión de casos.</p>
      </div>
      <label class="campo">Correo institucional
        <input type="email" name="correo" autocomplete="username" required [(ngModel)]="correo" />
      </label>
      <label class="campo">Contraseña
        <input type="password" name="clave" autocomplete="current-password" required [(ngModel)]="clave" />
      </label>
      @if (error(); as mensaje) { <p class="alerta alerta-error" role="alert">{{ mensaje }}</p> }
      <button type="submit" class="primario" [disabled]="ocupado()">
        {{ ocupado() ? 'Ingresando…' : 'Ingresar' }}
      </button>
    </form>
  `,
  styles: `.acceso { max-width: 24rem; margin: 3rem auto; }`,
})
export class Login extends ConOperaciones {
  private readonly sesiones = inject(SesionService);
  private readonly router = inject(Router);
  protected correo = '';
  protected clave = '';

  protected ingresar(): void {
    this.operar(this.sesiones.iniciar(this.correo, this.clave), () => {
      if (this.sesiones.puedeGestionarCasos()) {
        void this.router.navigate(['/docente/casos']);
      } else {
        this.sesiones.cerrar();
        this.error.set('Su cuenta no tiene permiso para gestionar casos de estudio.');
      }
    });
  }
}
