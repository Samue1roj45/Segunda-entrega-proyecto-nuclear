import { Component, ElementRef, OnDestroy, OnInit, afterEveryRender, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { Campana } from '../../compartido/campana';
import { Logo } from '../../compartido/logo';
import { Paleta } from '../../compartido/paleta';
import { Permiso } from '../../core/modelos/permiso';
import { Rol } from '../../core/modelos/rol';
import { SesionService } from '../../core/sesion.service';
import { COLORES_AVATAR, PreferenciasService } from '../../core/preferencias.service';
import { TemaService } from '../../core/tema.service';

interface Seccion {
  readonly titulo: string;
  readonly icono: string;
  readonly ruta: string;
  readonly permiso: Permiso | null;
  readonly grupo: string;
}

/** Secciones del menú lateral. Solo aparecen las que el rol tiene permitidas (RF-02). */
const TECLAS: Record<string, string> = { '/inicio': 'i', '/casos': 'c', '/sesiones': 's', '/participaciones': 'p', '/resultados': 'r', '/docentes': 'd', '/mis-sesiones': 'm', '/mis-resultados': 'n' };

const SECCIONES: readonly Seccion[] = [
  { titulo: 'Inicio', icono: 'M4 11 12 4l8 7v8a1 1 0 0 1-1 1h-5v-6h-4v6H5a1 1 0 0 1-1-1z', ruta: '/inicio', permiso: null, grupo: 'General' },
  { titulo: 'Casos de estudio', icono: 'M5 4h10l4 4v12H5zM14 4v5h5M8 13h8M8 17h5', ruta: '/casos', permiso: Permiso.GestionarCasos, grupo: 'Docencia' },
  { titulo: 'Mis sesiones', icono: 'M5 5h14v15H5zM5 9h14M9 3v4m6-4v4', ruta: '/sesiones', permiso: Permiso.ProgramarSesiones, grupo: 'Docencia' },
  { titulo: 'Participaciones', icono: 'M4 19c1.5-4 4.2-6 8-6s6.5 2 8 6M12 10a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z', ruta: '/participaciones', permiso: Permiso.ConsultarParticipaciones, grupo: 'Evaluación' },
  { titulo: 'Resultados y reportes', icono: 'M5 20V10m7 10V4m7 16v-7', ruta: '/resultados', permiso: Permiso.ConsultarParticipaciones, grupo: 'Evaluación' },
  { titulo: 'Docentes', icono: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4 20a8 8 0 0 1 16 0', ruta: '/docentes', permiso: Permiso.AdministrarDocentes, grupo: 'Administración' },
  { titulo: 'Componentes', icono: 'M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z', ruta: '/componentes', permiso: Permiso.AdministrarDocentes, grupo: 'Administración' },
  { titulo: 'Mis casos', icono: 'M5 4h10l4 4v12H5zM14 4v5h5M9 14l2 2 4-4', ruta: '/mis-sesiones', permiso: Permiso.ParticiparEnCasos, grupo: 'Mi aprendizaje' },
  { titulo: 'Mis resultados', icono: 'M5 20V10m7 10V4m7 16v-7', ruta: '/mis-resultados', permiso: Permiso.ConsultarResultadosPropios, grupo: 'Mi aprendizaje' },
];

/**
 * Página interior de SOMA. Composición propia ("aurora"): luz lila en movimiento, un dock lateral de
 * vidrio agrupado por tareas y una cabecera que sitúa al usuario. Comparte el lenguaje del login
 * (lila, vidrio, serif) sin repetir su escena.
 */
@Component({
  selector: 'app-panel',
  imports: [Campana, Logo, Paleta, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    @if (cargandoRuta()) { <div class="progreso" aria-hidden="true"><i></i></div> }
    @if (bienvenida()) {
      <div class="bienvenida" (click)="bienvenida.set(false)" role="presentation">
        <img src="logo-soma.webp" alt="" /><p class="hola">{{ saludoTexto() }}</p><p class="sub">Simulaciones para el aprendizaje en psicología social</p>
      </div>
    }
    <div class="aurora" aria-hidden="true"><i></i><i></i><i></i><img src="logo-soma.webp" alt="" /></div>

    <div class="marco" [class.enfoque]="enfoque()">
      <aside class="lateral">
        <a routerLink="/inicio" class="marca" aria-label="SOMA, inicio">
          <soma-logo [tamano]="52" />
          <span><strong>SOMA</strong><small>Psicología social</small></span>
        </a>

        <nav aria-label="Secciones" #navegacion>
          <span class="pildora" aria-hidden="true" [style.transform]="'translateY(' + pildora().y + 'px)'" [style.height.px]="pildora().h" [style.opacity]="pildora().h ? 1 : 0"></span>
          @for (g of grupos(); track g.nombre) {
            <p class="grupo">{{ g.nombre }}</p>
            @for (s of g.items; track s.titulo) {
              <a [routerLink]="s.ruta" routerLinkActive="activo" [routerLinkActiveOptions]="{ exact: s.ruta === '/inicio' }" class="item">
                <span class="ico"><svg viewBox="0 0 24 24" aria-hidden="true"><path [attr.d]="s.icono" /></svg></span>
                <span class="txt">{{ s.titulo }}</span>
              </a>
            }
          }
        </nav>

        <div class="perfil">
          <a routerLink="/perfil" class="enlace-perfil" aria-label="Mi perfil" title="Mi perfil">
            <span class="avatar" [style.background]="degradado()" aria-hidden="true">{{ iniciales() }}</span>
            <span class="datos">
              <strong>{{ sesion()?.nombreCompleto }}</strong>
              <small>{{ rolLegible() }}</small>
            </span>
          </a>
          <button type="button" class="salir" (click)="salir()" aria-label="Cerrar sesión" title="Cerrar sesión">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 4h4v16h-4M10 8l-4 4 4 4M6 12h10" /></svg>
          </button>
        </div>
      </aside>

      <div class="contenido">
        @if (enfoque()) {
          <div class="barra-enfoque"><span>Modo enfoque · solo tú y el caso</span><a routerLink="/mis-sesiones" class="boton-suave boton-chico enlace-boton">Salir</a></div>
        }
        <header class="barra">
          <div class="encabezado">
            <p class="sobre">{{ actual().grupo }}</p>
            <h1>{{ titulo() }}</h1>
          </div>
          <div class="chips">
            @defer (on immediate) { <soma-paleta [destinos]="destinos()" /> }
            <button type="button" class="tema" (click)="cambiarTema($event)" [attr.aria-label]="temas.tema() === 'noche' ? 'Cambiar a modo día' : 'Cambiar a modo noche'" [title]="temas.tema() === 'noche' ? 'Modo día' : 'Modo noche'">
              @if (temas.tema() === 'noche') {
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 4V2m0 20v-2M4 12H2m20 0h-2M6 6 4.6 4.6m14.8 14.8L18 18M6 18l-1.4 1.4M19.4 4.6 18 6m-6 10a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" /></svg>
              } @else {
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 14.5A8 8 0 0 1 9.5 4 8 8 0 1 0 20 14.5Z" /></svg>
              }
            </button>
            @if (puedeNotificaciones() && prefs.campana()) { @defer (on immediate) { <soma-campana /> } }
            <span class="chip fecha"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 5h14v15H5zM5 9h14M9 3v4m6-4v4" /></svg>{{ hoy }}</span>
            <span class="chip rol"><i></i>{{ rolLegible() }}</span>
          </div>
        </header>

        <main id="contenido" class="vista" tabindex="-1"><router-outlet /></main>
      </div>
    </div>
  `,
  styles: `
    :host { display: block; min-height: 100vh; position: relative; background: #e9e4f4; }

    /* ---- Aurora: luz lila a la deriva + isotipo como marca de agua ---- */
    .aurora { position: fixed; inset: 0; z-index: 0; overflow: hidden; pointer-events: none;
      background: radial-gradient(120% 90% at 12% 0%, #f6f2fd 0%, #e6dff5 45%, #d9d0ee 100%); }
    .aurora i { position: absolute; border-radius: 50%; filter: blur(70px); opacity: .75; animation: deriva 26s ease-in-out infinite alternate; }
    .aurora i:nth-child(1) { width: 46vmax; height: 46vmax; left: -12vmax; top: -14vmax; background: radial-gradient(circle, #c9b8f0, transparent 65%); }
    .aurora i:nth-child(2) { width: 40vmax; height: 40vmax; right: -10vmax; top: 18vh; background: radial-gradient(circle, #aebcf0, transparent 65%); animation-duration: 32s; animation-delay: -8s; }
    .aurora i:nth-child(3) { width: 36vmax; height: 36vmax; left: 30vw; bottom: -18vmax; background: radial-gradient(circle, #e8b8dd, transparent 65%); opacity: .55; animation-duration: 38s; animation-delay: -15s; }
    .aurora img { position: absolute; right: -6vmin; bottom: -8vmin; width: min(62vmin, 560px); opacity: .09; filter: saturate(.8); }
    @keyframes deriva { from { transform: translate3d(0,0,0) scale(1); } to { transform: translate3d(6vmax, 4vmax, 0) scale(1.18); } }

    .progreso { position: fixed; top: 0; left: 0; right: 0; height: 3px; z-index: 300; overflow: hidden; }
    .progreso i { display: block; height: 100%; width: 40%; border-radius: 3px; background: linear-gradient(90deg, transparent, #8b6fe0, #c3a9ff, transparent); animation: corre 1s ease-in-out infinite; }
    @keyframes corre { from { transform: translateX(-100%); } to { transform: translateX(260%); } }
    .bienvenida { position: fixed; inset: 0; z-index: 250; display: grid; place-content: center; justify-items: center; gap: .6rem; text-align: center; cursor: pointer;
      background: radial-gradient(100% 80% at 50% 30%, #f3effb, #d9d0ee); animation: bv 2.1s ease both; }
    .bienvenida img { width: 150px; animation: bv-img 1.6s cubic-bezier(.2,.8,.2,1) both; filter: drop-shadow(0 18px 24px rgba(88,68,140,.35)); }
    .bienvenida .hola { font-family: var(--serif); font-size: clamp(2rem, 5vw, 3.2rem); color: var(--lila-950); animation: bv-tx 1.2s .3s ease both; }
    .bienvenida .sub { color: var(--tinta-suave); letter-spacing: .12em; text-transform: uppercase; font-size: .72rem; animation: bv-tx 1.2s .5s ease both; }
    @keyframes bv { 0%, 70% { opacity: 1; } 100% { opacity: 0; visibility: hidden; } }
    @keyframes bv-img { from { opacity: 0; transform: scale(.7) translateY(20px); filter: blur(10px); } }
    @keyframes bv-tx { from { opacity: 0; transform: translateY(14px); } }
    :host-context([data-tema='noche']) .bienvenida { background: radial-gradient(100% 80% at 50% 30%, #2b2150, #110d27); }
    :host-context([data-tema='noche']) .bienvenida .hola { color: #f1ecff; }
    .enlace-perfil { display: flex; align-items: center; gap: .65rem; flex: 1; min-width: 0; text-decoration: none; border-radius: 14px; transition: background .2s; }
    .enlace-perfil:hover { background: rgba(255,255,255,.5); }
    .barra-enfoque { display: flex; justify-content: space-between; align-items: center; gap: 1rem; padding: .5rem .9rem; border-radius: 999px; font-size: .85rem; color: var(--lila-800);
      background: rgba(255,255,255,.55); border: 1px solid rgba(255,255,255,.8); }
    .marco.enfoque { grid-template-columns: 1fr; max-width: 980px; }
    .marco.enfoque .lateral, .marco.enfoque .chips { display: none; }
    .marco { position: relative; z-index: 1; display: grid; grid-template-columns: 272px minmax(0, 1fr); gap: 1.4rem; padding: 1.2rem; min-height: 100vh; max-width: 1640px; margin: 0 auto; }

    /* ---- Dock lateral ---- */
    .lateral {
      position: sticky; top: 1.2rem; height: calc(100vh - 2.4rem); display: flex; flex-direction: column; gap: 1.2rem;
      padding: 1.3rem 1rem 1rem; border-radius: 30px; overflow: hidden;
      background: linear-gradient(170deg, rgba(255,255,255,.72), rgba(255,255,255,.34) 60%, rgba(226,216,246,.45));
      border: 1px solid rgba(255,255,255,.75);
      -webkit-backdrop-filter: blur(28px) saturate(1.7); backdrop-filter: blur(28px) saturate(1.7);
      box-shadow: 0 34px 60px -34px rgba(59,45,92,.5), inset 0 1.5px 0 rgba(255,255,255,.95), inset 0 -1px 0 rgba(150,130,200,.18);
    }
    .marca { display: flex; align-items: center; gap: .75rem; text-decoration: none; padding: .1rem .5rem .6rem; border-bottom: 1px solid rgba(94,74,138,.12); }
    .marca strong { display: block; font-family: var(--serif); font-size: 1.85rem; font-weight: 600; line-height: 1; color: var(--lila-950); letter-spacing: .08em; }
    .marca small { color: var(--tinta-suave); font-size: .68rem; letter-spacing: .14em; text-transform: uppercase; }
    .pildora { position: absolute; left: 0; right: 2px; top: 0; border-radius: 16px; z-index: 0; pointer-events: none;
      background: linear-gradient(100deg, rgba(255,255,255,.85), rgba(255,255,255,.4)); box-shadow: 0 10px 24px -14px rgba(59,45,92,.55), inset 0 1px 0 #fff;
      transition: transform .45s cubic-bezier(.3,1.3,.4,1), height .3s, opacity .2s; }
    .pildora::before { content: ''; position: absolute; left: -1rem; top: 22%; bottom: 22%; width: 4px; border-radius: 0 4px 4px 0; background: var(--lila-700); box-shadow: 0 0 12px var(--lila-500); }
    :host-context([data-tema='noche']) .pildora { background: linear-gradient(100deg, rgba(255,255,255,.16), rgba(255,255,255,.05)); box-shadow: 0 12px 26px -14px rgba(0,0,0,.7), inset 0 1px 0 rgba(255,255,255,.2); }
    :host-context([data-tema='noche']) .pildora::before { background: #b49cf5; box-shadow: 0 0 14px #8a6fe0; }
    nav { position: relative; display: grid; gap: .2rem; align-content: start; overflow-y: auto; padding-right: 2px; flex: 1; scrollbar-width: none; }
    .grupo { margin: .9rem .6rem .3rem; font-size: .66rem; letter-spacing: .18em; text-transform: uppercase; color: var(--lila-500); font-weight: 500; }
    .grupo:first-child { margin-top: .2rem; }
    .item { position: relative; display: flex; align-items: center; gap: .75rem; padding: .5rem .6rem; border-radius: 16px; color: var(--tinta); text-decoration: none; transition: background .2s, transform .2s, box-shadow .2s; }
    .ico { width: 2.15rem; height: 2.15rem; border-radius: 12px; display: grid; place-items: center; background: rgba(255,255,255,.55); box-shadow: inset 0 1px 0 #fff; transition: background .2s, color .2s; color: var(--lila-700); }
    .ico svg { width: 19px; height: 19px; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }
    .item:hover { background: rgba(255,255,255,.5); transform: translateX(2px); }
    .item { z-index: 1; } .item.activo { color: var(--lila-950); font-weight: 500; }
    .item.activo .ico { background: linear-gradient(145deg, var(--lila-500), var(--lila-900)); color: #fff; box-shadow: 0 8px 16px -8px rgba(59,45,92,.7), inset 0 1px 0 rgba(255,255,255,.35); }

    .perfil { display: flex; align-items: center; gap: .65rem; padding: .6rem; border-radius: 20px; background: rgba(255,255,255,.55); border: 1px solid rgba(255,255,255,.8); box-shadow: inset 0 1px 0 #fff; }
    .avatar { flex: none; width: 2.5rem; height: 2.5rem; border-radius: 50%; display: grid; place-items: center; color: #fff; font-weight: 600; font-size: .9rem;
      box-shadow: 0 0 0 3px rgba(255,255,255,.75), 0 8px 14px -8px rgba(59,45,92,.7); }
    .datos { display: grid; line-height: 1.2; min-width: 0; flex: 1; }
    .datos strong { font-weight: 500; font-size: .88rem; color: var(--lila-950); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .datos small { color: var(--tinta-suave); font-size: .74rem; }
    .salir { flex: none; border: 0; background: rgba(255,255,255,.7); border-radius: 12px; width: 2.2rem; height: 2.2rem; display: grid; place-items: center; transition: background .2s, transform .2s; }
    .salir:hover { background: #fff; transform: scale(1.06); }
    .salir svg { width: 18px; height: 18px; fill: none; stroke: var(--lila-900); stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }

    /* ---- Contenido ---- */
    .contenido { display: grid; align-content: start; gap: 1.2rem; min-width: 0; padding-top: .3rem; }
    .barra { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; flex-wrap: wrap; padding: .2rem .3rem 0; }
    .sobre { font-size: .7rem; letter-spacing: .2em; text-transform: uppercase; color: var(--lila-700); margin-bottom: .2rem; }
    h1 { font-size: clamp(2rem, 3.2vw, 3rem); line-height: 1; }
    .chips { display: flex; gap: .5rem; flex-wrap: wrap; align-items: center; }
    .chip { display: inline-flex; align-items: center; gap: .45rem; padding: .5rem .9rem; border-radius: 999px; font-size: .85rem; color: var(--lila-900);
      background: rgba(255,255,255,.55); border: 1px solid rgba(255,255,255,.8); box-shadow: inset 0 1px 0 #fff, 0 8px 18px -14px rgba(59,45,92,.5);
      -webkit-backdrop-filter: blur(14px); backdrop-filter: blur(14px); }
    .chip.fecha::first-letter { text-transform: uppercase; }
    .tema { width: 2.5rem; height: 2.5rem; border-radius: 50%; display: grid; place-items: center; border: 1px solid rgba(255,255,255,.8); background: rgba(255,255,255,.55); box-shadow: inset 0 1px 0 #fff, 0 8px 18px -14px rgba(59,45,92,.5); transition: background .2s, transform .3s; }
    .tema:hover { background: #fff; transform: rotate(18deg) scale(1.06); }
    .tema svg { width: 19px; height: 19px; fill: none; stroke: var(--lila-800); stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
    .chip svg { width: 16px; height: 16px; fill: none; stroke: var(--lila-700); stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
    .chip.rol i { width: 8px; height: 8px; border-radius: 50%; background: #5cb88f; box-shadow: 0 0 0 3px rgba(92,184,143,.25); }
    .vista { display: grid; gap: 1.2rem; min-width: 0; animation: aparece .45s cubic-bezier(.2,.7,.2,1) both; }
    @keyframes aparece { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: none; } }


    /* ---- Modo noche ---- */
    :host-context([data-tema='noche']) { background: #14102a; }
    :host-context([data-tema='noche']) .aurora { background: radial-gradient(120% 90% at 12% 0%, #2b2150 0%, #1a1438 50%, #110d27 100%); }
    :host-context([data-tema='noche']) .aurora i { opacity: .5; mix-blend-mode: screen; }
    :host-context([data-tema='noche']) .aurora i:nth-child(1) { background: radial-gradient(circle, #6a4fc2, transparent 65%); }
    :host-context([data-tema='noche']) .aurora i:nth-child(2) { background: radial-gradient(circle, #3b55b8, transparent 65%); }
    :host-context([data-tema='noche']) .aurora i:nth-child(3) { background: radial-gradient(circle, #9b3f8f, transparent 65%); opacity: .35; }
    :host-context([data-tema='noche']) .aurora img { opacity: .1; filter: brightness(1.6) saturate(.9); }
    :host-context([data-tema='noche']) .lateral { background: linear-gradient(170deg, rgba(70,56,120,.55), rgba(28,22,58,.55) 60%, rgba(50,38,96,.5)); border-color: rgba(190,170,240,.22); box-shadow: 0 34px 60px -30px rgba(0,0,0,.7), inset 0 1.5px 0 rgba(255,255,255,.14), inset 0 -1px 0 rgba(0,0,0,.3); }
    :host-context([data-tema='noche']) .marca { border-color: rgba(190,170,240,.16); }
    :host-context([data-tema='noche']) .marca strong, :host-context([data-tema='noche']) .datos strong { color: #f1ecff; }
    :host-context([data-tema='noche']) .marca small, :host-context([data-tema='noche']) .datos small { color: #a89dc9; }
    :host-context([data-tema='noche']) .item { color: #d8d0ef; }
    :host-context([data-tema='noche']) .ico { background: rgba(255,255,255,.08); box-shadow: inset 0 1px 0 rgba(255,255,255,.12); color: #c6b9ee; }
    :host-context([data-tema='noche']) .item:hover { background: rgba(255,255,255,.07); }
    :host-context([data-tema='noche']) .item.activo { color: #fff; }
    :host-context([data-tema='noche']) .item.activo .ico { background: linear-gradient(145deg, #9b83e6, #5a3fb0); color: #fff; }
    :host-context([data-tema='noche']) .grupo { color: #8c7fbd; }
    :host-context([data-tema='noche']) .perfil { background: rgba(255,255,255,.07); border-color: rgba(190,170,240,.2); box-shadow: inset 0 1px 0 rgba(255,255,255,.1); }
    :host-context([data-tema='noche']) .salir, :host-context([data-tema='noche']) .tema, :host-context([data-tema='noche']) .chip { background: rgba(255,255,255,.09); border-color: rgba(190,170,240,.22); box-shadow: inset 0 1px 0 rgba(255,255,255,.12); color: #e2daf7; }
    :host-context([data-tema='noche']) .salir:hover, :host-context([data-tema='noche']) .tema:hover { background: rgba(255,255,255,.18); }
    :host-context([data-tema='noche']) .salir svg, :host-context([data-tema='noche']) .tema svg, :host-context([data-tema='noche']) .chip svg { stroke: #d6cbf5; }
    :host-context([data-tema='noche']) .sobre { color: #a996e6; }
    :host-context([data-tema='noche']) .avatar { box-shadow: 0 0 0 3px rgba(255,255,255,.16), 0 8px 14px -8px rgba(0,0,0,.7); }

    @media (max-width: 1000px) {
      .marco { grid-template-columns: 1fr; padding: .8rem .8rem 6.2rem; }
      .contenido { order: 1; }
      /* En móvil el dock pasa abajo, como barra flotante de iconos */
      .lateral { order: 2; position: fixed; left: .7rem; right: .7rem; bottom: .7rem; top: auto; height: auto; z-index: 20; flex-direction: row; align-items: center; padding: .45rem; border-radius: 26px; gap: .3rem; }
      .marca, .grupo, .perfil .datos, .txt { display: none; }
      nav { display: flex; flex: 1; gap: .15rem; overflow-x: auto; }
      .item { padding: .3rem; flex: none; }
      .pildora { display: none; }
      .item.activo { background: linear-gradient(100deg, rgba(255,255,255,.85), rgba(255,255,255,.4)); box-shadow: 0 10px 24px -14px rgba(59,45,92,.55), inset 0 1px 0 #fff; }
      :host-context([data-tema='noche']) .item.activo { background: rgba(255,255,255,.14); }
      .perfil { padding: .2rem; background: none; border: 0; box-shadow: none; }
      .avatar { display: none; } .enlace-perfil { flex: none; }
      .marco.enfoque { padding-bottom: 1rem; }
    }
    @media (prefers-reduced-motion: reduce) { .aurora i, .vista { animation: none; } }
    @media (prefers-reduced-transparency: reduce) { .lateral, .chip { -webkit-backdrop-filter: none; backdrop-filter: none; background: rgba(246,243,252,.96); } }
  `,
})
export class Panel implements OnInit, OnDestroy {
  protected readonly temas = inject(TemaService);
  protected readonly prefs = inject(PreferenciasService);
  protected readonly bienvenida = signal(this.debeSaludar());
  protected readonly cargandoRuta = signal(false);
  private readonly sesiones = inject(SesionService);
  private readonly router = inject(Router);

  protected readonly sesion = this.sesiones.sesion;
  protected readonly hoy = new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date());

  protected readonly secciones = computed(() =>
    SECCIONES.filter((s) => s.permiso === null || this.sesiones.tienePermiso(s.permiso)),
  );
  protected readonly grupos = computed(() => {
    const m = new Map<string, Seccion[]>();
    for (const s of this.secciones()) { m.set(s.grupo, [...(m.get(s.grupo) ?? []), s]); }
    return [...m].map(([nombre, items]) => ({ nombre, items }));
  });

  private readonly url = toSignal(
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd), map(() => this.router.url), startWith(this.router.url)),
    { initialValue: '' },
  );
  protected readonly actual = computed(() => {
    const u = this.url();
    return SECCIONES.find((s) => s.ruta !== '/inicio' && u.startsWith(s.ruta))
      ?? (u.startsWith('/participar') ? { ...SECCIONES[6], titulo: 'Participar en el caso' }
        : u.startsWith('/perfil') ? { ...SECCIONES[0], titulo: 'Mi perfil', grupo: 'Cuenta', ruta: '/perfil' } : SECCIONES[0]);
  });
  protected readonly destinos = computed(() => [
    ...this.secciones().map((s) => ({ titulo: s.titulo, ruta: s.ruta, grupo: s.grupo, icono: s.icono, tecla: TECLAS[s.ruta] })),
    { titulo: 'Mi perfil', ruta: '/perfil', grupo: 'Cuenta', icono: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4 20a8 8 0 0 1 16 0', tecla: 'u' },
  ]);
  protected readonly enfoque = computed(() => this.url().startsWith('/participar'));
  protected readonly degradado = computed(() => { const c = COLORES_AVATAR[this.prefs.avatar()]; return `linear-gradient(135deg, ${c[0]}, ${c[1]})`; });
  protected readonly saludoTexto = computed(() => `${this.saludo()}, ${this.nombreCorto()}`);
  protected readonly puedeNotificaciones = computed(() => this.sesiones.tienePermiso(Permiso.ProgramarSesiones));
  private readonly saludo = () => { const h = new Date().getHours(); return h < 12 ? 'Buenos días' : h < 19 ? 'Buenas tardes' : 'Buenas noches'; };
  protected readonly nombreCorto = computed(() => this.sesion()?.nombreCompleto.split(' ')[0] ?? '');
  protected readonly titulo = computed(() =>
    this.actual() === SECCIONES[0] ? `${this.saludo()}, ${this.nombreCorto()}` : this.actual().titulo,
  );
  protected readonly iniciales = computed(() =>
    (this.sesion()?.nombreCompleto ?? '')
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0].toUpperCase())
      .join(''),
  );
  protected readonly rolLegible = computed(() => {
    switch (this.sesion()?.rol) {
      case Rol.Administrador: return 'Administrador';
      case Rol.Docente: return 'Docente';
      case Rol.Estudiante: return 'Estudiante';
      default: return '';
    }
  });

  private readonly navegacion = viewChild<ElementRef<HTMLElement>>('navegacion');
  protected readonly pildora = signal({ y: 0, h: 0 });

  /** Coloca la píldora detrás del elemento activo del menú (se desliza con una animación elástica). */
  private posicionarPildora(): void {
    const nav = this.navegacion()?.nativeElement, activo = nav?.querySelector<HTMLElement>('.item.activo');
    const n = activo ? { y: activo.offsetTop, h: activo.offsetHeight } : { y: 0, h: 0 };
    const a = this.pildora();
    if (n.y !== a.y || n.h !== a.h) this.pildora.set(n);
  }

  constructor() {
    afterEveryRender(() => this.posicionarPildora());
    this.router.events.subscribe((e) => {
      if (e instanceof NavigationStart) this.cargandoRuta.set(true);
      else if (e instanceof NavigationEnd || e instanceof NavigationCancel || e instanceof NavigationError) this.cargandoRuta.set(false);
      if (e instanceof NavigationEnd) setTimeout(() => this.posicionarPildora(), 30);
    });
  }

  private debeSaludar(): boolean {
    try {
      if (!this.prefs.bienvenida() || sessionStorage.getItem('soma.saludado')) return false;
      sessionStorage.setItem('soma.saludado', '1');
      return !window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches;
    } catch { return false; }
  }

  ngOnInit(): void { this.temas.aplicar(); if (this.bienvenida()) setTimeout(() => this.bienvenida.set(false), 2100); }
  ngOnDestroy(): void { this.temas.quitar(); }
  protected cambiarTema(e: MouseEvent): void {
    const r = (e.currentTarget as HTMLElement).getBoundingClientRect();
    this.temas.alternar({ x: r.left + r.width / 2, y: r.top + r.height / 2 });
  }

  protected salir(): void {
    this.sesiones.cerrar();
    void this.router.navigate(['/login']);
  }
}
