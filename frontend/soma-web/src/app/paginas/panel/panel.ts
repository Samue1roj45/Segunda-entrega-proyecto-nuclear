import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Fondo } from '../../compartido/fondo';
import { Logo } from '../../compartido/logo';
import { Permiso } from '../../core/modelos/permiso';
import { Rol } from '../../core/modelos/rol';
import { SesionService } from '../../core/sesion.service';

interface Seccion {
  readonly titulo: string;
  readonly icono: string;
  readonly ancla: string | null;
  readonly permiso: Permiso | null;
}

/** Secciones del menú lateral. Solo aparecen las que el rol tiene permitidas (RF-02). */
const SECCIONES: readonly Seccion[] = [
  { titulo: 'Inicio', icono: 'M4 11 12 4l8 7v8a1 1 0 0 1-1 1h-5v-6h-4v6H5a1 1 0 0 1-1-1z', ancla: null, permiso: null },
  { titulo: 'Mis sesiones', icono: 'M5 5h14v15H5zM5 9h14M9 3v4m6-4v4', ancla: 'sesiones', permiso: Permiso.ProgramarSesiones },
  { titulo: 'Casos de estudio', icono: 'M5 4h10l4 4v12H5zM14 4v5h5M8 13h8M8 17h5', ancla: 'casos', permiso: Permiso.GestionarCasos },
  { titulo: 'Participaciones', icono: 'M4 19c1.5-4 4.2-6 8-6s6.5 2 8 6M12 10a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z', ancla: 'participaciones', permiso: Permiso.ConsultarParticipaciones },
  { titulo: 'Mis casos', icono: 'M5 4h10l4 4v12H5zM14 4v5h5M9 14l2 2 4-4', ancla: 'participaciones', permiso: Permiso.ParticiparEnCasos },
  { titulo: 'Resultados', icono: 'M5 20V10m7 10V4m7 16v-7', ancla: 'resultados', permiso: Permiso.ConsultarResultadosPropios },
  { titulo: 'Reportes', icono: 'M6 3h9l4 4v14H6zM9 13h7M9 17h7M9 9h3', ancla: 'resultados', permiso: Permiso.DescargarReportes },
];

/**
 * Página interior de SOMA: menú lateral de cristal, cabecera con el usuario y el contenido de la
 * sección. Usa el mismo fondo del inicio de sesión para que la experiencia se sienta continua.
 */
