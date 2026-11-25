import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { claseEstado, nombreEstado } from '../../compartido/formato';
import { mensajeDeError } from '../../core/con-operaciones';
import { DialogoService } from '../../core/dialogo.service';
import { Permiso } from '../../core/modelos/permiso';
import { SesionService } from '../../core/sesion.service';
import { ToastService } from '../../core/toast.service';
import { CasoEliminado } from '../../core/modelos/dominio';

/** «Activos» son los publicados: los que se pueden programar. Archivados y papelera son cosas distintas. */
const FILTROS: readonly { valor: string; texto: string; ayuda: string }[] = [
  { valor: '', texto: 'Todos', ayuda: 'Todos tus casos: borradores, activos y archivados. Los de la papelera no aparecen aquí.' },
  { valor: 'PUBLICADO', texto: 'Activos', ayuda: 'Casos publicados y disponibles para programar sesiones.' },
  { valor: 'BORRADOR', texto: 'Borradores', ayuda: 'Casos en preparación: se pueden editar y publicar.' },
  { valor: 'ARCHIVADO', texto: 'Archivados', ayuda: 'Casos guardados para reutilizarlos, con todo su contenido y sus sesiones anteriores. «Recuperar» los vuelve a activar; «Duplicar» crea una copia editable. Archivar no es eliminar.' },
];
import { CasoResumen, SomaApiService } from '../../core/soma-api.service';

/**
 * Módulo 2: casos de estudio del docente. Lista con filtro por categoría (todos, activos, borradores, archivados),
 * archivo y recuperación desde la lista, duplicación para reutilizar el contenido, papelera e importación DSL.
 */
