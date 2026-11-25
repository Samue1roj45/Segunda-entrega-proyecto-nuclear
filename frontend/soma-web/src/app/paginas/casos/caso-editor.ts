import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { claseEstado, guardarArchivo, nombreEstado } from '../../compartido/formato';
import { DialogoService } from '../../core/dialogo.service';
import { mensajeDeError } from '../../core/con-operaciones';
import { CasoDetalle, Herramienta, InformeValidacion, Pregunta, TipoHerramienta } from '../../core/modelos/dominio';
import { SomaApiService } from '../../core/soma-api.service';

type Pestana = 'datos' | 'contenido' | 'rubrica' | 'herramientas' | 'publicacion';

/**
 * Editor de un caso (módulo 2): datos generales, escenarios con preguntas y respuestas, rúbrica con criterios,
 * herramientas de apoyo, validación y publicación. Después de cada cambio se vuelve a leer el caso del servidor,
 * que es quien aplica las reglas (por ejemplo, un caso publicado ya no se edita).
 */
@Component({
  selector: 'app-caso-editor',
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <div class="pagina">
      <p class="migas"><a routerLink="/casos">Casos de estudio</a> / {{ caso()?.nombre ?? 'Nuevo caso' }}</p>
      <div class="cabecera-pagina">
        <h2>{{ caso()?.nombre ?? 'Nuevo caso' }}</h2>
        <div class="acciones">
          @if (caso(); as c) {
            <span [class]="'etiqueta ' + clase(c.estado)">{{ nombre(c.estado) }}</span>
            <button type="button" class="boton-suave boton-chico" (click)="vista.set(true)">Vista previa</button>
          }
        </div>
      </div>

      @if (vista() && caso(); as c) {
        <div class="modal-fondo" (click)="vista.set(false)"></div>
        <div class="modal" role="dialog" aria-modal="true" aria-label="Vista previa del caso">
          <header><div><p class="sobre-modal">Así lo verá el estudiante</p><h3>{{ c.nombre }}</h3></div><button type="button" class="boton-suave boton-chico" (click)="vista.set(false)">Cerrar</button></header>
          <div class="cuerpo-modal">
            @if (c.contextoHistoria) { <p class="historia">{{ c.contextoHistoria }}</p> }
            @if (c.desarrolloSituacional) { <p class="historia">{{ c.desarrolloSituacional }}</p> }
            @for (e of c.escenarios; track e.id) {
              <section class="esc"><h4>Escenario {{ e.orden }} · {{ e.nombre }}</h4><p class="suave">{{ e.contexto }}</p>
                @for (p of e.preguntas; track p.id) {
                  <div class="preg"><p><strong>{{ p.orden }}. {{ p.enunciado }}</strong></p>
                    @for (r of p.respuestas; track r.id) { <label class="opc"><input type="radio" [name]="'vp' + p.id" disabled /><span><span class="codigo">{{ r.codigo }}</span> {{ r.texto }}</span></label> }
                  </div>
                }
              </section>
            } @empty { <p class="suave">Este caso todavía no tiene escenarios.</p> }
          </div>
        </div>
      }

      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (aviso(); as a) { <p class="aviso-ok" role="status">{{ a }}</p> }
      @if (cargando()) { <div class="esqueleto" role="status" aria-label="Cargando"><i></i><i></i><i></i></div> }

      @if (!nuevo()) {
        <div class="pestanas" role="tablist">
          @for (p of pestanas; track p.id) {
            <button type="button" role="tab" [class.activa]="pestana() === p.id" [attr.aria-selected]="pestana() === p.id" (click)="irA(p.id)">{{ p.titulo }}</button>
          }
        </div>
      }

      @if (nuevo() || pestana() === 'datos') {
        <section class="cristal bloque">
          <form class="formulario" (ngSubmit)="guardarDatos()">
            <label class="campo-f">Nombre del caso
              <input name="nombre" [(ngModel)]="datos.nombre" required maxlength="200" [disabled]="!editable()" />
            </label>
            <label class="campo-f">Tiempo estimado (minutos)
              <input name="tiempo" type="number" min="1" [(ngModel)]="datos.tiempoEstimadoMinutos" [disabled]="!editable()" />
            </label>
            <label class="campo-f">Contexto de la historia
              <textarea name="ctx" [(ngModel)]="datos.contextoHistoria" rows="4" [disabled]="!editable()"></textarea>
            </label>
            <label class="campo-f">Desarrollo situacional
              <textarea name="des" [(ngModel)]="datos.desarrolloSituacional" rows="5" [disabled]="!editable()"></textarea>
            </label>
            @if (editable()) {
              <div class="acciones"><button class="boton-primario" type="submit" [disabled]="ocupado()">{{ nuevo() ? 'Crear caso' : 'Guardar cambios' }}</button></div>
            } @else {
              <p class="aviso-info">Este caso ya no es un borrador, por eso sus datos no se pueden modificar.</p>
            }
          </form>
        </section>
      }

      @if (!nuevo() && caso(); as c) {
        @if (pestana() === 'contenido') {
          @for (e of c.escenarios; track e.id) {
            <section class="cristal bloque">
              <div class="cabecera-pagina">
                <h3>Escenario {{ e.orden }} · {{ e.nombre }}</h3>
                @if (editable()) {
                  <div class="acciones">
                    <button type="button" class="boton-suave boton-chico" (click)="abrir('esc', e.id, { nombre: e.nombre, contexto: e.contexto })">Editar</button>
                    <button type="button" class="boton-peligro boton-chico" (click)="borrar(api.borrarEscenario(c.id, e.id), 'Escenario eliminado.', '¿Eliminar este escenario con sus preguntas?')">Eliminar</button>
                  </div>
                }
              </div>
              @if (abierto('esc', e.id)) {
                <form class="formulario bloque-interno" (ngSubmit)="guardar(api.editarEscenario(c.id, e.id, { nombre: f['nombre'], contexto: f['contexto'] }), 'Escenario actualizado.')">
                  <label class="campo-f">Nombre<input name="n" [(ngModel)]="f['nombre']" required /></label>
                  <label class="campo-f">Contexto<textarea name="c" [(ngModel)]="f['contexto']" rows="3"></textarea></label>
                  <div class="acciones"><button class="boton-primario boton-chico" type="submit">Guardar</button><button class="boton-suave boton-chico" type="button" (click)="cerrar()">Cancelar</button></div>
                </form>
              } @else { <p class="suave">{{ e.contexto }}</p> }

              @for (p of e.preguntas; track p.id) {
                <div class="bloque-interno">
                  <div class="cabecera-pagina">
                    <h4>{{ p.orden }}. {{ p.enunciado }}</h4>
                    @if (editable()) {
                      <div class="acciones">
                        <button type="button" class="boton-suave boton-chico" (click)="abrir('preg', p.id, { enunciado: p.enunciado })">Editar</button>
                        <button type="button" class="boton-peligro boton-chico" (click)="borrar(api.borrarPregunta(c.id, p.id), 'Pregunta eliminada.', '¿Eliminar esta pregunta?')">Eliminar</button>
                      </div>
                    }
                  </div>
                  @if (abierto('preg', p.id)) {
                    <form class="formulario" (ngSubmit)="guardar(api.editarPregunta(c.id, p.id, f['enunciado']), 'Pregunta actualizada.')">
                      <label class="campo-f">Enunciado<textarea name="en" [(ngModel)]="f['enunciado']" rows="2" required></textarea></label>
                      <div class="acciones"><button class="boton-primario boton-chico" type="submit">Guardar</button><button class="boton-suave boton-chico" type="button" (click)="cerrar()">Cancelar</button></div>
                    </form>
                  }
                  <ul class="lista-simple">
                    @for (r of p.respuestas; track r.id) {
                      <li class="fila-item">
                        <span class="texto"><span class="codigo">{{ r.codigo }}</span> {{ r.texto }}
                          @if (r.justificacion) { <br /><span class="ayuda">Justificación: {{ r.justificacion }}</span> }
                        </span>
                        @if (r.esCorrecta) { <span class="etiqueta ok">Correcta</span> }
                        @if (editable()) {
                          <span class="acciones">
                            @if (!r.esCorrecta) { <button type="button" class="boton-suave boton-chico" (click)="guardar(api.marcarCorrecta(c.id, p.id, r.id), 'Respuesta correcta actualizada.')">Marcar correcta</button> }
                            <button type="button" class="boton-suave boton-chico" (click)="abrir('resp', r.id, { codigo: r.codigo, texto: r.texto, justificacion: r.justificacion ?? '', porcentaje: r.porcentaje })">Editar</button>
                            <button type="button" class="boton-peligro boton-chico" (click)="borrar(api.borrarRespuesta(c.id, r.id), 'Respuesta eliminada.', '¿Eliminar esta respuesta?')">Eliminar</button>
                          </span>
                        }
                      </li>
                      @if (abierto('resp', r.id)) {
                        <form class="formulario bloque-interno" (ngSubmit)="guardarRespuesta(c.id, r.id)">
                          <div class="rejilla-2">
                            <label class="campo-f">Código<input name="cod" [(ngModel)]="f['codigo']" maxlength="10" required /></label>
                            <label class="campo-f">Porcentaje<input name="por" type="number" step="any" [(ngModel)]="f['porcentaje']" /></label>
                          </div>
                          <label class="campo-f">Texto<textarea name="tx" [(ngModel)]="f['texto']" rows="2" required></textarea></label>
                          <label class="campo-f">Justificación<textarea name="ju" [(ngModel)]="f['justificacion']" rows="2"></textarea></label>
                          <div class="acciones"><button class="boton-primario boton-chico" type="submit">Guardar</button><button class="boton-suave boton-chico" type="button" (click)="cerrar()">Cancelar</button></div>
                        </form>
                      }
                    }
                  </ul>
                  @if (editable()) {
                    @if (abierto('nresp', p.id)) {
                      <form class="formulario bloque-interno" (ngSubmit)="crearRespuesta(c.id, p)">
                        <div class="rejilla-2">
                          <label class="campo-f">Código<input name="cod" [(ngModel)]="f['codigo']" maxlength="10" required /></label>
                          <label class="campo-f">Porcentaje<input name="por" type="number" step="any" [(ngModel)]="f['porcentaje']" /></label>
                        </div>
                        <label class="campo-f">Texto de la respuesta<textarea name="tx" [(ngModel)]="f['texto']" rows="2" required></textarea></label>
                        <label class="campo-f">Justificación (opcional)<textarea name="ju" [(ngModel)]="f['justificacion']" rows="2"></textarea></label>
                        <label class="ayuda"><input type="checkbox" name="corr" [(ngModel)]="f['esCorrecta']" /> Es la respuesta correcta</label>
                        <div class="acciones"><button class="boton-primario boton-chico" type="submit">Agregar respuesta</button><button class="boton-suave boton-chico" type="button" (click)="cerrar()">Cancelar</button></div>
                      </form>
                    } @else {
                      <div><button type="button" class="boton-suave boton-chico" (click)="abrir('nresp', p.id, { codigo: siguienteCodigo(p), texto: '', justificacion: '', porcentaje: null, esCorrecta: false })">+ Respuesta</button></div>
                    }
                  }
                </div>
              }

              @if (editable()) {
                @if (abierto('npreg', e.id)) {
                  <form class="formulario bloque-interno" (ngSubmit)="guardar(api.crearPregunta(c.id, e.id, f['enunciado']), 'Pregunta creada.')">
                    <label class="campo-f">Enunciado de la pregunta<textarea name="en" [(ngModel)]="f['enunciado']" rows="2" required></textarea></label>
                    <div class="acciones"><button class="boton-primario boton-chico" type="submit">Crear pregunta</button><button class="boton-suave boton-chico" type="button" (click)="cerrar()">Cancelar</button></div>
                  </form>
                } @else {
                  <div><button type="button" class="boton-suave boton-chico" (click)="abrir('npreg', e.id, { enunciado: '' })">+ Pregunta</button></div>
                }
              }
            </section>
          } @empty { <p class="cristal vacio">Este caso todavía no tiene escenarios.</p> }

          @if (editable()) {
            <section class="cristal bloque">
              @if (abierto('nesc', 0)) {
                <form class="formulario" (ngSubmit)="guardar(api.crearEscenario(c.id, { nombre: f['nombre'], contexto: f['contexto'] }), 'Escenario creado.')">
                  <h3>Nuevo escenario</h3>
                  <label class="campo-f">Nombre<input name="n" [(ngModel)]="f['nombre']" required /></label>
                  <label class="campo-f">Contexto<textarea name="c" [(ngModel)]="f['contexto']" rows="3"></textarea></label>
                  <div class="acciones"><button class="boton-primario" type="submit">Crear escenario</button><button class="boton-suave" type="button" (click)="cerrar()">Cancelar</button></div>
                </form>
              } @else { <button type="button" class="boton-primario" (click)="abrir('nesc', 0, { nombre: '', contexto: '' })">+ Escenario</button> }
            </section>
          }
        }

        @if (pestana() === 'rubrica') {
          <section class="cristal bloque">
            <h3>Rúbrica de evaluación</h3>
            <form class="formulario" (ngSubmit)="guardarRubrica(c.id)">
              <label class="campo-f">Nombre<input name="rn" [(ngModel)]="rubricaForm.nombre" required [disabled]="!editable()" /></label>
              <label class="campo-f">Descripción<textarea name="rd" [(ngModel)]="rubricaForm.descripcion" rows="2" [disabled]="!editable()"></textarea></label>
              @if (editable()) {
                <div class="acciones">
                  <button class="boton-primario" type="submit">{{ c.rubrica ? 'Guardar rúbrica' : 'Crear rúbrica' }}</button>
                  @if (c.rubrica) { <button type="button" class="boton-peligro" (click)="borrar(api.borrarRubrica(c.id), 'Rúbrica eliminada.', '¿Eliminar la rúbrica y todos sus criterios?')">Eliminar rúbrica</button> }
                </div>
              }
            </form>
          </section>
          @if (c.rubrica; as r) {
            <section class="cristal bloque">
              <h3>Criterios</h3>
              <ul class="lista-simple">
                @for (k of r.criteriosEvaluacion; track k.id) {
                  <li class="fila-item">
                    <span class="texto"><strong>{{ k.criterio }}</strong>
                      <span class="ayuda"> · rango {{ k.rango.min }} a {{ k.rango.max }}</span>
                      @if (k.parametros.length) { <br /><span class="ayuda">{{ k.parametros.join(' · ') }}</span> }
                    </span>
                    @if (editable()) {
                      <span class="acciones">
                        <button type="button" class="boton-suave boton-chico" (click)="editarCriterio(k.id, k.criterio, k.parametros, k.rango.min, k.rango.max)">Editar</button>
                        <button type="button" class="boton-peligro boton-chico" (click)="borrar(api.borrarCriterio(c.id, k.id), 'Criterio eliminado.', '¿Eliminar este criterio?')">Eliminar</button>
                      </span>
                    }
                  </li>
                } @empty { <li class="vacio">Aún no hay criterios.</li> }
              </ul>
              @if (editable()) {
                <form class="formulario bloque-interno" (ngSubmit)="guardarCriterio(c.id)">
                  <h4>{{ editandoCriterio() ? 'Editar criterio' : 'Nuevo criterio' }}</h4>
                  <label class="campo-f">Criterio<input name="cr" [(ngModel)]="criterio.criterio" required /></label>
                  <label class="campo-f">Parámetros (uno por línea)<textarea name="pa" [(ngModel)]="criterio.parametros" rows="3"></textarea></label>
                  <div class="rejilla-2">
                    <label class="campo-f">Rango mínimo<input name="mi" type="number" step="any" [(ngModel)]="criterio.min" required /></label>
                    <label class="campo-f">Rango máximo<input name="ma" type="number" step="any" [(ngModel)]="criterio.max" required /></label>
                  </div>
                  <div class="acciones">
                    <button class="boton-primario boton-chico" type="submit">{{ editandoCriterio() ? 'Guardar criterio' : 'Agregar criterio' }}</button>
                    @if (editandoCriterio()) { <button type="button" class="boton-suave boton-chico" (click)="nuevoCriterio()">Cancelar</button> }
                  </div>
                </form>
              }
            </section>
          }
        }

        @if (pestana() === 'herramientas') {
          <section class="cristal bloque">
            <h3>Herramientas de apoyo para el estudiante</h3>
            <ul class="lista-simple">
              @for (h of herramientas(); track h.idHerramienta) {
                <li class="fila-item">
                  <span class="texto"><strong>{{ h.nombre }}</strong> <span class="etiqueta">{{ h.tipo }}</span>
                    @if (h.descripcion) { <br /><span class="ayuda">{{ h.descripcion }}</span> }
                  </span>
                  <button type="button" class="boton-peligro boton-chico" (click)="quitarHerramienta(c.id, h)">Quitar</button>
                </li>
              } @empty { <li class="vacio">Este caso no tiene herramientas.</li> }
            </ul>
            <form class="formulario bloque-interno" (ngSubmit)="agregarHerramienta(c.id)">
              <h4>Agregar herramienta</h4>
              <div class="rejilla-2">
                <label class="campo-f">Nombre<input name="hn" [(ngModel)]="herramienta.nombre" maxlength="100" required /></label>
                <label class="campo-f">Tipo
                  <select name="ht" [(ngModel)]="herramienta.tipo">
                    @for (t of tipos; track t) { <option [value]="t">{{ t }}</option> }
                  </select>
                </label>
              </div>
              <label class="campo-f">Descripción<input name="hd" [(ngModel)]="herramienta.descripcion" maxlength="500" /></label>
              <label class="campo-f">Contenido o enlace<textarea name="hc" [(ngModel)]="herramienta.contenido" rows="3"></textarea></label>
              <div class="acciones"><button class="boton-primario boton-chico" type="submit">Agregar</button></div>
            </form>
          </section>
        }

        @if (pestana() === 'publicacion') {
          <section class="cristal bloque">
            <h3>Validación y publicación</h3>
            <p class="suave">Un caso solo se puede publicar si cumple las reglas académicas. Publicado, queda disponible para programar sesiones y ya no se edita.</p>
            @if (c.estado === 'ARCHIVADO') {
              <p class="aviso-info">Este caso está archivado: se conserva completo, con sus sesiones anteriores, pero no se ofrece para sesiones nuevas. «Recuperar» lo vuelve a activar; para modificarlo, duplícalo desde la lista de casos.</p>
            }
            <div class="acciones">
              <button type="button" class="boton-suave" (click)="validar(c.id)" [disabled]="ocupado()">Validar caso</button>
              @if (c.estado === 'BORRADOR') { <button type="button" class="boton-primario" (click)="publicar(c.id)" [disabled]="ocupado()">Publicar</button> }
              @if (c.estado === 'PUBLICADO') { <button type="button" class="boton-suave" (click)="archivar(c.id)" [disabled]="ocupado()">Archivar</button> }
              @if (c.estado === 'ARCHIVADO') { <button type="button" class="boton-primario" (click)="recuperar(c.id)" [disabled]="ocupado()">Recuperar</button> }
              <button type="button" class="boton-suave" (click)="exportar(c)" [disabled]="ocupado()">Exportar (DSL)</button>
            </div>
            @if (informe(); as i) {
              @if (i.valido) { <p class="aviso-ok">El caso cumple todas las reglas y está listo para publicarse.</p> }
              @else {
                <p class="alerta-error">El caso tiene {{ i.incumplimientos.length }} punto(s) por corregir:</p>
                <ul class="lista-simple">
                  @for (x of i.incumplimientos; track $index) {
                    <li class="fila-item"><span class="texto">{{ x.mensaje }} @if (x.ubicacion) { <span class="ayuda">({{ x.ubicacion }})</span> }</span></li>
                  }
                </ul>
              }
            }
            <p class="meta">
              @if (c.fechaCreacion) { <span>Creado {{ c.fechaCreacion | date: 'd MMM y, HH:mm' }}</span> }
              @if (c.fechaPublicacion) { <span>Publicado {{ c.fechaPublicacion | date: 'd MMM y, HH:mm' }}</span> }
              @if (c.fechaArchivado) { <span>Archivado {{ c.fechaArchivado | date: 'd MMM y, HH:mm' }}</span> }
            </p>
          </section>
        }
      }
    </div>
  `,
})
export class CasoEditor implements OnInit {
  protected readonly api = inject(SomaApiService);
  private readonly dialogos = inject(DialogoService);
  protected readonly vista = signal(false);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly pestanas: { id: Pestana; titulo: string }[] = [
    { id: 'datos', titulo: 'Datos' }, { id: 'contenido', titulo: 'Escenarios y preguntas' },
    { id: 'rubrica', titulo: 'Rúbrica' }, { id: 'herramientas', titulo: 'Herramientas' }, { id: 'publicacion', titulo: 'Publicación' },
  ];
  protected readonly tipos: TipoHerramienta[] = ['CALCULADORA', 'TABLA', 'DOCUMENTO', 'ENLACE', 'OTRA'];
  protected readonly nombre = nombreEstado;
  protected readonly clase = claseEstado;

  protected readonly caso = signal<CasoDetalle | null>(null);
  protected readonly nuevo = signal(false);
  protected readonly pestana = signal<Pestana>('datos');
  protected readonly cargando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly informe = signal<InformeValidacion | null>(null);
  protected readonly herramientas = signal<Herramienta[]>([]);
  protected readonly editable = computed(() => this.nuevo() || this.caso()?.estado === 'BORRADOR');

  protected datos = { nombre: '', desarrolloSituacional: '', contextoHistoria: '', tiempoEstimadoMinutos: null as number | null };
  protected rubricaForm = { nombre: '', descripcion: '' };
  protected criterio = { criterio: '', parametros: '', min: null as number | null, max: null as number | null };
  protected readonly editandoCriterio = signal<number | null>(null);
  protected herramienta = { nombre: '', tipo: 'DOCUMENTO' as TipoHerramienta, descripcion: '', contenido: '' };

  /** Formulario genérico de la edición en línea abierta (escenario, pregunta o respuesta). */
  protected f: Record<string, any> = {};
  private abiertoClave: string | null = null;

  ngOnInit(): void {
    // Se escucha el parámetro: al crear el caso la ruta pasa de «nuevo» a su id sin recrear la pantalla.
    this.ruta.paramMap.subscribe((p) => {
      const id = p.get('id');
      if (id === null || id === 'nuevo') { this.nuevo.set(true); this.caso.set(null); return; }
      this.nuevo.set(false);
      this.cargar(Number(id));
    });
  }

  private get id(): number { return this.caso()!.id; }

  private cargar(id: number): void {
    this.cargando.set(true);
    this.api.caso(id).subscribe({
      next: (c) => {
        this.caso.set(c); this.cargando.set(false);
        this.datos = { nombre: c.nombre, desarrolloSituacional: c.desarrolloSituacional ?? '', contextoHistoria: c.contextoHistoria ?? '', tiempoEstimadoMinutos: c.tiempoEstimadoMinutos };
        this.rubricaForm = { nombre: c.rubrica?.nombre ?? '', descripcion: c.rubrica?.descripcion ?? '' };
      },
      error: (e: HttpErrorResponse) => { this.cargando.set(false); this.error.set(mensajeDeError(e)); },
    });
  }

  protected irA(p: Pestana): void {
    this.pestana.set(p); this.error.set(null); this.aviso.set(null); this.cerrar();
    if (p === 'herramientas') { this.cargarHerramientas(); }
  }

  private cargarHerramientas(): void {
    this.api.herramientasCaso(this.id).subscribe({ next: (h) => this.herramientas.set(h), error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)) });
  }

  // ----- edición en línea -----
  protected abrir(tipo: string, id: number, valores: Record<string, any>): void { this.abiertoClave = `${tipo}-${id}`; this.f = { ...valores }; }
  protected cerrar(): void { this.abiertoClave = null; this.f = {}; }
  protected abierto(tipo: string, id: number): boolean { return this.abiertoClave === `${tipo}-${id}`; }
  protected siguienteCodigo(p: Pregunta): string { return String.fromCharCode(65 + p.respuestas.length); }

  /** Ejecuta una operación sobre el caso y vuelve a leerlo. */
  protected guardar(operacion: Observable<unknown>, mensaje: string): void {
    this.error.set(null); this.aviso.set(null); this.ocupado.set(true);
    operacion.subscribe({
      next: () => { this.ocupado.set(false); this.aviso.set(mensaje); this.cerrar(); this.cargar(this.id); },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
    });
  }

  protected borrar(operacion: Observable<unknown>, mensaje: string, confirmacion: string): void {
    void this.dialogos.confirmar({ titulo: 'Confirmar', mensaje: confirmacion, confirmar: 'Eliminar', peligro: true }).then((ok) => { if (ok) this.guardar(operacion, mensaje); });
  }

  protected guardarDatos(): void {
    const d = { ...this.datos, tiempoEstimadoMinutos: this.datos.tiempoEstimadoMinutos ?? null };
    this.error.set(null); this.ocupado.set(true);
    if (this.nuevo()) {
      this.api.crearCaso(d).subscribe({
        next: (c) => { this.ocupado.set(false); void this.router.navigate(['/casos', c.id]); },
        error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
      });
    } else {
      this.ocupado.set(false);
      this.guardar(this.api.editarCaso(this.id, d), 'Datos guardados.');
    }
  }

  protected crearRespuesta(caso: number, p: Pregunta): void {
    this.error.set(null); this.ocupado.set(true);
    this.api.crearRespuesta(caso, p.id, { codigo: this.f['codigo'], texto: this.f['texto'], justificacion: this.f['justificacion'] || null, porcentaje: this.f['porcentaje'] ?? null, esCorrecta: !!this.f['esCorrecta'] }).subscribe({
      next: () => { this.ocupado.set(false); this.aviso.set('Respuesta agregada.'); this.cerrar(); this.cargar(caso); },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
    });
  }

  protected guardarRespuesta(caso: number, id: number): void {
    this.guardar(this.api.editarRespuesta(caso, id, { codigo: this.f['codigo'], texto: this.f['texto'], justificacion: this.f['justificacion'] || null, porcentaje: this.f['porcentaje'] ?? null }), 'Respuesta actualizada.');
  }

  // ----- rúbrica -----
  protected guardarRubrica(caso: number): void { this.guardar(this.api.guardarRubrica(caso, this.rubricaForm), 'Rúbrica guardada.'); }

  protected nuevoCriterio(): void { this.editandoCriterio.set(null); this.criterio = { criterio: '', parametros: '', min: null, max: null }; }

  protected editarCriterio(id: number, criterio: string, parametros: string[], min: number | null, max: number | null): void {
    this.editandoCriterio.set(id);
    this.criterio = { criterio, parametros: parametros.join('\n'), min, max };
  }

  protected guardarCriterio(caso: number): void {
    const cuerpo = {
      criterio: this.criterio.criterio,
      parametros: this.criterio.parametros.split('\n').map((p) => p.trim()).filter(Boolean),
      rango: { min: this.criterio.min, max: this.criterio.max },
    };
    const id = this.editandoCriterio();
    this.guardar(id === null ? this.api.crearCriterio(caso, cuerpo) : this.api.editarCriterio(caso, id, cuerpo), id === null ? 'Criterio agregado.' : 'Criterio actualizado.');
    this.nuevoCriterio();
  }

  // ----- herramientas -----
  protected agregarHerramienta(caso: number): void {
    const h = this.herramienta;
    this.error.set(null); this.ocupado.set(true);
    this.api.agregarHerramienta(caso, { nombre: h.nombre, tipo: h.tipo, descripcion: h.descripcion || null, contenido: h.contenido || null, orden: null }).subscribe({
      next: () => { this.ocupado.set(false); this.aviso.set('Herramienta agregada.'); this.herramienta = { nombre: '', tipo: 'DOCUMENTO', descripcion: '', contenido: '' }; this.cargarHerramientas(); },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
    });
  }

  protected quitarHerramienta(caso: number, h: Herramienta): void {
    void this.dialogos.confirmar({ titulo: 'Quitar herramienta', mensaje: `¿Quitar la herramienta «${h.nombre}» del caso?`, confirmar: 'Quitar', peligro: true }).then((ok) => {
      if (!ok) return;
      this.api.quitarHerramienta(caso, h.idHerramienta).subscribe({ next: () => this.cargarHerramientas(), error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)) });
    });
  }

  // ----- publicación -----
  protected validar(caso: number): void {
    this.error.set(null); this.ocupado.set(true);
    this.api.validarCaso(caso).subscribe({
      next: (i) => { this.ocupado.set(false); this.informe.set(i); },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
    });
  }

  protected publicar(caso: number): void {
    void this.dialogos.confirmar({ titulo: 'Publicar el caso', mensaje: 'Después de publicarlo ya no podrás editarlo. Podrás duplicarlo para hacer una versión nueva.', confirmar: 'Publicar' }).then((ok) => { if (ok) this.guardar(this.api.publicarCaso(caso), 'Caso publicado.'); });
  }
  protected archivar(caso: number): void {
    void this.dialogos.confirmar({ titulo: 'Archivar el caso', mensaje: 'El caso dejará de estar disponible para programar sesiones nuevas.', detalle: ['Conserva todo su contenido y sus sesiones anteriores.', 'Podrás recuperarlo o duplicarlo desde «Archivados».'], confirmar: 'Archivar' }).then((ok) => { if (ok) this.guardar(this.api.archivarCaso(caso), 'Caso archivado.'); });
  }

  protected recuperar(caso: number): void {
    void this.dialogos.confirmar({ titulo: 'Recuperar el caso', mensaje: 'El caso volverá a estar activo y disponible para programar sesiones.', detalle: ['Se valida otra vez con las reglas académicas.'], confirmar: 'Recuperar' }).then((ok) => { if (ok) this.guardar(this.api.recuperarCaso(caso), 'Caso recuperado: ya está activo.'); });
  }

  protected exportar(c: CasoDetalle): void {
    this.api.exportarDsl(c.id).subscribe({
      next: (dsl) => guardarArchivo(new Blob([JSON.stringify(dsl, null, 2)], { type: 'application/json' }), `caso-${c.id}.json`),
      error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)),
    });
  }
}
