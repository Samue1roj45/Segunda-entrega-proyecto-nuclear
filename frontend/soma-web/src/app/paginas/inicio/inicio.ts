import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { Barras } from '../../compartido/graficas';
import { Neuronas } from '../../compartido/neuronas';
import { Contar } from '../../compartido/contar';
import { mensajeDeError } from '../../core/con-operaciones';
import { Permiso } from '../../core/modelos/permiso';
import { SesionService } from '../../core/sesion.service';
import { Docente, Notificacion } from '../../core/modelos/dominio';
import { CasoResumen, Participacion, Resultado, Sesion, SomaApiService } from '../../core/soma-api.service';

/** Una lista del panel con sus estados de carga y error. */
class Bloque<T> {
  readonly datos = signal<T[]>([]);
  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);
  readonly visible = signal(false);

  cargar(peticion: Observable<T[]>): void {
    this.visible.set(true);
    this.cargando.set(true);
    this.error.set(null);
    peticion.subscribe({
      next: (datos) => {
        this.datos.set(datos);
        this.cargando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        if (e.status === 403) {
          this.visible.set(false); // el servidor no le da acceso: la sección no se muestra
        } else {
          this.error.set(mensajeDeError(e));
        }
      },
    });
  }
}

const ESTADOS: Record<string, string> = {
  SIN_INICIAR: 'Sin iniciar', EN_CURSO: 'En curso', FINALIZADO: 'Finalizada',
  BORRADOR: 'Borrador', PUBLICADO: 'Publicado',
  NO_INICIADA: 'No iniciada', FINALIZADA: 'Finalizada', INCOMPLETA: 'Incompleta',
};

/**
 * Inicio del panel: resumen en tarjetas y las listas que el rol puede ver (sesiones, casos,
 * participaciones y resultados), leídas del backend único.
 */
