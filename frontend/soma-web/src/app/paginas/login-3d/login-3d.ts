import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { Fondo3d } from '../../escena3d/fondo-3d';
import { Logo } from '../../compartido/logo';
import { ConOperaciones } from '../../core/con-operaciones';
import { SesionUsuario } from '../../core/modelos/sesion-usuario';
import { SesionService } from '../../core/sesion.service';

/**
 * Inicio de sesión (Módulo 1, RF-01). Docentes y administrador entran con correo y contraseña; el
 * estudiante, con su correo y el código de acceso que recibió en la convocatoria. Las credenciales
 * las valida el servidor; aquí solo se exigen los campos obligatorios.
 */
@Component({
  selector: 'app-login-3d',
  imports: [FormsModule, Fondo3d, Logo],
  template: `
    <soma-fondo-3d />

    <main class="escenario">
      <section class="marca" aria-label="SOMA">
        <soma-logo [tamano]="150" />
        <p class="palabra">SOMA</p>
        <p class="lema">Simulaciones para el aprendizaje<br />en psicología social</p>
        <span class="linea" aria-hidden="true"></span>
        <h2 class="titular">Explora realidades,<br />comprende personas.</h2>
        <p class="bajada">
          Simulaciones interactivas para analizar, reflexionar y construir soluciones en contextos sociales reales.
        </p>
      </section>

      <form #formulario="ngForm" class="tarjeta cristal" novalidate (ngSubmit)="ingresar(formulario)">
        <header>
          <h1>Bienvenido a SOMA</h1>
          <p class="suave">Inicia sesión para continuar con tu experiencia de aprendizaje</p>
        </header>

        <label class="campo">
          <span class="sr-only">Correo institucional</span>
          <svg class="icono" viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2.5" /><path d="m4 7 8 6 8-6" /></svg>
          <input type="email" name="correo" placeholder="Correo institucional" autocomplete="username" required
                 maxlength="150" [(ngModel)]="correo" #campoCorreo="ngModel" />
        </label>
        @if (enviado() && campoCorreo.invalid) {
          <span class="falta" role="alert">Escribe tu correo institucional.</span>
        }

        @if (esEstudiante()) {
          <label class="campo">
            <span class="sr-only">Código de acceso</span>
            <svg class="icono" viewBox="0 0 24 24" aria-hidden="true"><circle cx="8" cy="12" r="4" /><path d="M12 12h9m-3 0v3m-3-3v2" /></svg>
            <input name="codigo" placeholder="Código de acceso de la convocatoria" autocomplete="one-time-code"
                   required maxlength="64" [(ngModel)]="codigo" #campoCodigo="ngModel" />
          </label>
          @if (enviado() && campoCodigo.invalid) {
            <span class="falta" role="alert">Escribe el código que recibiste por correo.</span>
          }
        } @else {
          <label class="campo">
            <span class="sr-only">Contraseña</span>
            <svg class="icono" viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="10" width="14" height="10" rx="2.5" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></svg>
            <input [type]="verClave() ? 'text' : 'password'" name="clave" placeholder="Contraseña"
                   autocomplete="current-password" required maxlength="100" [(ngModel)]="clave" #campoClave="ngModel" />
            <button type="button" class="ojo" (click)="verClave.set(!verClave())"
                    [attr.aria-label]="verClave() ? 'Ocultar contraseña' : 'Mostrar contraseña'">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12Z" /><circle cx="12" cy="12" r="3" />
                @if (!verClave()) { <path d="M4 4l16 16" /> }
              </svg>
            </button>
          </label>
          @if (enviado() && campoClave.invalid) {
            <span class="falta" role="alert">Escribe tu contraseña.</span>
          }
        }

        <div class="opciones">
          <label class="recordar">
            <input type="checkbox" name="recordar" [(ngModel)]="recordar" /> Recordarme
          </label>
          <button type="button" class="enlace" (click)="noDisponible('recuperar')">¿Olvidaste tu contraseña?</button>
        </div>

        @if (error(); as mensaje) { <p class="alerta-error" role="alert">{{ mensaje }}</p> }
        @if (info(); as mensaje) { <p class="info" role="status">{{ mensaje }}</p> }

        <button type="submit" class="boton-primario ingresar" [disabled]="ocupado()">
          <span>{{ ocupado() ? 'Ingresando…' : 'Ingresar' }}</span>
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14m-5-5 5 5-5 5" /></svg>
        </button>

        <button type="button" class="enlace cambio" (click)="alternarTipo()">
          {{ esEstudiante() ? '¿Eres docente o administrador? Ingresa con tu contraseña'
                            : '¿Eres estudiante? Ingresa con tu código de acceso' }}
        </button>

        <div class="separador"><span>o continúa con</span></div>
        <div class="sociales">
          <button type="button" class="social" aria-label="Continuar con Google" (click)="noDisponible('social')">
            <span class="g">G</span>
          </button>
          <button type="button" class="social" aria-label="Continuar con Microsoft" (click)="noDisponible('social')">
            <svg viewBox="0 0 24 24" aria-hidden="true" class="ms"><rect x="3" y="3" width="8" height="8" /><rect x="13" y="3" width="8" height="8" /><rect x="3" y="13" width="8" height="8" /><rect x="13" y="13" width="8" height="8" /></svg>
          </button>
          <button type="button" class="social" aria-label="Continuar con Apple" (click)="noDisponible('social')">
            <svg viewBox="0 0 24 24" aria-hidden="true" class="apple"><path d="M16.4 12.6c0-2.3 1.9-3.4 2-3.5-1.1-1.6-2.8-1.8-3.4-1.8-1.4-.1-2.8.9-3.5.9-.7 0-1.9-.9-3.1-.8-1.6 0-3.1.9-3.9 2.4-1.7 2.9-.4 7.2 1.2 9.6.8 1.2 1.8 2.5 3 2.4 1.2 0 1.7-.8 3.1-.8s1.9.8 3.1.8c1.3 0 2.1-1.2 2.9-2.4.9-1.3 1.3-2.6 1.3-2.7-.1 0-2.6-1-2.7-4.1ZM14.2 5.6c.6-.8 1.1-1.9 1-3-.9 0-2.1.6-2.7 1.4-.6.7-1.1 1.8-1 2.9 1 .1 2.1-.5 2.7-1.3Z" /></svg>
          </button>
        </div>

        <p class="registro">¿No tienes una cuenta? <button type="button" class="enlace fuerte" (click)="noDisponible('registro')">Regístrate</button></p>
      </form>
    </main>
  `,
  styles: `
    :host { display: block; min-height: 100vh; }
    .escenario {
      position: relative; z-index: 1; min-height: 100vh;
      display: grid; grid-template-columns: 1.1fr minmax(360px, 480px); align-items: center;
      gap: clamp(2rem, 6vw, 7rem); padding: clamp(1.5rem, 5vw, 5rem) clamp(1.5rem, 7vw, 8rem);
    }
    .marca { display: grid; justify-items: start; gap: .35rem; max-width: 30rem; padding-left: 4rem; }
    .marca soma-logo { margin-left: 3.2rem; filter: drop-shadow(0 18px 24px rgba(59,45,92,.18)); }
    .palabra { font-family: var(--serif); font-size: clamp(4.2rem, 8vw, 6.4rem); line-height: .9; color: var(--lila-950); letter-spacing: .04em; }
    .lema { text-transform: uppercase; letter-spacing: .22em; font-size: .78rem; color: var(--lila-800); text-align: center; line-height: 1.6; }
    .linea { width: 3rem; height: 2px; background: var(--lila-800); margin: 1.6rem 0 .8rem; border-radius: 2px; }
    .titular { font-size: clamp(2rem, 3.2vw, 2.7rem); line-height: 1.05; color: var(--lila-950); }
    .bajada { margin-top: .9rem; color: var(--tinta); font-size: 1.05rem; line-height: 1.55; max-width: 24rem; }

    .tarjeta { padding: 2.6rem 2.4rem 2rem; display: grid; gap: .95rem; }
    .tarjeta header { text-align: center; display: grid; gap: .5rem; margin-bottom: .9rem; }
    h1 { font-size: 2.6rem; }
    header .suave { font-size: 1rem; line-height: 1.45; padding: 0 1.4rem; }

    .campo {
      position: relative; display: flex; align-items: center; gap: .8rem;
      padding: 0 1.2rem; height: 3.6rem; border-radius: 18px;
      background: rgba(255,255,255,.55); border: 1px solid rgba(255,255,255,.9);
      box-shadow: inset 0 1px 2px rgba(59,45,92,.06); transition: border-color .15s, background .15s;
    }
    .campo:focus-within { background: rgba(255,255,255,.8); border-color: var(--lila-300); }
    .campo input { flex: 1; border: 0; background: transparent; font: inherit; font-size: 1rem; color: var(--tinta); outline: none; min-width: 0; }
    .campo input::placeholder { color: #8d84a3; }
    .icono, .ojo svg { width: 22px; height: 22px; fill: none; stroke: var(--lila-800); stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; flex: none; }
    .ojo { border: 0; background: none; padding: .3rem; display: flex; border-radius: 8px; }
    .falta { color: var(--alerta); font-size: .82rem; margin: -.5rem 0 0 1rem; }

    .opciones { display: flex; justify-content: space-between; align-items: center; font-size: .95rem; margin-top: .2rem; }
    .recordar { display: flex; align-items: center; gap: .55rem; color: var(--tinta); cursor: pointer; }
    .recordar input { width: 1.2rem; height: 1.2rem; accent-color: var(--lila-800); }
    .enlace { border: 0; background: none; padding: 0; color: var(--lila-900); font-size: .95rem; }
    .enlace:hover { text-decoration: underline; }
    .fuerte { font-weight: 600; }

    .ingresar { width: 100%; height: 3.6rem; font-size: 1.1rem; justify-content: center; position: relative; margin-top: .4rem; }
    .ingresar svg { position: absolute; right: 1.6rem; width: 20px; height: 20px; fill: none; stroke: #fff; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }
    .cambio { font-size: .85rem; color: var(--lila-700); justify-self: center; }

    .separador { display: flex; align-items: center; gap: 1rem; color: var(--tinta-suave); font-size: .9rem; margin-top: .4rem; }
    .separador::before, .separador::after { content: ''; flex: 1; height: 1px; background: rgba(94,74,138,.25); }
    .sociales { display: flex; justify-content: center; gap: 1.6rem; }
    .social {
      width: 4.4rem; height: 4.2rem; border-radius: 16px; display: grid; place-items: center;
      background: rgba(255,255,255,.55); border: 1px solid rgba(255,255,255,.9); color: var(--lila-900);
      transition: background .15s, transform .15s;
    }
    .social:hover { background: rgba(255,255,255,.85); transform: translateY(-1px); }
    .g { font-weight: 600; font-size: 1.7rem; color: var(--lila-800); }
    .ms { width: 24px; height: 24px; fill: var(--lila-800); }
    .apple { width: 26px; height: 26px; fill: var(--lila-800); }
    .registro { text-align: center; color: var(--tinta-suave); font-size: .95rem; }
    .info { padding: .7rem 1rem; border-radius: 14px; background: rgba(255,255,255,.6); color: var(--lila-900); font-size: .88rem; }

    @media (max-width: 960px) {
      .escenario { grid-template-columns: 1fr; justify-items: center; }
      .marca { padding-left: 0; justify-items: center; text-align: center; }
      .marca soma-logo { margin-left: 0; }
      .titular, .bajada, .linea { display: none; }
      .tarjeta { width: 100%; max-width: 480px; }
    }
  `,
})
export class Login3d extends ConOperaciones {
  private readonly sesiones = inject(SesionService);
  private readonly router = inject(Router);

