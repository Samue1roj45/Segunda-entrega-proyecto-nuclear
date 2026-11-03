import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { ConOperaciones } from '../../core/con-operaciones';
import { SesionUsuario } from '../../core/modelos/sesion-usuario';
import { SesionService } from '../../core/sesion.service';

export type TipoIngreso = 'estudiante' | 'personal';

/**
 * Inicio de sesión del Módulo 1. El estudiante ingresa con su correo institucional y el código de
 * autorización de la convocatoria (RF-01); docentes y administrador, con correo y contraseña.
 * La interfaz solo exige los campos obligatorios: las credenciales las valida el servidor.
 */
@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <form #formulario="ngForm" class="tarjeta pila acceso" novalidate (ngSubmit)="ingresar(formulario)">
      <div>
        <h1>Iniciar sesión</h1>
        <p class="suave">Simulador académico de casos de estudio.</p>
      </div>

      <div class="selector" role="tablist" aria-label="Tipo de usuario">
        <button type="button" role="tab" [class.activo]="tipo() === 'estudiante'"
                [attr.aria-selected]="tipo() === 'estudiante'" (click)="cambiar('estudiante')">Estudiante</button>
        <button type="button" role="tab" [class.activo]="tipo() === 'personal'"
                [attr.aria-selected]="tipo() === 'personal'" (click)="cambiar('personal')">Docente o administrador</button>
      </div>

      <label class="campo">Correo institucional
        <input type="email" name="correo" autocomplete="username" required maxlength="150"
               [(ngModel)]="correo" #campoCorreo="ngModel" />
        @if (enviado() && campoCorreo.invalid) {
          <span class="falta" role="alert">El correo institucional es obligatorio.</span>
        }
      </label>

      @if (tipo() === 'estudiante') {
        <label class="campo">Código de autorización
          <input name="codigo" autocomplete="one-time-code" required maxlength="64"
                 [(ngModel)]="codigo" #campoCodigo="ngModel" />
          @if (enviado() && campoCodigo.invalid) {
            <span class="falta" role="alert">El código de autorización es obligatorio.</span>
          }
        </label>
        <p class="suave">Use el código que recibió en la convocatoria de la sesión.</p>
      } @else {
        <label class="campo">Contraseña
          <input type="password" name="clave" autocomplete="current-password" required maxlength="100"
                 [(ngModel)]="clave" #campoClave="ngModel" />
          @if (enviado() && campoClave.invalid) {
            <span class="falta" role="alert">La contraseña es obligatoria.</span>
          }
        </label>
      }

      @if (error(); as mensaje) { <p class="alerta alerta-error" role="alert">{{ mensaje }}</p> }
      <button type="submit" class="primario" [disabled]="ocupado()">
        {{ ocupado() ? 'Ingresando…' : 'Ingresar' }}
      </button>
    </form>
  `,
  styles: `
    .acceso { max-width: 25rem; margin: 3rem auto; }
    .selector { display: grid; grid-template-columns: 1fr 1fr; gap: .4rem; }
    .selector button.activo { background: var(--marino); border-color: var(--marino); color: #fff; }
    .falta { color: var(--rojo); font-weight: 400; font-size: .8rem; }
  `,
})
export class Login extends ConOperaciones {
  private readonly sesiones = inject(SesionService);
  private readonly router = inject(Router);
  protected readonly tipo = signal<TipoIngreso>('estudiante');
  protected readonly enviado = signal(false);
  protected correo = '';
  protected codigo = '';
  protected clave = '';

  protected cambiar(tipo: TipoIngreso): void {
    this.tipo.set(tipo);
    this.enviado.set(false);
    this.error.set(null);
  }

  /** Con campos vacíos no se envía la solicitud (CP-RF01-05); el resto lo decide el servidor. */
  protected ingresar(formulario: NgForm): void {
    this.enviado.set(true);
    this.error.set(null);
    if (formulario.invalid) {
      return;
    }
    const correo = this.correo.trim();
    const peticion: Observable<SesionUsuario> =
      this.tipo() === 'estudiante'
        ? this.sesiones.iniciarEstudiante(correo, this.codigo.trim())
        : this.sesiones.iniciar(correo, this.clave);
    this.operar(peticion, () => void this.router.navigate(['/inicio']));
  }
}