@Component({
  selector: 'app-inicio',
  imports: [Barras, Contar, Neuronas, DatePipe, DecimalPipe, RouterLink],
  template: `
    <section class="bienvenida cristal">
      <soma-neuronas />
      <div class="texto-bienvenida">
        <p class="sobre">{{ esEstudiante() ? 'Tu espacio de práctica' : esAdministrador() ? 'Administración' : 'Tu espacio docente' }}</p>
        <h2>Explora realidades,<br />comprende personas.</h2>
        <p class="suave">
          {{ esEstudiante()
            ? 'Entra a las sesiones a las que fuiste convocado, resuelve los casos y revisa tus resultados.'
            : esAdministrador()
              ? 'Gestiona los docentes de SOMA y los permisos de cada uno.'
              : 'Diseña casos, programa sesiones y acompaña el aprendizaje de tus estudiantes.' }}
        </p>
        <div class="accesos">
          @for (a of accesos(); track a.ruta) { <a class="boton-primario" [routerLink]="a.ruta">{{ a.texto }}</a> }
        </div>
      </div>
      <img class="cerebro" src="logo-soma.webp" alt="" aria-hidden="true" />
    </section>

    <section class="resumen" aria-label="Resumen">
      @for (k of indicadores(); track k.titulo) {
        <article class="kpi cristal">
          <p class="suave">{{ k.titulo }}</p>
          <p class="valor" [somaContar]="k.valor"></p>
          <p class="nota">{{ k.nota }}</p>
        </article>
      }
    </section>

    @if (esAdministrador() && docentes().length) {
      <section class="columnas">
        <article class="bloque cristal">
          <header><h2>Docentes por permiso</h2><span class="suave">{{ docentes().length }} docentes</span></header>
          <soma-barras [datos]="permisosPorDocente()" />
        </article>
        <article class="bloque cristal">
          <header><h2>Estado de la plataforma</h2></header>
          <ul class="lista">
            <li><div class="principal"><strong>Docentes activos</strong><span class="suave">{{ activos() }} de {{ docentes().length }}</span></div><span class="etiqueta ok">{{ activos() }}</span></li>
            <li><div class="principal"><strong>Docentes sin permisos</strong><span class="suave">Pueden iniciar sesión, pero no hacer nada todavía</span></div><span class="etiqueta" [class.mal]="sinPermisos() > 0">{{ sinPermisos() }}</span></li>
            <li><div class="principal"><strong>Docentes inactivos</strong><span class="suave">No pueden iniciar sesión</span></div><span class="etiqueta">{{ docentes().length - activos() }}</span></li>
          </ul>
        </article>
      </section>
    }

    @if (actividad().length) {
      <section class="bloque cristal">
        <header><h2>Actividad reciente</h2><span class="suave">últimos movimientos</span></header>
        <ol class="feed">
          @for (a of actividad(); track $index) {
            <li><span class="punto-f" [class]="'punto-f ' + a.tipo"></span><div><strong>{{ a.texto }}</strong><small>{{ a.fecha | date: "EEE d MMM, h:mm a" }}</small></div></li>
          }
        </ol>
      </section>
    }

    @if (aviso(); as texto) { <p class="aviso cristal" role="status">{{ texto }}</p> }

    @if (sesiones.visible()) {
      <section id="sesiones" class="bloque cristal">
        <header><h2>Mis sesiones</h2><span class="suave">{{ sesiones.datos().length }} programadas</span></header>
        @if (sesiones.cargando()) { <div class="esqueleto" aria-label="Cargando sesiones"><i></i><i></i></div> }
        @else if (sesiones.error(); as e) { <p class="alerta-error">{{ e }}</p> }
        @else if (!sesiones.datos().length) { <p class="vacio">Aún no tienes sesiones programadas.</p> }
        @else {
          <ul class="lista">
            @for (s of sesiones.datos(); track s.idPractica) {
              <li>
                <div class="principal">
                  <strong>{{ s.nombreCaso }}</strong>
                  <span class="suave">{{ s.espacioAcademico }} · {{ s.fechaInicio | date: 'd MMM, h:mm a' }} · {{ s.tiempoMaximoMinutos }} min</span>
                </div>
                <span class="etiqueta" [class.en-curso]="s.estado === 'EN_CURSO'" [class.finalizado]="s.estado === 'FINALIZADO'">{{ estado(s.estado) }}</span>
                @if (s.accionesPermitidas.includes('INICIAR')) {
                  <button type="button" class="boton-suave" (click)="iniciar(s)">Iniciar</button>
                }
                @if (s.accionesPermitidas.includes('FINALIZAR')) {
                  <button type="button" class="boton-suave" (click)="finalizar(s)">Finalizar</button>
                }
              </li>
            }
          </ul>
        }
      </section>
    }

    <div class="columnas">
      @if (casos.visible()) {
        <section id="casos" class="bloque cristal">
          <header><h2>Casos de estudio</h2><span class="suave">{{ publicados() }} publicados</span></header>
          @if (casos.cargando()) { <div class="esqueleto" aria-label="Cargando casos"><i></i><i></i></div> }
          @else if (casos.error(); as e) { <p class="alerta-error">{{ e }}</p> }
          @else if (!casos.datos().length) { <p class="vacio">Todavía no has creado casos.</p> }
          @else {
            <ul class="lista">
              @for (c of casos.datos(); track c.id) {
                <li>
                  <div class="principal">
                    <strong>{{ c.nombre }}</strong>
                    <span class="suave">{{ c.tiempoEstimadoMinutos ?? '—' }} min · actualizado {{ c.fechaActualizacion | date: 'd MMM y' }}</span>
                  </div>
                  <span class="etiqueta" [class.publicado]="c.estado === 'PUBLICADO'">{{ estado(c.estado) }}</span>
                </li>
              }
            </ul>
          }
        </section>
      }

      @if (participaciones.visible()) {
        <section id="participaciones" class="bloque cristal">
          <header><h2>{{ esEstudiante() ? 'Mis participaciones' : 'Participaciones' }}</h2></header>
          @if (participaciones.cargando()) { <div class="esqueleto" aria-label="Cargando participaciones"><i></i><i></i></div> }
          @else if (participaciones.error(); as e) { <p class="alerta-error">{{ e }}</p> }
          @else if (!participaciones.datos().length) { <p class="vacio">Sin participaciones registradas.</p> }
          @else {
            <ul class="lista">
              @for (p of participaciones.datos(); track p.id) {
                <li>
                  <div class="principal">
                    <strong>{{ p.practicaNombre }}</strong>
                    <span class="suave">{{ esEstudiante() ? 'Intento ' + p.numeroIntento : p.estudianteNombre + ' · intento ' + p.numeroIntento }}</span>
                  </div>
                  <span class="etiqueta" [class.en-curso]="p.estado === 'EN_CURSO'" [class.finalizado]="p.estado !== 'EN_CURSO'">{{ estado(p.estado) }}</span>
                </li>
              }
            </ul>
          }
        </section>
      }

      @if (resultados.visible()) {
        <section id="resultados" class="bloque cristal">
          <header><h2>Resultados</h2></header>
          @if (resultados.cargando()) { <div class="esqueleto" aria-label="Cargando resultados"><i></i><i></i></div> }
          @else if (resultados.error(); as e) { <p class="alerta-error">{{ e }}</p> }
          @else if (!resultados.datos().length) { <p class="vacio">Aún no hay calificaciones.</p> }
          @else {
            <ul class="lista">
              @for (r of resultados.datos(); track r.id) {
                <li>
                  <div class="principal">
                    <strong>{{ r.practicaNombre }}</strong>
                    <span class="suave">{{ r.correctas }} correctas · {{ r.incorrectas }} incorrectas · {{ r.noRespondidas }} sin responder</span>
                  </div>
                  <span class="nota-final">{{ r.notaFinal | number: '1.1-1' }}<small>/{{ r.escalaMaxima }}</small></span>
                </li>
              }
            </ul>
          }
        </section>
      }
    </div>
  `,
  styles: `
    :host { display: grid; gap: 1.4rem; }
    .bienvenida {
      position: relative; overflow: hidden; display: grid; grid-template-columns: 1fr auto; align-items: center; gap: 1rem;
      padding: 2.2rem 2.4rem; min-height: 230px;
      background: radial-gradient(90% 140% at 100% 0%, rgba(var(--d-201_184_240),.75), transparent 60%), linear-gradient(120deg, rgba(255,255,255,.78), rgba(var(--d-236_229_250),.5));
    }
    .texto-bienvenida, .cerebro { position: relative; z-index: 1; }
    .bienvenida h2 { font-size: clamp(2rem, 2.8vw, 2.8rem); line-height: 1.04; margin: .3rem 0 .8rem; }
    .bienvenida .suave { max-width: 34rem; line-height: 1.55; }
    .sobre { text-transform: uppercase; letter-spacing: .2em; font-size: .7rem; color: var(--lila-700); }
    .accesos { display: flex; flex-wrap: wrap; gap: .6rem; margin-top: 1.2rem; }
    .cerebro { width: clamp(130px, 16vw, 210px); height: auto; margin-right: .6rem; filter: drop-shadow(0 18px 22px rgba(var(--d-88_68_140),.3)); animation: flota 8s ease-in-out infinite; }
    @keyframes flota { 50% { transform: translateY(-9px) rotate(-1.5deg); } }
    .resumen { display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); gap: 1.2rem; }
    .kpi { position: relative; overflow: hidden; padding: 1.3rem 1.4rem 1.3rem 1.4rem; display: grid; gap: .2rem; transition: transform .25s, box-shadow .25s; }
    .kpi:hover { transform: translateY(-3px); }
    .kpi::before { content: ''; position: absolute; right: -28px; top: -28px; width: 110px; height: 110px; border-radius: 50%; background: radial-gradient(circle, var(--tono, var(--d-c9b8f0)), transparent 70%); opacity: .6; }
    .kpi::after { content: ''; position: absolute; left: 1.4rem; right: 1.4rem; bottom: 0; height: 3px; border-radius: 3px 3px 0 0; background: linear-gradient(90deg, var(--tono, var(--d-c9b8f0)), transparent); }
    .kpi:nth-child(2) { --tono: #aebcf0; } .kpi:nth-child(3) { --tono: #e8b8dd; } .kpi:nth-child(4) { --tono: #b7e0d0; }
    .kpi .valor { font-family: var(--sans); font-weight: 300; font-variant-numeric: tabular-nums; font-size: 2.6rem; line-height: 1; color: var(--lila-950); }
    .kpi .nota { font-size: .85rem; color: var(--lila-700); }
    .aviso { padding: .9rem 1.2rem; }
    .columnas { display: grid; grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)); gap: 1.4rem; align-items: start; }
    .bloque { padding: 1.4rem 1.5rem; display: grid; gap: 1rem; scroll-margin-top: 1.5rem; }
    .bloque header { display: flex; justify-content: space-between; align-items: baseline; }
    h2 { font-size: 1.7rem; }
    .lista { list-style: none; margin: 0; padding: 0; display: grid; gap: .6rem; }
    .lista li {
      display: flex; align-items: center; gap: .8rem; padding: .85rem 1rem; border-radius: 16px;
      background: rgba(255,255,255,.5); border: 1px solid rgba(255,255,255,.8); transition: transform .2s, background .2s, box-shadow .2s;
    }
    .lista li:hover { background: rgba(255,255,255,.8); transform: translateY(-1px); box-shadow: 0 12px 22px -16px rgba(var(--d-59_45_92),.5); }
    .lista .principal { flex: 1; display: grid; gap: .15rem; min-width: 0; }
    .lista strong { font-weight: 500; color: var(--lila-950); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .lista .suave { font-size: .85rem; }
    .feed { list-style: none; margin: 0; padding: 0 0 0 .4rem; display: grid; gap: .1rem; position: relative; }
    .feed li { display: flex; gap: .9rem; align-items: flex-start; padding: .55rem 0; position: relative; }
    .feed li::before { content: ''; position: absolute; left: .42rem; top: 1.6rem; bottom: -.5rem; width: 2px; background: rgba(var(--d-94_74_138),.16); } .feed li:last-child::before { display: none; }
    .punto-f { flex: none; width: .9rem; height: .9rem; border-radius: 50%; margin-top: .25rem; background: var(--lila-500); box-shadow: 0 0 0 4px rgba(var(--d-139_111_224),.18); position: relative; z-index: 1; }
    .punto-f.ok, .punto-f.fin { background: #5cb88f; box-shadow: 0 0 0 4px rgba(92,184,143,.2); } .punto-f.mal { background: #d98a5b; box-shadow: 0 0 0 4px rgba(217,138,91,.22); }
    .feed div { display: grid; gap: .1rem; min-width: 0; } .feed strong { font-weight: 500; color: var(--lila-950); font-size: .92rem; } .feed small { color: var(--tinta-suave); text-transform: capitalize; }
    :host-context([data-tema='noche']) .feed strong { color: var(--n-f1ecff); } :host-context([data-tema='noche']) .feed li::before { background: rgba(255,255,255,.14); }
    .vacio { padding: 1.4rem; text-align: center; color: var(--tinta-suave); border: 1px dashed rgba(var(--d-94_74_138),.3); border-radius: 16px; }
    .nota-final { font-weight: 300; font-variant-numeric: tabular-nums; font-size: 1.6rem; color: var(--lila-900); }
    @media (max-width: 700px) { .cerebro { display: none; } .bienvenida { grid-template-columns: 1fr; } }
    .nota-final small { font-size: 1rem; color: var(--tinta-suave); }
    :host-context([data-tema='noche']) .bienvenida { background: radial-gradient(90% 140% at 100% 0%, rgba(var(--n-s5),.5), transparent 60%), linear-gradient(120deg, rgba(var(--n-s3),.6), rgba(var(--n-s4),.5)); }
    :host-context([data-tema='noche']) .cerebro { filter: drop-shadow(0 0 26px rgba(var(--n-borde),.55)) brightness(1.25) hue-rotate(var(--giro, 0deg)) saturate(var(--sat, 1)); }
    :host-context([data-tema='noche']) .sobre, :host-context([data-tema='noche']) .kpi .nota { color: var(--n-a996e6); }
    :host-context([data-tema='noche']) .kpi .valor, :host-context([data-tema='noche']) .lista strong, :host-context([data-tema='noche']) .nota-final { color: var(--n-f1ecff); }
    :host-context([data-tema='noche']) .lista li { background: rgba(255,255,255,.06); border-color: rgba(var(--n-borde),.16); }
    :host-context([data-tema='noche']) .lista li:hover { background: rgba(255,255,255,.12); box-shadow: 0 12px 22px -16px #000; }
    :host-context([data-tema='noche']) .vacio { border-color: rgba(var(--n-borde),.3); }
    :host-context([data-tema='noche']) .esqueleto i { background: linear-gradient(100deg, rgba(255,255,255,.05) 30%, rgba(255,255,255,.16) 50%, rgba(255,255,255,.05) 70%) 0 0 / 220% 100%; }

  `,
})
export class Inicio implements OnInit {
  private readonly api = inject(SomaApiService);
  private readonly sesion = inject(SesionService);