@Component({
  selector: 'app-panel',
  imports: [Fondo, Logo, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <soma-fondo />
    <div class="velo" aria-hidden="true"></div>

    <div class="marco">
      <aside class="lateral cristal">
        <a routerLink="/inicio" class="marca" aria-label="SOMA, inicio">
          <soma-logo [tamano]="54" />
          <span>
            <strong>SOMA</strong>
            <small>Psicología social</small>
          </span>
        </a>

        <nav aria-label="Secciones">
          @for (s of secciones(); track s.titulo) {
            <a [routerLink]="'/inicio'" [fragment]="s.ancla ?? undefined"
               routerLinkActive="activo"
               [routerLinkActiveOptions]="{ paths: 'exact', queryParams: 'ignored', matrixParams: 'ignored', fragment: 'exact' }" class="item">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path [attr.d]="s.icono" /></svg>
              {{ s.titulo }}
            </a>
          }
        </nav>

        <div class="tarjeta-ayuda">
          <p class="cita">“Comprender al otro empieza por escuchar su contexto.”</p>
          <p class="suave">Simulaciones para el aprendizaje en psicología social</p>
        </div>
      </aside>

      <div class="principal">
        <header class="barra">
          <div>
            <p class="suave fecha">{{ hoy }}</p>
            <h1>Hola, {{ nombreCorto() }}</h1>
          </div>
          <div class="usuario cristal">
            <span class="avatar" aria-hidden="true">{{ iniciales() }}</span>
            <span class="datos">
              <strong>{{ sesion()?.nombreCompleto }}</strong>
              <small>{{ rolLegible() }}</small>
            </span>
            <button type="button" class="salir" (click)="salir()" aria-label="Cerrar sesión" title="Cerrar sesión">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 4h4v16h-4M10 8l-4 4 4 4M6 12h10" /></svg>
            </button>
          </div>
        </header>

        <router-outlet />
      </div>
    </div>
  `,
  styles: `
    :host { display: block; min-height: 100vh; }
    .velo { position: fixed; inset: 0; z-index: 0; background: rgba(240, 235, 250, .45); backdrop-filter: blur(6px); }
    .marco {
      position: relative; z-index: 1; display: grid; grid-template-columns: 270px 1fr;
      gap: 1.6rem; padding: 1.6rem; min-height: 100vh;
    }
    .lateral {
      position: sticky; top: 1.6rem; height: calc(100vh - 3.2rem);
      display: flex; flex-direction: column; gap: 1.8rem; padding: 1.6rem 1.2rem;
    }
    .marca { display: flex; align-items: center; gap: .7rem; text-decoration: none; padding: 0 .4rem; }
    .marca strong { display: block; font-family: var(--serif); font-size: 1.9rem; line-height: 1; color: var(--lila-950); letter-spacing: .05em; }
    .marca small { color: var(--tinta-suave); font-size: .75rem; letter-spacing: .12em; text-transform: uppercase; }
    nav { display: grid; gap: .3rem; }
    .item {
      display: flex; align-items: center; gap: .8rem; padding: .75rem .9rem; border-radius: 14px;
      color: var(--tinta); text-decoration: none; font-weight: 400; transition: background .15s;
    }
    .item svg { width: 21px; height: 21px; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }
    .item:hover { background: rgba(255,255,255,.55); }
    .item.activo { background: linear-gradient(180deg, var(--lila-800), var(--lila-950)); color: #fff; box-shadow: 0 10px 22px -12px rgba(42,31,66,.7); }
    .tarjeta-ayuda { margin-top: auto; padding: 1.1rem; border-radius: 18px; background: rgba(255,255,255,.5); display: grid; gap: .5rem; }
    .cita { font-family: var(--serif); font-size: 1.15rem; line-height: 1.25; color: var(--lila-900); }
    .tarjeta-ayuda .suave { font-size: .78rem; }

    .principal { display: grid; align-content: start; gap: 1.6rem; min-width: 0; }
    .barra { display: flex; justify-content: space-between; align-items: center; gap: 1rem; padding: .4rem .4rem 0; }
    .fecha { font-size: .9rem; }
    .fecha::first-letter { text-transform: uppercase; }
    h1 { font-size: clamp(2rem, 3vw, 2.8rem); }
    .usuario { display: flex; align-items: center; gap: .8rem; padding: .5rem .6rem .5rem .5rem; border-radius: 999px; }
    .avatar {
      width: 2.6rem; height: 2.6rem; border-radius: 50%; display: grid; place-items: center;
      background: linear-gradient(135deg, var(--lila-300), var(--lila-700)); color: #fff; font-weight: 600;
    }
    .datos { display: grid; line-height: 1.2; }
    .datos small { color: var(--tinta-suave); }
    .salir { border: 0; background: rgba(255,255,255,.6); border-radius: 50%; width: 2.4rem; height: 2.4rem; display: grid; place-items: center; }
    .salir:hover { background: #fff; }
    .salir svg { width: 19px; height: 19px; fill: none; stroke: var(--lila-900); stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }

    @media (max-width: 900px) {
      .marco { grid-template-columns: 1fr; }
      .lateral { position: static; height: auto; }
      .tarjeta-ayuda { display: none; }
      nav { grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); }
      .datos { display: none; }
    }
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
  protected readonly nombreCorto = computed(() => this.sesion()?.nombreCompleto.split(' ')[0] ?? '');
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
