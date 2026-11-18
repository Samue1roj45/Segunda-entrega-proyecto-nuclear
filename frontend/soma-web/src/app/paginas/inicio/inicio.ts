import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { mensajeDeError } from '../../core/con-operaciones';
import { Permiso } from '../../core/modelos/permiso';
import { SesionService } from '../../core/sesion.service';
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
  imports: [DatePipe, DecimalPipe],
  template: `
    <section class="bienvenida cristal">
      <div>
        <p class="sobre">{{ esEstudiante() ? 'Tu espacio de práctica' : esAdministrador() ? 'Administración' : 'Tu espacio docente' }}</p>
        <h2>Explora realidades,<br />comprende personas.</h2>
        <p class="suave">
          {{ esEstudiante()
            ? 'Entra a las sesiones a las que fuiste convocado, resuelve los casos y revisa tus resultados.'
            : esAdministrador()
              ? 'Gestiona los docentes de SOMA y los permisos de cada uno.'
              : 'Diseña casos, programa sesiones y acompaña el aprendizaje de tus estudiantes.' }}
        </p>
      </div>
      <div class="orbes" aria-hidden="true"><span></span><span></span><span></span></div>
    </section>

    <section class="resumen" aria-label="Resumen">
      @for (k of indicadores(); track k.titulo) {
        <article class="kpi cristal">
          <p class="suave">{{ k.titulo }}</p>
          <p class="valor">{{ k.valor }}</p>
          <p class="nota">{{ k.nota }}</p>
        </article>
      }
    </section>

    @if (aviso(); as texto) { <p class="aviso cristal" role="status">{{ texto }}</p> }

    @if (sesiones.visible()) {
      <section id="sesiones" class="bloque cristal">
        <header><h2>Mis sesiones</h2><span class="suave">{{ sesiones.datos().length }} programadas</span></header>
        @if (sesiones.cargando()) { <p class="suave">Cargando sesiones…</p> }
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
          @if (casos.cargando()) { <p class="suave">Cargando casos…</p> }
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
          @if (participaciones.cargando()) { <p class="suave">Cargando participaciones…</p> }
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
          @if (resultados.cargando()) { <p class="suave">Cargando resultados…</p> }
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
      position: relative; overflow: hidden; display: grid; grid-template-columns: 1fr auto; align-items: center;
      padding: 2rem 2.2rem; background: linear-gradient(120deg, rgba(255,255,255,.65), rgba(221,212,239,.55));
    }
    .bienvenida h2 { font-size: clamp(1.9rem, 2.6vw, 2.5rem); line-height: 1.05; margin: .3rem 0 .7rem; }
    .bienvenida .suave { max-width: 34rem; line-height: 1.5; }
    .sobre { text-transform: uppercase; letter-spacing: .18em; font-size: .72rem; color: var(--lila-700); }
    .orbes { position: relative; width: 220px; height: 130px; }
    .orbes span {
      position: absolute; border-radius: 50%;
      background: radial-gradient(circle at 35% 30%, #fff 0, rgba(226,217,243,.7) 45%, rgba(138,118,184,.55) 100%);
      box-shadow: 0 18px 30px -14px rgba(59,45,92,.45);
    }
    .orbes span:nth-child(1) { width: 110px; height: 110px; left: 20px; top: 10px; }
    .orbes span:nth-child(2) { width: 64px; height: 64px; left: 140px; top: 0; }
    .orbes span:nth-child(3) { width: 38px; height: 38px; left: 150px; top: 84px; }
    .resumen { display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); gap: 1.2rem; }
    .kpi { padding: 1.3rem 1.4rem; display: grid; gap: .2rem; }
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
      background: rgba(255,255,255,.5); border: 1px solid rgba(255,255,255,.8);
    }
    .lista .principal { flex: 1; display: grid; gap: .15rem; min-width: 0; }
    .lista strong { font-weight: 500; color: var(--lila-950); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .lista .suave { font-size: .85rem; }
    .vacio { padding: 1.4rem; text-align: center; color: var(--tinta-suave); border: 1px dashed rgba(94,74,138,.3); border-radius: 16px; }
    .nota-final { font-weight: 300; font-variant-numeric: tabular-nums; font-size: 1.6rem; color: var(--lila-900); }
    @media (max-width: 700px) { .orbes { display: none; } .bienvenida { grid-template-columns: 1fr; } }
    .nota-final small { font-size: 1rem; color: var(--tinta-suave); }
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

  protected readonly esEstudiante = computed(() => this.sesion.tienePermiso(Permiso.ParticiparEnCasos));
  protected readonly esAdministrador = this.sesion.esAdministrador;
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
    if (this.sesion.tienePermiso(Permiso.ProgramarSesiones)) this.sesiones.cargar(this.api.sesiones());
    if (this.sesion.tienePermiso(Permiso.GestionarCasos)) this.casos.cargar(this.api.casos());
    if (this.sesion.tienePermiso(Permiso.ConsultarParticipaciones) || this.esEstudiante()) {
      this.participaciones.cargar(this.api.participaciones());
    }
    if (this.sesion.tienePermiso(Permiso.ConsultarResultadosPropios) || this.sesion.tienePermiso(Permiso.ConsultarParticipaciones)) {
      this.resultados.cargar(this.api.resultados());
    }
    if (!this.sesiones.visible() && !this.casos.visible() && !this.participaciones.visible() && !this.resultados.visible()) {
      this.aviso.set('Tu rol administra docentes y permisos; esa sección se integrará en este panel.');
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
