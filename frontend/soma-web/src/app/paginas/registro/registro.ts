import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { mensajeDeError } from '../../core/con-operaciones';
import { InvitacionPublica, InvitacionesService } from '../../core/invitaciones.service';

/**
 * Pantalla pública a la que llega el estudiante desde el correo de invitación. El correo NO se escribe: sale de la
 * invitación. El estudiante crea su contraseña y llena sus datos; al terminar entra con su correo y esa contraseña.
 */
@Component({
  selector: 'app-registro',
  imports: [FormsModule, RouterLink],
  template: `
    <main class="registro">
      <section class="cristal tarjeta" aria-labelledby="titulo-registro">
        <p class="sobre">SOMA · Simulaciones para el aprendizaje en psicología social</p>
        <h1 id="titulo-registro">Crea tu cuenta</h1>

        @if (cargando()) { <div class="esqueleto" role="status" aria-label="Revisando tu invitación"><i></i><i></i></div> }
        @else if (errorInvitacion(); as e) {
          <p class="alerta-error" role="alert">{{ e }}</p>
          <p class="ayuda">Si ya tienes cuenta, <a routerLink="/login">inicia sesión</a>.</p>
        } @else if (creada()) {
          <p class="aviso-ok" role="status">Tu cuenta quedó creada. Ya puedes iniciar sesión con tu correo y tu contraseña.</p>
          <div class="acciones"><a class="boton-primario" routerLink="/login">Ir a iniciar sesión</a></div>
        } @else if (invitacion(); as i) {
          <p class="suave"><strong>{{ i.docente }}</strong> te invitó a SOMA. Completa tus datos para crear tu cuenta.</p>
          @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }

          <form class="formulario" (ngSubmit)="crear()" #f="ngForm" novalidate>
            <label class="campo-f">Correo (viene de tu invitación)
              <input type="email" name="correo" [value]="i.correo" readonly aria-readonly="true" />
            </label>
            <label class="campo-f">Nombre completo
              <input name="nombre" [(ngModel)]="nombre" required minlength="5" maxlength="150" autocomplete="name" placeholder="Nombre y apellidos" />
            </label>
            <div class="rejilla-2">
              <label class="campo-f">Tipo de documento
                <select name="tipo" [(ngModel)]="tipo" required>
                  <option value="CC">Cédula de ciudadanía</option>
                  <option value="TI">Tarjeta de identidad</option>
                  <option value="CE">Cédula de extranjería</option>
                  <option value="PEP">Permiso especial (PEP)</option>
                  <option value="PAS">Pasaporte</option>
                </select>
              </label>
              <label class="campo-f">Número de documento
                <input name="documento" [(ngModel)]="documento" required minlength="5" maxlength="15" autocomplete="off" inputmode="text" />
              </label>
            </div>
            <div class="rejilla-2">
              <label class="campo-f">Fecha de nacimiento
                <input type="date" name="nacimiento" [(ngModel)]="nacimiento" required [max]="hoy" autocomplete="bday" />
                @if (edad() !== null) { <span class="ayuda">{{ edad() }} años</span> }
              </label>
              <label class="campo-f">Teléfono (opcional)
                <input type="tel" name="telefono" [(ngModel)]="telefono" maxlength="20" autocomplete="tel" placeholder="3001234567" />
              </label>
            </div>
            <div class="rejilla-2">
              <label class="campo-f">Contraseña
                <input [type]="ver() ? 'text' : 'password'" name="clave" [(ngModel)]="clave" (ngModelChange)="claveActual.set($event)" required minlength="8" maxlength="72" autocomplete="new-password" />
              </label>
              <label class="campo-f">Repite la contraseña
                <input [type]="ver() ? 'text' : 'password'" name="clave2" [(ngModel)]="clave2" required autocomplete="new-password" />
              </label>
            </div>
            <label class="verclave"><input type="checkbox" [checked]="ver()" (change)="ver.set(!ver())" /> Mostrar contraseña</label>
            <ul class="reglas" aria-label="Requisitos de la contraseña">
              @for (r of reglas(); track r.texto) { <li [class.cumple]="r.ok">{{ r.ok ? '✓' : '•' }} {{ r.texto }}</li> }
            </ul>
            <label class="acepto">
              <input type="checkbox" name="acepto" [(ngModel)]="acepto" required />
              <span>Autorizo el tratamiento de mis datos personales para fines académicos dentro de SOMA.</span>
            </label>
            <div class="acciones">
              <button class="boton-primario" type="submit" [disabled]="ocupado()">{{ ocupado() ? 'Creando…' : 'Crear mi cuenta' }}</button>
            </div>
          </form>
        }
      </section>
    </main>
  `,
  styles: `
    .registro { min-height: 100vh; display: grid; place-items: center; padding: 2rem 1rem; }
    .tarjeta { width: min(640px, 100%); padding: 2rem 2.2rem; }
    .sobre { text-transform: uppercase; letter-spacing: .16em; font-size: .68rem; color: var(--lila-700); margin: 0 0 .3rem; }
    h1 { font-family: 'Cormorant Garamond', Georgia, serif; font-weight: 600; font-size: 2.2rem; margin: 0 0 .6rem; }
    .formulario { margin-top: 1rem; }
    input[readonly] { opacity: .75; cursor: not-allowed; }
    .reglas { list-style: none; margin: 0; padding: 0; display: flex; flex-wrap: wrap; gap: .3rem .9rem; font-size: .82rem; color: var(--tinta-suave); }
    .reglas .cumple { color: #2d6a4f; }
    .verclave, .acepto { display: flex; gap: .5rem; align-items: flex-start; font-size: .85rem; cursor: pointer; }
    a.boton-primario { text-decoration: none; display: inline-block; }
  `,
})
export class Registro implements OnInit {
  private readonly ruta = inject(ActivatedRoute);
  private readonly invitaciones = inject(InvitacionesService);
  private readonly navegar = inject(Router);