  protected readonly esEstudiante = signal(false);
  protected readonly verClave = signal(false);
  protected readonly enviado = signal(false);
  protected readonly info = signal<string | null>(null);
  protected correo = '';
  protected clave = '';
  protected codigo = '';
  protected recordar = false;

  protected alternarTipo(): void {
    this.esEstudiante.set(!this.esEstudiante());
    this.enviado.set(false);
    this.error.set(null);
    this.info.set(null);
  }

  /** Funciones del diseño que el backend todavía no ofrece: se avisa en lugar de fallar en silencio. */
  protected noDisponible(que: 'recuperar' | 'social' | 'registro'): void {
    this.error.set(null);
    this.info.set(
      que === 'recuperar'
        ? 'Para restablecer tu contraseña, comunícate con el administrador de SOMA.'
        : que === 'registro'
          ? 'Las cuentas las crea el administrador; los estudiantes entran con el código de su convocatoria.'
          : 'El ingreso con Google, Microsoft o Apple estará disponible próximamente.',
    );
  }

  protected ingresar(formulario: NgForm): void {
    this.enviado.set(true);
    this.info.set(null);
    if (formulario.invalid) {
      return;
    }
    const correo = this.correo.trim();
    const peticion: Observable<SesionUsuario> = this.esEstudiante()
      ? this.sesiones.iniciarEstudiante(correo, this.codigo.trim(), this.recordar)
      : this.sesiones.iniciar(correo, this.clave, this.recordar);
    this.operar(peticion, () => void this.router.navigate(['/inicio']));
  }
}
