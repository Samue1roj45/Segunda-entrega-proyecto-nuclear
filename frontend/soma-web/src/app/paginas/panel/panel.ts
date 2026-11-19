import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { Logo } from '../../compartido/logo';
import { Permiso } from '../../core/modelos/permiso';
import { Rol } from '../../core/modelos/rol';
import { SesionService } from '../../core/sesion.service';

interface Seccion {
  readonly titulo: string;
  readonly icono: string;
  readonly ruta: string;
  readonly permiso: Permiso | null;
  readonly grupo: string;
}

/** Secciones del menú lateral. Solo aparecen las que el rol tiene permitidas (RF-02). */
const SECCIONES: readonly Seccion[] = [
  { titulo: 'Inicio', icono: 'M4 11 12 4l8 7v8a1 1 0 0 1-1 1h-5v-6h-4v6H5a1 1 0 0 1-1-1z', ruta: '/inicio', permiso: null, grupo: 'General' },
  { titulo: 'Casos de estudio', icono: 'M5 4h10l4 4v12H5zM14 4v5h5M8 13h8M8 17h5', ruta: '/casos', permiso: Permiso.GestionarCasos, grupo: 'Docencia' },
  { titulo: 'Mis sesiones', icono: 'M5 5h14v15H5zM5 9h14M9 3v4m6-4v4', ruta: '/sesiones', permiso: Permiso.ProgramarSesiones, grupo: 'Docencia' },
  { titulo: 'Participaciones', icono: 'M4 19c1.5-4 4.2-6 8-6s6.5 2 8 6M12 10a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z', ruta: '/participaciones', permiso: Permiso.ConsultarParticipaciones, grupo: 'Evaluación' },
  { titulo: 'Resultados y reportes', icono: 'M5 20V10m7 10V4m7 16v-7', ruta: '/resultados', permiso: Permiso.ConsultarParticipaciones, grupo: 'Evaluación' },
  { titulo: 'Docentes', icono: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4 20a8 8 0 0 1 16 0', ruta: '/docentes', permiso: Permiso.AdministrarDocentes, grupo: 'Administración' },
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
  imports: [Logo, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="aurora" aria-hidden="true"><i></i><i></i><i></i><img src="logo-soma.webp" alt="" /></div>

    <div class="marco">
      <aside class="lateral">
        <a routerLink="/inicio" class="marca" aria-label="SOMA, inicio">
          <soma-logo [tamano]="52" />
          <span><strong>SOMA</strong><small>Psicología social</small></span>
        </a>

        <nav aria-label="Secciones">
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
          <span class="avatar" aria-hidden="true">{{ iniciales() }}</span>
          <span class="datos">
            <strong>{{ sesion()?.nombreCompleto }}</strong>
            <small>{{ rolLegible() }}</small>
          </span>
          <button type="button" class="salir" (click)="salir()" aria-label="Cerrar sesión" title="Cerrar sesión">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 4h4v16h-4M10 8l-4 4 4 4M6 12h10" /></svg>
          </button>
        </div>
      </aside>

      <div class="contenido">
        <header class="barra">
          <div class="encabezado">
            <p class="sobre">{{ actual().grupo }}</p>
            <h1>{{ titulo() }}</h1>
          </div>
          <div class="chips">
            <span class="chip fecha"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 5h14v15H5zM5 9h14M9 3v4m6-4v4" /></svg>{{ hoy }}</span>
            <span class="chip rol"><i></i>{{ rolLegible() }}</span>
          </div>
        </header>

        <main class="vista"><router-outlet /></main>
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
    nav { display: grid; gap: .2rem; align-content: start; overflow-y: auto; padding-right: 2px; flex: 1; scrollbar-width: none; }
    .grupo { margin: .9rem .6rem .3rem; font-size: .66rem; letter-spacing: .18em; text-transform: uppercase; color: var(--lila-500); font-weight: 500; }
    .grupo:first-child { margin-top: .2rem; }
    .item { position: relative; display: flex; align-items: center; gap: .75rem; padding: .5rem .6rem; border-radius: 16px; color: var(--tinta); text-decoration: none; transition: background .2s, transform .2s, box-shadow .2s; }
    .ico { width: 2.15rem; height: 2.15rem; border-radius: 12px; display: grid; place-items: center; background: rgba(255,255,255,.55); box-shadow: inset 0 1px 0 #fff; transition: background .2s, color .2s; color: var(--lila-700); }
    .ico svg { width: 19px; height: 19px; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }
    .item:hover { background: rgba(255,255,255,.5); transform: translateX(2px); }
    .item.activo { background: linear-gradient(100deg, rgba(255,255,255,.85), rgba(255,255,255,.4)); box-shadow: 0 10px 24px -14px rgba(59,45,92,.55), inset 0 1px 0 #fff; color: var(--lila-950); font-weight: 500; }
    .item.activo .ico { background: linear-gradient(145deg, var(--lila-500), var(--lila-900)); color: #fff; box-shadow: 0 8px 16px -8px rgba(59,45,92,.7), inset 0 1px 0 rgba(255,255,255,.35); }
    .item.activo::before { content: ''; position: absolute; left: -1rem; top: 22%; bottom: 22%; width: 4px; border-radius: 0 4px 4px 0; background: var(--lila-700); box-shadow: 0 0 12px var(--lila-500); }

    .perfil { display: flex; align-items: center; gap: .65rem; padding: .6rem; border-radius: 20px; background: rgba(255,255,255,.55); border: 1px solid rgba(255,255,255,.8); box-shadow: inset 0 1px 0 #fff; }
    .avatar { flex: none; width: 2.5rem; height: 2.5rem; border-radius: 50%; display: grid; place-items: center; color: #fff; font-weight: 600; font-size: .9rem;
      background: linear-gradient(135deg, var(--lila-300), var(--lila-800)); box-shadow: 0 0 0 3px rgba(255,255,255,.75), 0 8px 14px -8px rgba(59,45,92,.7); }
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
    .chips { display: flex; gap: .5rem; flex-wrap: wrap; }
    .chip { display: inline-flex; align-items: center; gap: .45rem; padding: .5rem .9rem; border-radius: 999px; font-size: .85rem; color: var(--lila-900);
      background: rgba(255,255,255,.55); border: 1px solid rgba(255,255,255,.8); box-shadow: inset 0 1px 0 #fff, 0 8px 18px -14px rgba(59,45,92,.5);
      -webkit-backdrop-filter: blur(14px); backdrop-filter: blur(14px); }
    .chip.fecha::first-letter { text-transform: uppercase; }
    .chip svg { width: 16px; height: 16px; fill: none; stroke: var(--lila-700); stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
    .chip.rol i { width: 8px; height: 8px; border-radius: 50%; background: #5cb88f; box-shadow: 0 0 0 3px rgba(92,184,143,.25); }
    .vista { display: grid; gap: 1.2rem; min-width: 0; animation: aparece .45s cubic-bezier(.2,.7,.2,1) both; }
    @keyframes aparece { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: none; } }

    @media (max-width: 1000px) {
      .marco { grid-template-columns: 1fr; padding: .8rem .8rem 6.2rem; }
      .contenido { order: 1; }
      /* En móvil el dock pasa abajo, como barra flotante de iconos */
      .lateral { order: 2; position: fixed; left: .7rem; right: .7rem; bottom: .7rem; top: auto; height: auto; z-index: 20; flex-direction: row; align-items: center; padding: .45rem; border-radius: 26px; gap: .3rem; }
      .marca, .grupo, .perfil .datos, .txt { display: none; }
      nav { display: flex; flex: 1; gap: .15rem; overflow-x: auto; }
      .item { padding: .3rem; flex: none; }
      .item.activo::before { display: none; }
      .perfil { padding: .2rem; background: none; border: 0; box-shadow: none; }
      .avatar { display: none; }
    }
    @media (prefers-reduced-motion: reduce) { .aurora i, .vista { animation: none; } }
    @media (prefers-reduced-transparency: reduce) { .lateral, .chip { -webkit-backdrop-filter: none; backdrop-filter: none; background: rgba(246,243,252,.96); } }
  `,
})
export class Panel {
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
      ?? (u.startsWith('/participar') ? { ...SECCIONES[6], titulo: 'Participar en el caso' } : SECCIONES[0]);
  });
  protected readonly nombreCorto = computed(() => this.sesion()?.nombreCompleto.split(' ')[0] ?? '');
  protected readonly titulo = computed(() =>
    this.actual() === SECCIONES[0] ? `Hola, ${this.nombreCorto()}` : this.actual().titulo,
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

  protected salir(): void {
    this.sesiones.cerrar();
    void this.router.navigate(['/login']);
  }
}