  protected readonly sesiones = new Bloque<Sesion>();
  protected readonly casos = new Bloque<CasoResumen>();
  protected readonly participaciones = new Bloque<Participacion>();
  protected readonly resultados = new Bloque<Resultado>();
  protected readonly aviso = signal<string | null>(null);
  protected readonly docentes = signal<Docente[]>([]);
  private readonly avisosCorreo = signal<Notificacion[]>([]);
  protected readonly activos = computed(() => this.docentes().filter((d) => d.activo).length);
  protected readonly sinPermisos = computed(() => this.docentes().filter((d) => d.activo && d.permisos.length === 0).length);
  protected readonly permisosPorDocente = computed(() => {
    const n = Math.max(1, this.docentes().length); const m = new Map<string, number>();
    for (const d of this.docentes()) for (const p of d.permisos) m.set(p, (m.get(p) ?? 0) + 1);
    return [...m].map(([p, c]) => ({ etiqueta: p.toLowerCase().replace(/_/g, ' ').replace(/^./, (x) => x.toUpperCase()), valor: c / n }));
  });
  /** Movimientos recientes armados con lo que ya devuelve el servidor: sesiones, avisos enviados y participaciones. */
  protected readonly actividad = computed(() => {
    const e: { fecha: string; texto: string; tipo: string }[] = [];
    for (const s of this.sesiones.datos()) {
      const etiqueta = s.estado === 'EN_CURSO' ? 'Sesión en curso' : s.estado === 'FINALIZADO' ? 'Sesión finalizada' : 'Sesión programada';
      e.push({ fecha: s.fechaInicio, texto: `${etiqueta}: «${s.nombreCaso}»`, tipo: s.estado === 'FINALIZADO' ? 'fin' : 'ini' });
    }
    for (const n of this.avisosCorreo()) e.push({ fecha: n.fechaCreacion, texto: `${n.asunto} → ${n.destinatarioCorreo}`, tipo: n.estado === 'ENVIADA' ? 'ok' : 'mal' });
    for (const p of this.participaciones.datos()) {
      if (p.inicio) e.push({ fecha: p.inicio, texto: `${p.estudianteNombre ? p.estudianteNombre + ' participó' : 'Participación'} en «${p.practicaNombre}» (intento ${p.numeroIntento})`, tipo: p.estado === 'FINALIZADA' ? 'fin' : 'ini' });
    }
    return e.filter((x) => x.fecha).sort((a, b) => b.fecha.localeCompare(a.fecha)).slice(0, 8);
  });