@Component({
  selector: 'app-casos-lista',
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina">
        <h2 class="sr-only">Casos de estudio</h2>
        <div class="acciones">
          <button type="button" class="boton-suave" (click)="importando.set(!importando())">Importar caso (DSL)</button>
          <a routerLink="/casos/nuevo" class="boton-primario enlace-boton">Nuevo caso</a>
        </div>
      </div>

      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (aviso(); as a) { <p class="aviso-ok" role="status">{{ a }}</p> }

      @if (importando()) {
        <section class="cristal bloque">
          <form class="formulario" (ngSubmit)="importar()">
            <h3>Importar un caso</h3>
            <p class="ayuda">Elige un archivo .json exportado desde SOMA o pega aquí su contenido. Se crea como borrador.</p>
            <input type="file" accept=".json,application/json" (change)="leerArchivo($event)" />
            <label class="campo-f">Contenido DSL
              <textarea name="dsl" [(ngModel)]="textoDsl" rows="8" placeholder='{"dsl": "...", "version": "...", "caso": { ... }}'></textarea>
            </label>
            <div class="acciones">
              <button class="boton-primario" type="submit" [disabled]="ocupado() || !textoDsl.trim()">Importar</button>
              <button class="boton-suave" type="button" (click)="importando.set(false)">Cancelar</button>
            </div>
          </form>
        </section>
      }

      <section class="cristal bloque">
        <div class="filtros">
          <label class="campo-f">Mostrar
            <select [ngModel]="estado()" (ngModelChange)="cambiarEstado($event)" name="estado">
              @for (f of filtros; track f.valor) { <option [value]="f.valor">{{ f.texto }}</option> }
            </select>
          </label>
        </div>
        <p class="ayuda">{{ ayudaFiltro() }}</p>
      </section>

      @if (cargando()) { <div class="esqueleto" role="status" aria-label="Cargando casos"><i></i><i></i><i></i></div> }
      @else if (casos().length === 0) {
        <p class="cristal vacio">{{ estado() === 'ARCHIVADO' ? 'No tienes casos archivados.' : 'Todavía no hay casos' + (estado() ? ' en esta categoría' : '') + '. Crea el primero con «Nuevo caso».' }}</p>
      }
      @else {
        <div class="tarjetas">
          @for (c of casos(); track c.id) {
            <article class="cristal tarjeta-item">
              <span class="etiqueta" [class]="'etiqueta ' + clase(c.estado)">{{ nombre(c.estado) }}</span>
              <h3><a [routerLink]="['/casos', c.id]">{{ c.nombre }}</a></h3>
              <div class="meta">
                @if (c.tiempoEstimadoMinutos) { <span>{{ c.tiempoEstimadoMinutos }} min estimados</span> }
                @if (c.fechaActualizacion) { <span>Actualizado {{ c.fechaActualizacion | date: 'd MMM y' }}</span> }
                @if (c.fechaPublicacion) { <span>Publicado {{ c.fechaPublicacion | date: 'd MMM y' }}</span> }
              </div>
              <div class="acciones">
                <a [routerLink]="['/casos', c.id]" class="boton-suave boton-chico enlace-boton">Abrir</a>
                @if (c.estado === 'ARCHIVADO') {
                  <button type="button" class="boton-primario boton-chico" [disabled]="cambiando() === c.id" [attr.aria-busy]="cambiando() === c.id" (click)="recuperar(c)" title="Vuelve a estar disponible para programar sesiones">{{ cambiando() === c.id ? 'Recuperando…' : 'Recuperar' }}</button>
                }
                <button type="button" class="boton-suave boton-chico" [disabled]="duplicando() === c.id" [attr.aria-busy]="duplicando() === c.id" (click)="duplicar(c)" title="Crea una copia editable como borrador; el original no cambia">{{ duplicando() === c.id ? 'Duplicando…' : 'Duplicar' }}</button>
                @if (c.estado === 'PUBLICADO') {
                  <button type="button" class="boton-suave boton-chico" [disabled]="cambiando() === c.id" [attr.aria-busy]="cambiando() === c.id" (click)="archivar(c)" title="Lo guarda para reutilizarlo; deja de ofrecerse para sesiones nuevas">{{ cambiando() === c.id ? 'Archivando…' : 'Archivar' }}</button>
                }
                @if (puedeEliminar()) { <button type="button" class="boton-peligro boton-chico" [disabled]="eliminando() === c.id" [attr.aria-busy]="eliminando() === c.id" (click)="eliminar(c)">{{ eliminando() === c.id ? 'Eliminando…' : 'Eliminar' }}</button> }
              </div>
            </article>
          }
        </div>
      }

      @if (puedeEliminar()) {
        <section class="cristal papelera" aria-label="Papelera de casos">
          <button type="button" class="boton-suave boton-chico" id="papelera-toggle" [attr.aria-expanded]="papeleraAbierta()" (click)="alternarPapelera()">
            Papelera ({{ papelera().length }})
          </button>
          @if (papeleraAbierta()) {
            @if (papelera().length === 0) { <p class="vacio">La papelera está vacía.</p> }
            @else {
              <ul class="lista-papelera">
                @for (p of papelera(); track p.id) {
                  <li>
                    <span><strong>{{ p.nombre || '(sin nombre)' }}</strong> · eliminado {{ p.fechaEliminacion | date: 'd MMM y, HH:mm' }}</span>
                    <span class="acciones">
                      <button type="button" class="boton-suave boton-chico" [disabled]="trabajandoPapelera() === p.id" (click)="restaurar(p)">Restaurar</button>
                      <button type="button" class="boton-peligro boton-chico" [disabled]="trabajandoPapelera() === p.id" (click)="borrarParaSiempre(p)">Borrar para siempre</button>
                    </span>
                  </li>
                }
              </ul>
            }
          }
        </section>
      }
    </div>
  `,
})
export class CasosLista implements OnInit {
  private readonly api = inject(SomaApiService);
  private readonly router = inject(Router);

  protected readonly casos = signal<CasoResumen[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly estado = signal('');
  protected readonly importando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly duplicando = signal<number | null>(null);
  protected readonly cambiando = signal<number | null>(null);
  protected readonly filtros = FILTROS;
  protected readonly ayudaFiltro = computed(() => FILTROS.find((f) => f.valor === this.estado())?.ayuda ?? '');
  private readonly toasts = inject(ToastService);
  private readonly dialogos = inject(DialogoService);
  private readonly sesiones = inject(SesionService);
  protected readonly puedeEliminar = computed(() => this.sesiones.tienePermiso(Permiso.EliminarCasos));
  protected readonly eliminando = signal<number | null>(null);
  protected textoDsl = '';
  protected readonly nombre = nombreEstado;
  protected readonly clase = claseEstado;

  ngOnInit(): void { this.cargar(); this.cargarPapelera(); }

  protected cambiarEstado(valor: string): void { this.estado.set(valor); this.cargar(); }

  private cargar(): void {
    this.cargando.set(true);
    this.api.casosPorEstado(this.estado() || undefined).subscribe({
      next: (c) => { this.casos.set(c); this.cargando.set(false); },
      error: (e: HttpErrorResponse) => { this.error.set(mensajeDeError(e)); this.cargando.set(false); },
    });
  }

  /** Copia un caso (también publicado) como borrador nuevo: exporta su DSL y lo vuelve a importar con otro nombre. */
  protected duplicar(c: CasoResumen): void {
    this.duplicando.set(c.id);
    this.api.exportarDsl(c.id).subscribe({
      next: (doc) => {
        const copia = JSON.parse(JSON.stringify(doc)) as { caso?: { nombre?: string } };
        if (copia.caso) copia.caso.nombre = `${(copia.caso.nombre ?? c.nombre).slice(0, 185)} (copia)`;
        this.api.importarDsl(copia).subscribe({
          next: (r) => { this.duplicando.set(null); this.toasts.ok('Caso duplicado como borrador.'); void this.router.navigate(['/casos', r.caso.id]); },
          error: (e: HttpErrorResponse) => { this.duplicando.set(null); this.toasts.error(mensajeDeError(e)); },
        });
      },
      error: (e: HttpErrorResponse) => { this.duplicando.set(null); this.toasts.error(mensajeDeError(e)); },
    });
  }

  /** Archivar no borra nada: el caso sale de los activos y queda en «Archivados» para reutilizarlo. */
  protected archivar(c: CasoResumen): void {
    void this.dialogos.confirmar({
      titulo: 'Archivar el caso', mensaje: `«${c.nombre}» dejará de ofrecerse para programar sesiones nuevas.`,
      detalle: ['Conserva todo su contenido y sus sesiones anteriores.', 'Lo encontrarás en «Archivados», donde puedes recuperarlo o duplicarlo.'],
      confirmar: 'Archivar',
    }).then((ok) => {
      if (!ok) { return; }
      this.cambiando.set(c.id);
      this.api.archivarCaso(c.id).subscribe({
        next: (d) => { this.cambiando.set(null); this.reflejarEstado(c.id, d.estado); this.toasts.ok('Caso archivado. Está en «Archivados».'); },
        error: (e: HttpErrorResponse) => { this.cambiando.set(null); this.toasts.error(mensajeDeError(e)); },
      });
    });
  }

  /** El servidor vuelve a validar el caso con las reglas vigentes antes de activarlo. */
  protected recuperar(c: CasoResumen): void {
    void this.dialogos.confirmar({
      titulo: 'Recuperar el caso', mensaje: `«${c.nombre}» volverá a estar activo y disponible para programar sesiones.`,
      detalle: ['Se valida otra vez con las reglas académicas.', 'Su contenido no cambia; si necesitas modificarlo, usa «Duplicar».'],
      confirmar: 'Recuperar',
    }).then((ok) => {
      if (!ok) { return; }
      this.cambiando.set(c.id);
      this.api.recuperarCaso(c.id).subscribe({
        next: (d) => { this.cambiando.set(null); this.reflejarEstado(c.id, d.estado); this.toasts.ok('Caso recuperado. Ya está entre los activos.'); },
        error: (e: HttpErrorResponse) => { this.cambiando.set(null); this.toasts.error(mensajeDeError(e)); },
      });
    });
  }

  /** Actualiza la tarjeta al instante; si ya no corresponde al filtro elegido, la quita de la vista. */
  private reflejarEstado(id: number, estado: CasoResumen['estado']): void {
    const filtro = this.estado();
    this.casos.update((l) => l
      .map((x) => (x.id === id ? { ...x, estado } : x))
      .filter((x) => !filtro || x.estado === filtro));
  }

  /** Permiso adicional que da el administrador. El servidor lo vuelve a comprobar y rechaza casos con sesiones. */
  protected eliminar(c: CasoResumen): void {
    void this.dialogos.confirmar({ titulo: 'Eliminar el caso', mensaje: `«${c.nombre}» se moverá a la papelera.`, detalle: ['Podrás restaurarlo desde la papelera.'], confirmar: 'Eliminar', peligro: true }).then((ok) => {
      if (!ok) { return; }
      this.eliminando.set(c.id);
      this.api.eliminarCaso(c.id).subscribe({
        next: () => { this.eliminando.set(null); this.casos.update((l) => l.filter((x) => x.id !== c.id)); this.toasts.ok('Caso movido a la papelera.'); this.cargarPapelera(); },
        error: (e: HttpErrorResponse) => { this.eliminando.set(null); this.toasts.error(mensajeDeError(e)); },
      });
    });
  }

  protected readonly papelera = signal<CasoEliminado[]>([]);
  protected readonly papeleraAbierta = signal(false);
  protected readonly trabajandoPapelera = signal<number | null>(null);

  protected alternarPapelera(): void { this.papeleraAbierta.update((v) => !v); }

  private cargarPapelera(): void {
    if (!this.puedeEliminar()) { return; }
    this.api.papeleraCasos().subscribe({ next: (l) => this.papelera.set(l), error: () => this.papelera.set([]) });
  }

  protected restaurar(c: CasoEliminado): void {
    this.trabajandoPapelera.set(c.id);
    this.api.restaurarCaso(c.id).subscribe({
      next: () => { this.trabajandoPapelera.set(null); this.toasts.ok('Caso restaurado.'); this.cargarPapelera(); this.cargar(); },
      error: (e: HttpErrorResponse) => { this.trabajandoPapelera.set(null); this.toasts.error(mensajeDeError(e)); },
    });
  }

  protected borrarParaSiempre(c: CasoEliminado): void {
    void this.dialogos.confirmar({ titulo: 'Borrar para siempre', mensaje: `«${c.nombre}» se borrará con sus escenarios, preguntas y rúbrica.`, detalle: ['Esta acción no se puede deshacer.'], confirmar: 'Borrar para siempre', peligro: true }).then((ok) => {
      if (!ok) { return; }
      this.trabajandoPapelera.set(c.id);
      this.api.eliminarCasoDefinitivo(c.id).subscribe({
        next: () => { this.trabajandoPapelera.set(null); this.toasts.ok('Caso borrado definitivamente.'); this.cargarPapelera(); },
        error: (e: HttpErrorResponse) => { this.trabajandoPapelera.set(null); this.toasts.error(mensajeDeError(e)); },
      });
    });
  }

  protected leerArchivo(evento: Event): void {
    const archivo = (evento.target as HTMLInputElement).files?.[0];
    if (!archivo) { return; }
    void archivo.text().then((t) => (this.textoDsl = t));
  }

  protected importar(): void {
    let documento: unknown;
    try { documento = JSON.parse(this.textoDsl); } catch { this.error.set('El contenido no es un JSON válido.'); return; }
    this.error.set(null);
    this.ocupado.set(true);
    this.api.importarDsl(documento).subscribe({
      next: (r) => {
        this.ocupado.set(false);
        this.aviso.set(r.advertencias.length ? `Caso importado con ${r.advertencias.length} advertencia(s): ${r.advertencias.join(' · ')}` : 'Caso importado como borrador.');
        void this.router.navigate(['/casos', r.caso.id]);
      },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
    });
  }
}
