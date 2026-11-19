import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { ConOperaciones } from '../../core/con-operaciones';
import { SesionUsuario } from '../../core/modelos/sesion-usuario';
import { SesionService } from '../../core/sesion.service';

/**
 * Inicio de sesión (Módulo 1, RF-01). Docentes y administrador entran con correo y contraseña; el
 * estudiante, con su correo y el código de acceso que recibió en la convocatoria. Las credenciales
 * las valida el servidor; aquí solo se exigen los campos obligatorios.
 */
@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <div class="escena">
      <!-- Render de cristal de la referencia, sin el contenido del panel (que se dibuja abajo con campos reales). -->
      <img class="fondo" src="fondo-login.svg" alt="" width="1672" height="941" fetchpriority="high" decoding="async" (load)="listo.set(true)" [class.listo]="listo()" />
      <div class="marca" aria-hidden="true">
        <img class="isotipo" src="logo-soma.svg" alt="" />
        <img class="nombre" src="soma-letras.svg" alt="" />
        <div class="sub">SIMULACIONES PARA EL APRENDIZAJE<br />EN PSICOLOGÍA SOCIAL</div>
        <div class="linea"></div>
        <div class="lema">Explora realidades,<br />comprende personas.</div>
        <p class="parrafo">Simulaciones interactivas para analizar,<br />reflexionar y construir soluciones<br />en contextos sociales reales.</p>
      </div>
      <h1 class="sr-only">SOMA · Simulaciones para el aprendizaje en psicología social. Explora realidades, comprende personas.</h1>

      <form #formulario="ngForm" class="tarjeta" novalidate (ngSubmit)="ingresar(formulario)">
        <div class="interior">
          <h2 class="titulo">Bienvenido a SOMA</h2>
          <p class="subtitulo">Inicia sesión para continuar con tu<br />experiencia de aprendizaje</p>

          @if (error(); as mensaje) { <p class="aviso error" role="alert">{{ mensaje }}</p> }
          @else if (info(); as mensaje) { <p class="aviso" role="status">{{ mensaje }}</p> }

          <label class="campo c1" [class.invalido]="enviado() && campoCorreo.invalid">
            <span class="sr-only">Correo institucional</span>
            <svg class="icono" viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2.5" /><path d="m4 7.5 8 5.5 8-5.5" /></svg>
            <input type="email" name="correo" placeholder="Correo institucional" autocomplete="username" required
                   maxlength="150" [(ngModel)]="correo" #campoCorreo="ngModel" />
          </label>
          @if (enviado() && campoCorreo.invalid) { <span class="sr-only" role="alert">Escribe tu correo institucional.</span> }

          @if (esEstudiante()) {
            <label class="campo c2" [class.invalido]="enviado() && campoCodigo.invalid">
              <span class="sr-only">Código de acceso</span>
              <svg class="icono" viewBox="0 0 24 24" aria-hidden="true"><circle cx="8" cy="12" r="4" /><path d="M12 12h9m-3 0v3m-3-3v2" /></svg>
              <input name="codigo" placeholder="Código de acceso" autocomplete="one-time-code" required maxlength="64"
                     [(ngModel)]="codigo" #campoCodigo="ngModel" />
            </label>
            @if (enviado() && campoCodigo.invalid) { <span class="sr-only" role="alert">Escribe el código que recibiste por correo.</span> }
          } @else {
            <label class="campo c2" [class.invalido]="enviado() && campoClave.invalid">
              <span class="sr-only">Contraseña</span>
              <svg class="icono" viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="10.5" width="14" height="9.5" rx="2.2" /><path d="M8 10.5V8a4 4 0 0 1 8 0v2.5" /></svg>
              <input [type]="verClave() ? 'text' : 'password'" name="clave" placeholder="Contraseña" autocomplete="current-password"
                     required maxlength="100" [(ngModel)]="clave" #campoClave="ngModel" />
              <button type="button" class="ojo" (click)="verClave.set(!verClave())"
                      [attr.aria-label]="verClave() ? 'Ocultar contraseña' : 'Mostrar contraseña'">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12Z" /><circle cx="12" cy="12" r="3" />
                  @if (!verClave()) { <path d="M4 4l16 16" /> }
                </svg>
              </button>
            </label>
            @if (enviado() && campoClave.invalid) { <span class="sr-only" role="alert">Escribe tu contraseña.</span> }
          }

          <label class="recordar">
            <input type="checkbox" name="recordar" [(ngModel)]="recordar" />
            <span class="caja" aria-hidden="true"></span>
            <span>Recordarme</span>
          </label>
          <button type="button" class="olvido" (click)="noDisponible('recuperar')">¿Olvidaste tu contraseña?</button>

          <button type="submit" class="ingresar" [disabled]="ocupado()">
            <span>{{ ocupado() ? 'Ingresando…' : 'Ingresar' }}</span>
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 12h15m-5-5 5 5-5 5" /></svg>
          </button>

          <div class="separador"><span>o continúa con</span></div>
          <div class="sociales">
            <button type="button" class="social" aria-label="Continuar con Google" (click)="noDisponible('social')">
              <span class="g" aria-hidden="true">G</span>
            </button>
            <button type="button" class="social" aria-label="Continuar con Microsoft" (click)="noDisponible('social')">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path fill="#4a3a6b" d="M3 3h8.500v8.500H3zM12.500 3H21v8.500h-8.500zM3 12.500h8.500V21H3zM12.500 12.500H21V21h-8.500z" /></svg>
            </button>
            <button type="button" class="social" aria-label="Continuar con Apple" (click)="noDisponible('social')">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path fill="#4a3a6b" d="M16.400 12.600c0-2.300 1.900-3.400 2-3.500-1.100-1.600-2.800-1.800-3.400-1.800-1.400-.1-2.800.9-3.500.9-.7 0-1.900-.9-3.100-.8-1.600 0-3.100.9-3.900 2.400-1.700 2.900-.4 7.200 1.200 9.600.8 1.200 1.800 2.500 3 2.400 1.200 0 1.700-.8 3.100-.8s1.900.8 3.100.8c1.300 0 2.100-1.200 2.900-2.400.9-1.300 1.300-2.600 1.300-2.700-.1 0-2.600-1-2.700-4.100ZM14.200 5.600c.6-.8 1.100-1.900 1-3-.9 0-2.100.6-2.700 1.400-.6.700-1.100 1.800-1 2.900 1 .1 2.100-.5 2.700-1.300Z"/></svg>
            </button>
          </div>

          <p class="registro">¿No tienes una cuenta? <button type="button" (click)="noDisponible('registro')">Regístrate</button></p>
        </div>
        <button type="button" class="cambio" (click)="alternarTipo()">
          {{ esEstudiante() ? 'Ingresar como docente o administrador' : 'Ingresar como estudiante, con código de acceso' }}
        </button>
      </form>
    </div>
  `,
  styles: `
    :host { display: block; }
    .escena { position: relative; min-height: 100vh; min-height: 100dvh; overflow: hidden; background: #cfc6e6; color: #4a3a6b; font-family: 'Figtree', var(--sans); }
    .fondo { position: fixed; inset: 0; width: 100%; height: 100%; object-fit: cover; object-position: 27% 40%; user-select: none; pointer-events: none; }

    .escena::before { content: ''; position: fixed; left: 0; right: 0; top: 43vh; height: 18vh; background: linear-gradient(180deg, rgba(207,198,230,0), rgba(207,198,230,.96) 55%); pointer-events: none; }
    /* Móvil / tableta vertical: el render queda de fondo y la tarjeta fluye debajo de la marca. */
    .tarjeta {
      position: relative; container-type: inline-size; aspect-ratio: 610 / 694;
      width: min(92vw, 460px); margin: 3vh auto 5vh; z-index: 1;
      border-radius: 6.6% / 5.8%; overflow: visible;
      background: linear-gradient(160deg, rgba(255,255,255,.62), rgba(236,229,250,.5) 55%, rgba(222,212,245,.45));
      -webkit-backdrop-filter: blur(22px) saturate(1.15); backdrop-filter: blur(22px) saturate(1.15);
      border: 1px solid rgba(255,255,255,.75);
      box-shadow: 0 30px 70px -20px rgba(74,58,107,.35), inset 0 1px 0 rgba(255,255,255,.9), inset 0 0 40px rgba(255,255,255,.25);
    }


    /* ---- Vidrio y movimiento ---- */
    .escena { background: linear-gradient(180deg, #e9e3f5 0%, #cfc6e6 100%); }
    .fondo { opacity: 0; transition: opacity .9s ease; filter: blur(.45px) saturate(1.04); }
    .fondo.listo { opacity: 1; }
    .marca > * { animation: entra .9s cubic-bezier(.2,.7,.2,1) both; }
    .marca .isotipo { animation: entra .9s cubic-bezier(.2,.7,.2,1) both, flota 7s ease-in-out 1s infinite; filter: drop-shadow(0 14px 18px rgba(88,68,140,.25)); }
    .marca .nombre { animation-delay: .12s; } .marca .sub { animation-delay: .22s; } .marca .linea { animation-delay: .3s; }
    .marca .lema { animation-delay: .38s; } .marca .parrafo { animation-delay: .46s; }
    .tarjeta { animation: tarjeta 1s cubic-bezier(.2,.7,.2,1) .1s both; }
    .tarjeta::after { content: ''; position: absolute; inset: 0; border-radius: inherit; pointer-events: none;
      background: linear-gradient(115deg, transparent 35%, rgba(255,255,255,.38) 48%, rgba(255,255,255,.1) 54%, transparent 65%);
      background-size: 250% 100%; background-position: 160% 0; animation: brillo 9s ease-in-out 1.5s infinite; mix-blend-mode: soft-light; }
    .campo { transition: transform .25s ease, box-shadow .25s ease, background .25s ease; }
    .campo:hover { transform: translateY(-1px); }
    .campo:focus-within { box-shadow: 0 0 0 3px rgba(139,116,196,.35), 0 10px 28px -10px rgba(88,68,140,.45); background: rgba(255,255,255,.78); }
    .ingresar { position: absolute; overflow: hidden; transition: transform .25s ease, box-shadow .25s ease; }
    .ingresar::after { content: ''; position: absolute; top: 0; bottom: 0; left: -40%; width: 30%; transform: skewX(-20deg);
      background: linear-gradient(90deg, transparent, rgba(255,255,255,.35), transparent); transition: left .7s ease; }
    .ingresar:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 16px 34px -10px rgba(74,58,107,.6); }
    .ingresar:hover:not(:disabled)::after { left: 120%; }
    .ingresar:active:not(:disabled) { transform: translateY(0) scale(.99); }
    .social { transition: transform .25s ease, box-shadow .25s ease, background .25s ease; }
    .social:hover { transform: translateY(-3px); box-shadow: 0 14px 26px -10px rgba(88,68,140,.5); background: rgba(255,255,255,.85); }
    @keyframes entra { from { opacity: 0; transform: translateY(14px); } to { opacity: 1; transform: none; } }
    @keyframes tarjeta { from { opacity: 0; transform: translateY(20px) scale(.985); } to { opacity: 1; transform: none; } }
    @keyframes flota { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-8px); } }
    @keyframes brillo { 0%,55% { background-position: 160% 0; } 85%,100% { background-position: -60% 0; } }
    @media (prefers-reduced-motion: reduce) { .marca > *, .tarjeta, .tarjeta::after, .marca .isotipo { animation: none !important; } .fondo { transition: none; } }

    /* Marca de la izquierda: texto y logo reales (el logo es vectorial), medidos en px de referencia. */
    .marca { position: relative; z-index: 1; --w: min(92vw, 460px); width: var(--w); margin: 6vh auto 0; text-align: center; color: #4a3a6b; }
    .marca .isotipo { width: 38%; height: auto; display: block; margin: 0 auto; }
    .marca .nombre { display: block; width: 80%; height: auto; margin: 0 auto; }
    .marca .sub { font-size: calc(var(--w) * .036); letter-spacing: .12em; line-height: 1.6; color: #5b4c7c; margin-top: calc(var(--w) * .03); }
    .marca .linea { width: 15%; height: 2px; background: #6b5a8e; margin: calc(var(--w) * .05) auto; }
    .marca .lema { font-family: 'Cormorant Garamond', Georgia, serif; font-weight: 500; font-size: calc(var(--w) * .085); line-height: 1.1; color: #43335f; }
    .marca .parrafo { margin: calc(var(--w) * .04) 0 0; font-size: calc(var(--w) * .042); line-height: 1.45; color: #5b4c7c; }
    @media (min-aspect-ratio: 3/2) and (min-width: 900px) {
      .marca { --W: max(100vw, 177.7778vh); --u: calc(var(--W) / 1672); position: absolute; margin: 0; width: calc(var(--u) * 360); text-align: left;
        left: calc(50vw - var(--W) / 2 + var(--u) * 385); top: calc(50vh - var(--W) * 941 / 3344 + var(--u) * 118); }
      .marca .isotipo { position: absolute; left: calc(var(--u) * 40); top: calc(var(--u) * -12); width: calc(var(--u) * 230); margin: 0; }
        .marca .nombre { position: absolute; left: calc(var(--u) * -5); top: calc(var(--u) * 194); width: calc(var(--u) * 372); margin: 0; }
      .marca .sub { position: absolute; left: 0; right: 0; top: calc(var(--u) * 298); margin: 0; font-size: calc(var(--u) * 15.6); line-height: 1.6; text-align: center; letter-spacing: .1em; white-space: nowrap; }
      .marca .linea { position: absolute; left: 0; top: calc(var(--u) * 354); width: calc(var(--u) * 60); margin: 0; }
      .marca .lema { position: absolute; left: 0; top: calc(var(--u) * 372); font-size: calc(var(--u) * 40); line-height: 1.06; font-weight: 600; text-align: left; white-space: nowrap; }
      .marca .parrafo { position: absolute; left: 0; top: calc(var(--u) * 467); margin: 0; font-size: calc(var(--u) * 20.5); line-height: 1.4; text-align: left; white-space: nowrap; }
    }

    /* Escritorio (≥ 3:2): el render ocupa todo el viewport y la tarjeta se posiciona sobre su panel de cristal. */
    @media (min-aspect-ratio: 3/2) and (min-width: 900px) {
      .escena { height: 100vh; min-height: 0; }
      .escena::before { display: none; }
      .fondo { position: absolute; inset: auto; left: 50%; top: 50%; width: max(100vw, 177.7778vh); height: auto; aspect-ratio: 1672 / 941; transform: translate(-50%, -50%); object-fit: fill; }
      .tarjeta {
        position: absolute; margin: 0; width: calc(max(100vw, 177.7778vh) * 610 / 1672);
        left: calc(50vw - max(100vw, 177.7778vh) / 2 + max(100vw, 177.7778vh) * 920 / 1672);
        top: calc(50vh - max(100vw, 177.7778vh) * 941 / 3344 + max(100vw, 177.7778vh) * 138 / 1672);
        background: linear-gradient(160deg, rgba(255,255,255,.10), rgba(255,255,255,.02));
        -webkit-backdrop-filter: none; backdrop-filter: none; border-color: transparent;
        box-shadow: none;
      }
    }

    /* Todo el interior se mide en "px de referencia" (1 u = 1 px de la imagen de 1672 de ancho). */
    .interior { --u: calc(100cqw / 610); position: absolute; inset: 0; }
    .interior > * { position: absolute; }
    .titulo { left: 0; right: 0; top: calc(var(--u) * 73); margin: 0; text-align: center; font-family: 'Cormorant Garamond', Georgia, serif; font-weight: 500; font-size: max(calc(var(--u) * 48), 26px); line-height: 1.1; color: #43335f; letter-spacing: 0; }
    .subtitulo { left: 0; right: 0; top: calc(var(--u) * 136); margin: 0; text-align: center; font-size: max(calc(var(--u) * 19.5), 14px); line-height: 1.37; color: #6a5d86; font-weight: 400; }
    .aviso { left: calc(var(--u) * 40); right: calc(var(--u) * 40); top: calc(var(--u) * 190); margin: 0; text-align: center; font-size: calc(var(--u) * 13.5); line-height: 1.25; color: #4a3a6b; }
    .aviso.error { color: #9b3b4e; }

    .campo {
      left: calc(var(--u) * 67); width: calc(var(--u) * 478); height: calc(var(--u) * 63); border-radius: calc(var(--u) * 32);
      display: flex; align-items: center; padding: 0 calc(var(--u) * 24) 0 calc(var(--u) * 26); gap: calc(var(--u) * 18);
      background: linear-gradient(180deg, rgba(255,255,255,.5), rgba(255,255,255,.26));
      border: 1px solid rgba(255,255,255,.8);
      box-shadow: 0 8px 22px -12px rgba(94,74,138,.28), inset 0 1px 0 rgba(255,255,255,.9);
      transition: border-color .18s, box-shadow .18s, background .18s;
    }
    .c1 { top: calc(var(--u) * 219); }
    .c2 { top: calc(var(--u) * 295); }
    .campo:focus-within { border-color: #8a77b8; background: linear-gradient(180deg, rgba(255,255,255,.88), rgba(255,255,255,.6)); box-shadow: 0 0 0 calc(var(--u) * 3) rgba(138,119,184,.28), 0 8px 22px -12px rgba(94,74,138,.3); }
    .campo.invalido { border-color: #c27a8a; }
    .campo input { flex: 1; min-width: 0; border: 0; outline: 0; background: transparent; font: inherit; font-size: max(calc(var(--u) * 17), 13.5px); color: #3f3257; }
    .campo input::placeholder { color: #8c829f; opacity: 1; }
    .icono, .ojo svg { width: calc(var(--u) * 28); height: calc(var(--u) * 28); flex: none; fill: none; stroke: #4a3a6b; stroke-width: 1.75; stroke-linecap: round; stroke-linejoin: round; }
    .ojo { border: 0; background: none; padding: 0; display: flex; border-radius: 8px; cursor: pointer; }
    .ojo svg { width: calc(var(--u) * 26); height: calc(var(--u) * 26); }

    .recordar { left: calc(var(--u) * 67); top: calc(var(--u) * 374); height: calc(var(--u) * 30); display: flex; align-items: center; gap: calc(var(--u) * 14); font-size: max(calc(var(--u) * 16.5), 13px); font-weight: 500; color: #43355f; cursor: pointer; }
    .recordar input { position: absolute; opacity: 0; width: 0; height: 0; }
    .caja { width: calc(var(--u) * 25); height: calc(var(--u) * 25); border-radius: calc(var(--u) * 6); background: rgba(255,255,255,.7); border: 1.5px solid #6f6093; transition: background .15s, box-shadow .15s; display: grid; place-items: center; }
    .recordar input:checked + .caja { background: #5b4a80; border-color: #5b4a80; }
    .recordar input:checked + .caja::after { content: ''; width: 28%; height: 52%; border: solid #fff; border-width: 0 2px 2px 0; transform: translateY(-8%) rotate(45deg); }
    .recordar input:focus-visible + .caja { box-shadow: 0 0 0 3px rgba(138,119,184,.45); }
    .olvido { right: calc(var(--u) * 65); top: calc(var(--u) * 374); height: calc(var(--u) * 30); border: 0; background: none; padding: 0; font: inherit; font-size: max(calc(var(--u) * 16.5), 13px); font-weight: 500; color: #43355f; cursor: pointer; }
    .olvido:hover, .registro button:hover, .cambio:hover { text-decoration: underline; }

    .ingresar {
      left: calc(var(--u) * 67); top: calc(var(--u) * 420); width: calc(var(--u) * 478); height: calc(var(--u) * 65); border-radius: calc(var(--u) * 33);
      display: flex; align-items: center; justify-content: center; border: 1px solid rgba(255,255,255,.12);
      background: linear-gradient(100deg, #5e4d82 0%, #5a4879 60%, #55437a 100%);
      box-shadow: 0 14px 26px -12px rgba(63,48,95,.65), inset 0 1px 0 rgba(255,255,255,.28), inset 0 -2px 6px rgba(40,28,66,.35);
      color: #fff; font: inherit; font-size: max(calc(var(--u) * 20), 15px); font-weight: 500; letter-spacing: .01em; cursor: pointer;
      transition: filter .2s, transform .2s, box-shadow .2s;
    }
    .ingresar svg { position: absolute; right: calc(var(--u) * 33); width: calc(var(--u) * 24); height: calc(var(--u) * 24); fill: none; stroke: #fff; stroke-width: 1.4; stroke-linecap: round; stroke-linejoin: round; transition: transform .2s; }
    .ingresar:hover:not(:disabled) { filter: brightness(1.08); transform: translateY(-1px); box-shadow: 0 18px 30px -12px rgba(63,48,95,.7), inset 0 1px 0 rgba(255,255,255,.3), inset 0 -2px 6px rgba(40,28,66,.35); }
    .ingresar:hover:not(:disabled) svg { transform: translateX(3px); }
    .ingresar:focus-visible { outline: 3px solid rgba(138,119,184,.6); outline-offset: 2px; }
    .ingresar:disabled { opacity: .75; cursor: progress; }

    .separador { left: calc(var(--u) * 67); width: calc(var(--u) * 478); top: calc(var(--u) * 506); height: calc(var(--u) * 24); display: flex; align-items: center; gap: calc(var(--u) * 22); font-size: max(calc(var(--u) * 15), 12px); color: #7b7092; }
    .separador::before, .separador::after { content: ''; flex: 1; height: 1px; background: rgba(110,96,145,.35); }

    .sociales { left: 0; right: 0; top: calc(var(--u) * 546); height: calc(var(--u) * 70); display: flex; justify-content: center; gap: calc(var(--u) * 26); }
    .social {
      width: calc(var(--u) * 72); height: calc(var(--u) * 70); border-radius: calc(var(--u) * 20); display: grid; place-items: center; cursor: pointer;
      background: linear-gradient(180deg, rgba(255,255,255,.42), rgba(255,255,255,.16)); border: 1px solid rgba(255,255,255,.7);
      box-shadow: 0 10px 22px -14px rgba(94,74,138,.35), inset 0 1px 0 rgba(255,255,255,.85); transition: transform .2s, background .2s;
    }
    .social svg { width: calc(var(--u) * 34); height: calc(var(--u) * 34); }
    .g { font-family: 'Figtree', sans-serif; font-weight: 700; font-size: calc(var(--u) * 34); line-height: 1; color: #4a3a6b; }
    .social:hover { transform: translateY(-2px); background: linear-gradient(180deg, rgba(255,255,255,.85), rgba(255,255,255,.5)); }
    .social:focus-visible { outline: 3px solid rgba(138,119,184,.6); outline-offset: 2px; }

    .registro { left: 0; right: 0; top: calc(var(--u) * 634); margin: 0; text-align: center; font-size: max(calc(var(--u) * 16.5), 13px); color: #75698d; }
    .registro button { border: 0; background: none; padding: 0; margin-left: calc(var(--u) * 6); font: inherit; font-weight: 600; color: #43335f; cursor: pointer; }

    .cambio { position: absolute; left: 0; right: 0; top: calc(100% + 14px); border: 0; background: none; font: inherit; font-size: 13.5px; color: #4a3d66; cursor: pointer; text-shadow: 0 1px 8px rgba(255,255,255,.7); }
  `,
})
export class Login extends ConOperaciones {
  protected readonly listo = signal(false);
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