  protected readonly esEstudiante = computed(() => this.sesion.tienePermiso(Permiso.ParticiparEnCasos));
  protected readonly esAdministrador = this.sesion.esAdministrador;
  protected readonly accesos = computed(() => {
    const r: { ruta: string; texto: string }[] = [];
    const p = (x: Permiso) => this.sesion.tienePermiso(x);
    if (p(Permiso.GestionarCasos)) r.push({ ruta: '/casos', texto: 'Crear o revisar casos' });
    if (p(Permiso.ProgramarSesiones)) r.push({ ruta: '/sesiones', texto: 'Programar sesión' });
    if (p(Permiso.ParticiparEnCasos)) r.push({ ruta: '/mis-sesiones', texto: 'Ir a mis casos' });
    if (p(Permiso.InvitarEstudiantes)) r.push({ ruta: '/mis-estudiantes', texto: 'Invitar estudiantes a Ecos' });
    if (p(Permiso.ParticiparEnCasos)) r.push({ ruta: '/ecos', texto: 'Jugar Ecos de una decisión' });
    if (p(Permiso.ConsultarResultadosPropios)) r.push({ ruta: '/mis-resultados', texto: 'Ver mis resultados' });
    if (this.sesion.esAdministrador()) { r.push({ ruta: '/docentes', texto: 'Gestionar docentes' }); r.push({ ruta: '/usuarios', texto: 'Administrar usuarios' }); }
    return r;
  });
  protected readonly publicados = computed(() => this.casos.datos().filter((c) => c.estado === 'PUBLICADO').length);