  protected readonly cargando = signal(true);
  protected readonly invitacion = signal<InvitacionPublica | null>(null);
  protected readonly errorInvitacion = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly creada = signal(false);
  protected readonly ver = signal(false);

  protected nombre = '';
  protected tipo = 'CC';
  protected documento = '';
  protected nacimiento = '';
  protected telefono = '';
  protected clave = '';
  protected clave2 = '';
  protected acepto = false;
  protected readonly hoy = new Date().toISOString().slice(0, 10);
  private token = '';

  protected readonly claveActual = signal('');
  protected readonly reglas = computed(() => {
    const c = this.claveActual();
    return [
      { texto: '8 caracteres o más', ok: c.length >= 8 },
      { texto: 'una mayúscula', ok: /[A-Z]/.test(c) },
      { texto: 'una minúscula', ok: /[a-z]/.test(c) },
      { texto: 'un número', ok: /[0-9]/.test(c) },
    ];
  });

  ngOnInit(): void {
    this.token = this.ruta.snapshot.paramMap.get('token') ?? '';
    this.invitaciones.consultar(this.token).subscribe({
      next: (i) => { this.invitacion.set(i); this.cargando.set(false); },
      error: (e: HttpErrorResponse) => { this.errorInvitacion.set(mensajeDeError(e)); this.cargando.set(false); },
    });
  }

  protected edad(): number | null {
    if (!this.nacimiento) { return null; }
    const n = new Date(this.nacimiento);
    if (Number.isNaN(n.getTime())) { return null; }
    const h = new Date();
    let e = h.getFullYear() - n.getFullYear();
    if (h.getMonth() < n.getMonth() || (h.getMonth() === n.getMonth() && h.getDate() < n.getDate())) { e--; }
    return e >= 0 ? e : null;
  }

  protected crear(): void {
    this.claveActual.set(this.clave);
    this.error.set(null);
    const faltantes = !this.nombre.trim() || !this.documento.trim() || !this.nacimiento || !this.clave;
    if (faltantes) { this.error.set('Completa todos los campos obligatorios.'); return; }
    if (this.nombre.trim().split(/\s+/).length < 2) { this.error.set('Escribe tu nombre completo (nombre y apellido).'); return; }
    if (this.reglas().some((r) => !r.ok)) { this.error.set('La contraseña no cumple los requisitos.'); return; }
    if (this.clave !== this.clave2) { this.error.set('Las contraseñas no coinciden.'); return; }
    if (!this.acepto) { this.error.set('Debes autorizar el tratamiento de tus datos para crear la cuenta.'); return; }
    this.ocupado.set(true);
    this.invitaciones.aceptar(this.token, {
      nombreCompleto: this.nombre.trim(), tipoDocumento: this.tipo, numeroDocumento: this.documento.trim(), fechaNacimiento: this.nacimiento,
      telefono: this.telefono.trim() || null, contrasena: this.clave, aceptaTratamientoDatos: this.acepto,
    }).subscribe({
      next: () => { this.ocupado.set(false); this.creada.set(true); this.clave = ''; this.clave2 = ''; setTimeout(() => void this.navegar.navigateByUrl('/login'), 4000); },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
    });
  }
}
