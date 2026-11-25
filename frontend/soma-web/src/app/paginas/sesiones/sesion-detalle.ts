import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { claseEstado, desdeInputFecha, nombreEstado, paraInputFecha } from '../../compartido/formato';
import { DialogoService } from '../../core/dialogo.service';
import { mensajeDeError } from '../../core/con-operaciones';
import { Convocado, EstudianteResumen, Intentos, Notificacion, SesionDetalle } from '../../core/modelos/dominio';
import { ToastService } from '../../core/toast.service';
import { CasoResumen, SomaApiService } from '../../core/soma-api.service';

/** Programación y administración de una sesión: condiciones, convocados, intentos extra y avisos por correo. */
@Component({
  selector: 'app-sesion-detalle',
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <div class="pagina">
      <p class="migas"><a routerLink="/sesiones">Mis sesiones</a> / {{ sesion()?.nombreCaso ?? 'Programar sesión' }}</p>
      <div class="cabecera-pagina">
        <h2>{{ sesion()?.nombreCaso ?? 'Programar sesión' }}</h2>
        @if (sesion(); as s) {
          <div class="acciones">
            <span [class]="'etiqueta ' + clase(s.estado)">{{ nombre(s.estado) }}</span>
            @if (s.accionesPermitidas.includes('INICIAR')) { <button type="button" class="boton-primario boton-chico" [disabled]="ocupado()" (click)="mover(true)">Iniciar</button> }
            @if (s.accionesPermitidas.includes('FINALIZAR')) { <button type="button" class="boton-peligro boton-chico" [disabled]="ocupado()" (click)="mover(false)">Finalizar</button> }
          </div>
        }
      </div>

      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (aviso(); as a) { <p class="aviso-ok" role="status">{{ a }}</p> }

      @if (sesion(); as s) {
        <section class="cristal bloque">
          <h3>Recorrido de la sesión</h3>
          <ol class="linea-tiempo">
            @for (p of pasos(); track p.titulo) {
              <li [class.hecho]="p.hecho" [class.actual]="p.actual"><strong>{{ p.titulo }}</strong><small>{{ p.detalle }}</small></li>
            }
          </ol>
        </section>
      }

      <section class="cristal bloque">
        <form class="formulario" (ngSubmit)="guardar()">
          <h3>{{ nueva() ? 'Condiciones de la sesión' : 'Condiciones' }}</h3>
          @if (nueva()) {
            <label class="campo-f">Caso publicado
              <select name="caso" [(ngModel)]="form.idCaso" required>
                <option [ngValue]="null" disabled>Elige un caso…</option>
                @for (c of casos(); track c.id) { <option [ngValue]="c.id">{{ c.nombre }}</option> }
              </select>
              @if (casos().length === 0) { <span class="ayuda">No tienes casos publicados. Publica uno desde «Casos de estudio».</span> }
            </label>
          }
          <label class="campo-f">Espacio académico<input name="esp" [(ngModel)]="form.espacioAcademico" maxlength="150" required [disabled]="!editable()" /></label>
          <div class="rejilla-2">
            <label class="campo-f">Inicio<input name="fi" type="datetime-local" [(ngModel)]="form.fechaInicio" required [disabled]="!editable()" /></label>
            <label class="campo-f">Fin<input name="ff" type="datetime-local" [(ngModel)]="form.fechaFin" required [disabled]="!editable()" /></label>
            <label class="campo-f">Tiempo máximo por estudiante (min)<input name="tm" type="number" min="1" [(ngModel)]="form.tiempoMaximoMinutos" required [disabled]="!editable()" /></label>
            <label class="campo-f">Lugar (si es presencial)<input name="lu" [(ngModel)]="form.lugar" maxlength="150" [disabled]="!editable()" /></label>
          </div>
          <label class="campo-f">Enlace de acceso (si es virtual)<input name="url" [(ngModel)]="form.urlAcceso" maxlength="255" [disabled]="!editable()" /></label>
          <label class="campo-f">Mensaje para los estudiantes<textarea name="msg" rows="3" [(ngModel)]="form.mensajePersonalizado" [disabled]="!editable()"></textarea></label>
          @if (nueva()) {
            @if (estudiantes().length) {
              <div class="campo-f selector-est">
                <span>Estudiantes a convocar <strong>({{ seleccion().size }} elegidos)</strong></span>
                <input class="entrada" type="search" name="buscar" placeholder="Buscar por nombre o correo…" [ngModel]="busqueda()" (ngModelChange)="busqueda.set($event)" />
                <div class="lista-est" role="group" aria-label="Estudiantes disponibles">
                  @for (e of filtrados(); track e.id) {
                    <label class="fila-est" [class.sel]="seleccion().has(e.id)">
                      <input type="checkbox" [checked]="seleccion().has(e.id)" (change)="alternar(e.id)" />
                      <span class="avatar-mini" aria-hidden="true">{{ iniciales(e.nombreCompleto) }}</span>
                      <span class="quien"><strong>{{ e.nombreCompleto }}</strong><small>{{ e.correoInstitucional }}</small></span>
                    </label>
                  } @empty { <p class="vacio">Ningún estudiante coincide con la búsqueda.</p> }
                </div>
                <div class="acciones"><button type="button" class="boton-suave boton-chico" (click)="elegirTodos()">Elegir los {{ filtrados().length }} visibles</button><button type="button" class="boton-suave boton-chico" (click)="seleccion.set(new_set())">Quitar selección</button></div>
              </div>
            } @else {
            <label class="campo-f">IDs de los estudiantes a convocar
              <input name="ids" [(ngModel)]="idsTexto" placeholder="Ej.: 4, 5, 6" required />
              <span class="ayuda">Separa los identificadores con comas. Cada uno debe ser un estudiante activo.</span>
            </label>
            }
          }
          @if (editable()) { <div class="acciones"><button class="boton-primario" type="submit" [disabled]="ocupado()">{{ nueva() ? 'Programar sesión' : 'Guardar cambios' }}</button></div> }
          @else { <p class="aviso-info">La sesión finalizó, por eso sus condiciones ya no se pueden modificar.</p> }
        </form>
      </section>

      @if (sesion(); as s) {
        <section class="cristal bloque">
          <div class="cabecera-pagina">
            <h3>Estudiantes convocados</h3>
            <span class="suave">{{ convocados().length }} en total</span>
          </div>
          <ul class="lista-simple">
            @for (c of convocados(); track c.idAutorizacion) {
              <li class="fila-item fila-persona">
                <span class="quien"><strong>{{ c.nombre }}</strong><small>{{ c.correo }} · ID {{ c.idEstudiante }}</small></span>
                <span class="estado-persona">
                  <span class="etiqueta">{{ nombre(c.estado) }}</span>
                  @if (intentos()[c.idAutorizacion]; as i) { <span class="ayuda">Intentos {{ i.intentosUsados }} de {{ i.intentosPermitidos }}</span> }
                </span>
                <span class="acciones">
                  <button type="button" class="boton-suave boton-chico" (click)="verIntentos(c)">Ver intentos</button>
                  <button type="button" class="boton-suave boton-chico" (click)="repetir(c)">Autorizar repetición</button>
                  @if (editable()) { <button type="button" class="boton-peligro boton-chico" (click)="retirar(s.idPractica, c)">Retirar</button> }
                </span>
              </li>
            } @empty { <li class="vacio">Esta sesión todavía no tiene estudiantes.</li> }
          </ul>
          @if (editable()) {
            @if (disponibles().length) {
              <form class="filtros" (ngSubmit)="agregar(s.idPractica)">
                <label class="campo-f">Agregar estudiante
                  <select name="nid" [(ngModel)]="nuevoId" required>
                    <option [ngValue]="null" disabled>Elige un estudiante…</option>
                    @for (e of disponibles(); track e.id) { <option [ngValue]="e.id">{{ e.nombreCompleto }} · {{ e.correoInstitucional }}</option> }
                  </select>
                </label>
                <button class="boton-suave" type="submit" [disabled]="ocupado() || !nuevoId">Agregar</button>
              </form>
            } @else {
              <form class="filtros" (ngSubmit)="agregar(s.idPractica)">
                <label class="campo-f">Agregar estudiante por ID<input name="nid" type="number" min="1" [(ngModel)]="nuevoId" required /></label>
                <button class="boton-suave" type="submit" [disabled]="ocupado() || !nuevoId">Agregar</button>
              </form>
            }
          }
        </section>

        <section class="cristal bloque">
          <h3>Avisos por correo</h3>
          <div class="acciones">
            <button type="button" class="boton-primario" [disabled]="ocupado()" (click)="convocatoria(s.idPractica)">Enviar convocatoria</button>
          </div>
          <form class="formulario" (ngSubmit)="cambio(s.idPractica)">
            <label class="campo-f">Avisar un cambio a los estudiantes<input name="mot" [(ngModel)]="motivoCambio" placeholder="Motivo del cambio (obligatorio)" required /></label>
            <div class="acciones"><button class="boton-suave" type="submit" [disabled]="ocupado() || !motivoCambio.trim()">Notificar cambio</button></div>
          </form>
          <h4>Historial</h4>
          <div class="tabla-envoltorio">
            <table class="tabla">
              <thead><tr><th>Fecha</th><th>Para</th><th>Asunto</th><th>Estado</th></tr></thead>
              <tbody>
                @for (n of notificaciones(); track n.id) {
                  <tr><td>{{ n.fechaCreacion | date: 'd MMM, HH:mm' }}</td><td>{{ n.destinatarioCorreo }}</td><td>{{ n.asunto }}</td>
                    <td><span [class]="'etiqueta ' + (n.estado === 'ENVIADA' ? 'ok' : n.estado === 'ERROR' ? 'mal' : '')" [title]="n.errorMensaje ?? ''">{{ nombre(n.estado) }}</span></td></tr>
                } @empty { <tr><td colspan="4" class="vacio">Aún no se han enviado avisos.</td></tr> }
              </tbody>
            </table>
          </div>
        </section>

        <section class="cristal bloque">
          <h3>Resultados de esta sesión</h3>
          <div class="acciones">
            <a class="boton-suave enlace-boton" [routerLink]="['/participaciones']" [queryParams]="{ practica: s.idPractica }">Ver participaciones</a>
            <a class="boton-suave enlace-boton" [routerLink]="['/resultados']" [queryParams]="{ practica: s.idPractica }">Ver resultados y calificar</a>
          </div>
          @if (s.fechaInicioReal) { <p class="meta"><span>Iniciada {{ s.fechaInicioReal | date: 'd MMM y, HH:mm' }}</span>@if (s.fechaCierreReal) { <span>Cerrada {{ s.fechaCierreReal | date: 'd MMM y, HH:mm' }} ({{ s.tipoCierre }})</span> }</p> }
        </section>
      }
    </div>
  `,
})
export class SesionDetalleVista implements OnInit {
  private readonly api = inject(SomaApiService);
  private readonly dialogos = inject(DialogoService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);
  protected readonly nombre = nombreEstado;
  protected readonly clase = claseEstado;

  protected readonly sesion = signal<SesionDetalle | null>(null);
  protected readonly nueva = signal(false);
  protected readonly casos = signal<CasoResumen[]>([]);
  protected readonly convocados = signal<Convocado[]>([]);
  protected readonly notificaciones = signal<Notificacion[]>([]);
  protected readonly intentos = signal<Record<number, Intentos>>({});
  protected readonly ocupado = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly estudiantes = signal<EstudianteResumen[]>([]);
  protected readonly busqueda = signal('');
  protected readonly seleccion = signal<Set<number>>(new Set());
  protected readonly filtrados = computed(() => {
    const q = this.busqueda().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    return this.estudiantes().filter((e) => !q || `${e.nombreCompleto} ${e.correoInstitucional}`.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').includes(q));
  });
  protected readonly disponibles = computed(() => {
    const ya = new Set(this.convocados().map((c) => c.idEstudiante));
    return this.estudiantes().filter((e) => !ya.has(e.id));
  });
  protected readonly pasos = computed(() => {
    const s = this.sesion(); if (!s) return [];
    const convoc = this.notificaciones().filter((n) => /CONVOC/i.test(n.tipo + n.asunto));
    const publ = this.notificaciones().filter((n) => /CALIF/i.test(n.tipo + n.asunto));
    const f = (d: string | null | undefined): string => (d ? new Date(d).toLocaleString('es-CO', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' }) : 'Pendiente');
    return [
      { titulo: 'Programada', detalle: f(s.fechaInicio) + ' (inicio previsto)', hecho: true, actual: s.estado === 'SIN_INICIAR' && !convoc.length },
      { titulo: 'Convocatoria enviada', detalle: convoc.length ? f(convoc[0].fechaCreacion) : 'Pendiente', hecho: convoc.length > 0, actual: s.estado === 'SIN_INICIAR' && convoc.length > 0 },
      { titulo: 'Iniciada', detalle: f(s.fechaInicioReal), hecho: !!s.fechaInicioReal, actual: s.estado === 'EN_CURSO' },
      { titulo: 'Finalizada', detalle: f(s.fechaCierreReal), hecho: s.estado === 'FINALIZADO', actual: s.estado === 'FINALIZADO' && !publ.length },
      { titulo: 'Calificaciones publicadas', detalle: publ.length ? f(publ[0].fechaCreacion) : 'Pendiente', hecho: publ.length > 0, actual: false },
    ];
  });
  protected readonly editable = computed(() => this.nueva() || this.sesion()?.estado !== 'FINALIZADO');

  protected form = { idCaso: null as number | null, espacioAcademico: '', fechaInicio: '', fechaFin: '', tiempoMaximoMinutos: 60 as number | null, lugar: '', urlAcceso: '', mensajePersonalizado: '' };
  protected idsTexto = '';
  protected nuevoId: number | null = null;
  protected motivoCambio = '';

  protected iniciales(n: string): string { return n.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join(''); }
  protected new_set(): Set<number> { return new Set(); }
  protected alternar(id: number): void { this.seleccion.update((s) => { const n = new Set(s); if (n.has(id)) n.delete(id); else n.add(id); return n; }); }
  protected elegirTodos(): void { this.seleccion.update((s) => { const n = new Set(s); this.filtrados().forEach((e) => n.add(e.id)); return n; }); }

  ngOnInit(): void {
    this.api.estudiantes().subscribe({ next: (e) => this.estudiantes.set(e), error: () => this.estudiantes.set([]) });
    // Se escucha el parámetro (no solo la primera lectura): al programar una sesión la ruta pasa de «nueva» a su id
    // sin recrear la pantalla, y hay que cambiar de modo.
    this.ruta.paramMap.subscribe((p) => {
      const id = p.get('id');
      this.error.set(null); this.aviso.set(null);
      if (id === 'nueva') {
        this.nueva.set(true); this.sesion.set(null); this.convocados.set([]); this.notificaciones.set([]);
        this.api.casosPorEstado('PUBLICADO').subscribe({ next: (c) => this.casos.set(c), error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)) });
        return;
      }
      this.nueva.set(false);
      this.cargar(Number(id));
    });
  }

  private cargar(id: number): void {
    this.api.sesionDetalle(id).subscribe({
      next: (s) => {
        this.sesion.set(s);
        this.form = { idCaso: s.idCaso, espacioAcademico: s.espacioAcademico, fechaInicio: paraInputFecha(s.fechaInicio), fechaFin: paraInputFecha(s.fechaFin), tiempoMaximoMinutos: s.tiempoMaximoMinutos, lugar: s.lugar ?? '', urlAcceso: s.urlAcceso ?? '', mensajePersonalizado: s.mensajePersonalizado ?? '' };
      },
      error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)),
    });
    this.api.convocados(id).subscribe({ next: (c) => this.convocados.set(c), error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)) });
    this.api.notificaciones(id).subscribe({ next: (n) => this.notificaciones.set(n), error: () => this.notificaciones.set([]) });
  }

  private ejecutar<T>(op: Observable<T>, mensaje: string, tras?: (r: T) => void): void {
    this.error.set(null); this.aviso.set(null); this.ocupado.set(true);
    op.subscribe({
      next: (r) => { this.ocupado.set(false); this.aviso.set(mensaje); this.toasts.ok(mensaje); tras?.(r); },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); this.toasts.error(mensajeDeError(e)); },
    });
  }

  protected guardar(): void {
    const f = this.form;
    const comun = { espacioAcademico: f.espacioAcademico, fechaInicio: desdeInputFecha(f.fechaInicio), fechaFin: desdeInputFecha(f.fechaFin), tiempoMaximoMinutos: Number(f.tiempoMaximoMinutos), lugar: f.lugar || null, mensajePersonalizado: f.mensajePersonalizado || null, urlAcceso: f.urlAcceso || null };
    if (this.nueva()) {
      const ids = this.estudiantes().length ? [...this.seleccion()] : this.idsTexto.split(/[,\s]+/).map((x) => Number(x)).filter((n) => Number.isInteger(n) && n > 0);
      if (!f.idCaso || ids.length === 0) { this.error.set('Elige un caso y elige al menos un estudiante.'); return; }
      this.ejecutar(this.api.agendarSesion({ ...comun, idCaso: f.idCaso, idsEstudiantes: ids }), 'Sesión programada.', (r) => void this.router.navigate(['/sesiones', r.idPractica]));
    } else {
      const id = this.sesion()!.idPractica;
      this.ejecutar(this.api.modificarSesion(id, comun), 'Condiciones actualizadas. Puedes avisar el cambio a los estudiantes.', () => this.cargar(id));
    }
  }

  protected mover(iniciar: boolean): void {
    const id = this.sesion()!.idPractica;
    const hacer = (): void => this.ejecutar(iniciar ? this.api.iniciarSesion(id) : this.api.finalizarSesion(id), iniciar ? 'Sesión iniciada.' : 'Sesión finalizada.', () => this.cargar(id));
    if (iniciar) { hacer(); return; }
    void this.dialogos.confirmar({ titulo: 'Finalizar la sesión', mensaje: 'Los estudiantes que estén en curso quedarán cerrados.', confirmar: 'Finalizar', peligro: true }).then((ok) => { if (ok) hacer(); });
  }

  protected agregar(id: number): void { this.ejecutar(this.api.agregarConvocado(id, this.nuevoId!), 'Estudiante agregado.', () => { this.nuevoId = null; this.cargar(id); }); }
  protected retirar(id: number, c: Convocado): void {
    void this.dialogos.confirmar({ titulo: 'Retirar estudiante', mensaje: `¿Retirar a ${c.nombre} de la sesión? Su historial se conserva.`, confirmar: 'Retirar', peligro: true }).then((ok) => { if (ok) { this.ejecutar(this.api.retirarConvocado(id, c.idEstudiante), 'Estudiante retirado.', () => this.cargar(id)); } });
  }

  protected verIntentos(c: Convocado): void {
    this.api.intentos(c.idAutorizacion).subscribe({ next: (i) => this.intentos.update((m) => ({ ...m, [c.idAutorizacion]: i })), error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)) });
  }

  protected repetir(c: Convocado): void {
    void this.dialogos.pedirTexto({ titulo: 'Autorizar un intento adicional', mensaje: `Para ${c.nombre}.`, confirmar: 'Autorizar', campo: { etiqueta: 'Motivo (opcional)', placeholder: 'Ej.: falla de conexión durante el intento' } }).then((motivo) => {
      if (motivo === null) return;
      this.ejecutar(this.api.autorizarRepeticion(c.idAutorizacion, motivo.trim() || null), 'Intento adicional autorizado.', (i) => this.intentos.update((m) => ({ ...m, [c.idAutorizacion]: i })));
    });
  }

  protected convocatoria(id: number): void {
    this.ejecutar(this.api.enviarConvocatoria(id), 'Convocatoria enviada.', (r) => { this.aviso.set(`Convocatoria: ${r.enviadas} enviada(s), ${r.omitidasDuplicado} ya enviada(s), ${r.errores} con error.`); this.cargar(id); });
  }

  protected cambio(id: number): void {
    this.ejecutar(this.api.notificarCambio(id, this.motivoCambio.trim()), 'Cambio notificado.', (r) => { this.aviso.set(`Cambio notificado: ${r.enviadas} enviada(s), ${r.errores} con error.`); this.motivoCambio = ''; this.cargar(id); });
  }
}