  protected readonly indicadores = computed(() => {
    const k: { titulo: string; valor: number | string; nota: string }[] = [];
    if (this.sesiones.visible()) {
      const enCurso = this.sesiones.datos().filter((s) => s.estado === 'EN_CURSO').length;
      k.push({ titulo: 'Sesiones', valor: this.sesiones.datos().length, nota: `${enCurso} en curso` });
    }
    if (this.casos.visible()) {
      k.push({ titulo: 'Casos', valor: this.casos.datos().length, nota: `${this.publicados()} publicados` });
    }
    if (this.participaciones.visible()) {
      const fin = this.participaciones.datos().filter((p) => p.estado === 'FINALIZADA').length;
      k.push({ titulo: 'Participaciones', valor: this.participaciones.datos().length, nota: `${fin} finalizadas` });
    }
    if (this.resultados.visible()) {
      const notas = this.resultados.datos().map((r) => Number(r.notaFinal));
      const promedio = notas.length ? (notas.reduce((a, b) => a + b, 0) / notas.length).toFixed(1) : '—';
      k.push({ titulo: 'Promedio', valor: promedio, nota: `${notas.length} calificaciones` });
    }
    return k;
  });

  ngOnInit(): void {
    if (this.sesion.esAdministrador()) this.api.docentes().subscribe({ next: (d) => this.docentes.set(d), error: () => undefined });
    if (this.sesion.tienePermiso(Permiso.ProgramarSesiones)) this.api.notificaciones().subscribe({ next: (n) => this.avisosCorreo.set(n), error: () => undefined });
    if (this.sesion.tienePermiso(Permiso.ProgramarSesiones)) this.sesiones.cargar(this.api.sesiones());
    if (this.sesion.tienePermiso(Permiso.GestionarCasos)) this.casos.cargar(this.api.casos());
    if (this.sesion.tienePermiso(Permiso.ConsultarParticipaciones) || this.esEstudiante()) {
      this.participaciones.cargar(this.api.participaciones());
    }
    if (this.sesion.tienePermiso(Permiso.ConsultarResultadosPropios) || this.sesion.tienePermiso(Permiso.ConsultarParticipaciones)) {
      this.resultados.cargar(this.api.resultados());
    }
    if (!this.sesiones.visible() && !this.casos.visible() && !this.participaciones.visible() && !this.resultados.visible()) {
      this.aviso.set(this.sesion.esAdministrador() ? null : 'Tu rol todavía no tiene secciones disponibles. Pide al administrador que te asigne permisos.');
    }
  }

  protected estado(codigo: string): string {
    return ESTADOS[codigo] ?? codigo;
  }

  protected iniciar(s: Sesion): void {
    this.api.iniciarSesion(s.idPractica).subscribe({
      next: () => this.sesiones.cargar(this.api.sesiones()),
      error: (e: HttpErrorResponse) => this.aviso.set(mensajeDeError(e)),
    });
  }

  protected finalizar(s: Sesion): void {
    this.api.finalizarSesion(s.idPractica).subscribe({
      next: () => this.sesiones.cargar(this.api.sesiones()),
      error: (e: HttpErrorResponse) => this.aviso.set(mensajeDeError(e)),
    });
  }
}
