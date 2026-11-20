import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable, forkJoin } from 'rxjs';
import { Barras, Histograma } from '../../compartido/graficas';
import { DialogoService } from '../../core/dialogo.service';
import { ReportesService } from '../../core/reportes.service';
import { ToastService } from '../../core/toast.service';
import { guardarArchivo, nombreEstado } from '../../compartido/formato';
import { mensajeDeError } from '../../core/con-operaciones';
import { Permiso } from '../../core/modelos/permiso';
import { NotaOficial, ResultadoDetalle, Retroalimentacion } from '../../core/modelos/dominio';
import { SesionService } from '../../core/sesion.service';
import { SomaApiService } from '../../core/soma-api.service';

/**
 * Resultados de una sesión (módulo 5): notas de cada intento, detalle por pregunta, nota oficial, retroalimentación,
 * cierre de intentos pendientes, publicación de calificaciones y descarga de reportes en PDF y Excel.
 */
@Component({
  selector: 'app-resultados-docente',
  imports: [DatePipe, DecimalPipe, FormsModule, Histograma, Barras],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2 class="sr-only">Resultados</h2></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (aviso(); as a) { <p class="aviso-ok" role="status">{{ a }}</p> }

      <section class="cristal bloque">
        <form class="filtros" (ngSubmit)="buscar()">
          <label class="campo-f">ID de sesión<input name="p" type="number" min="1" [(ngModel)]="practica" /></label>
          <label class="campo-f">ID de estudiante<input name="e" type="number" min="1" [(ngModel)]="estudiante" /></label>
          <button class="boton-primario" type="submit">Buscar</button>
        </form>
        @if (practica) {
          <div class="acciones" style="margin-top:.9rem">
            <button type="button" class="boton-suave" [disabled]="ocupado()" (click)="evaluar()">Cerrar intentos pendientes</button>
            <button type="button" class="boton-suave" [disabled]="ocupado()" (click)="publicar()">Publicar calificaciones</button>
            @if (puedeReportes()) {
              <button type="button" class="boton-suave" [disabled]="reportes.generando('pdf', practica)" (click)="descargar('pdf')">{{ reportes.generando('pdf', practica) ? 'Generando…' : 'Reporte PDF' }}</button>
              <button type="button" class="boton-suave" [disabled]="reportes.generando('xlsx', practica)" (click)="descargar('xlsx')">{{ reportes.generando('xlsx', practica) ? 'Generando…' : 'Reporte Excel' }}</button>
            }
          </div>
          <p class="ayuda">«Cerrar intentos pendientes» evalúa los intentos incompletos o sin responder. «Publicar calificaciones» envía la nota oficial a cada estudiante por correo.</p>
        }
      </section>

      @if (resultados().length) {
        <section class="resumen-estadistico">
          <article class="cristal kpi"><p class="suave">Intentos evaluados</p><p class="valor">{{ resultados().length }}</p></article>
          <article class="cristal kpi"><p class="suave">Promedio</p><p class="valor">{{ estadisticas().promedio | number: '1.1-2' }}</p></article>
          <article class="cristal kpi"><p class="suave">Nota más alta</p><p class="valor">{{ estadisticas().maxima | number: '1.1-2' }}</p></article>
          <article class="cristal kpi"><p class="suave">Nota más baja</p><p class="valor">{{ estadisticas().minima | number: '1.1-2' }}</p></article>
        </section>
        <div class="dos">
          <section class="cristal bloque"><h3>Distribución de notas</h3>@defer (on idle) { <soma-histograma [valores]="proporciones()" /> } @placeholder { <p class="suave">Cargando gráfica…</p> }</section>
          <section class="cristal bloque"><h3>Aciertos por pregunta</h3>
            @if (aciertos().length) { @defer (on idle) { <soma-barras [datos]="aciertos()" /> } @placeholder { <p class="suave">Cargando gráfica…</p> } } @else { <p class="suave">{{ calculando() ? 'Calculando…' : 'Se mostrará cuando haya intentos con detalle.' }}</p> }
          </section>
        </div>
        @if (mapa().filas.length) {
          <section class="cristal bloque">
            <h3>Mapa de calor</h3>
            <p class="ayuda">Cada fila es un estudiante y cada columna una pregunta. Verde: correcta · rojo: incorrecta · gris: sin responder.</p>
            <div class="mapa-envoltorio" role="table" aria-label="Mapa de calor de respuestas por estudiante y pregunta">
              <div class="mapa" [style.grid-template-columns]="'minmax(130px, 200px) repeat(' + mapa().columnas.length + ', minmax(26px, 48px))'">
                <span class="m-vacio"></span>
                @for (c of mapa().columnas; track c.id) { <span class="m-col" [title]="c.texto">{{ c.n }}</span> }
                @for (f of mapa().filas; track f.nombre + f.intento) {
                  <span class="m-fila" [title]="f.nombre">{{ f.nombre }}</span>
                  @for (c of mapa().columnas; track c.id) {
                    <span class="m-cel" [class]="'m-cel ' + (f.celdas[c.id] || 'na')" [title]="f.nombre + ' · P' + c.n + ': ' + etiquetaCelda(f.celdas[c.id])"></span>
                  }
                }
              </div>
            </div>
          </section>
        }
      }

      @if (cargando()) { <p class="suave">Cargando…</p> }
      @else if (resultados().length === 0) { <p class="cristal vacio">No hay resultados todavía para esa búsqueda.</p> }
      @else {
        <section class="cristal bloque">
          <div class="barra-tabla">
            <label class="campo-f buscador">Buscar en la tabla<input type="search" [ngModel]="busqueda()" (ngModelChange)="buscarTexto($event)" name="bq" placeholder="Nombre o correo" /></label>
            <span class="ayuda">{{ filas().length }} de {{ resultados().length }} intentos</span>
            <button type="button" class="boton-suave boton-chico" [disabled]="!filas().length" (click)="exportarCsv()">Exportar lo filtrado (CSV)</button>
          </div>
          <div class="tabla-envoltorio">
            <table class="tabla">
              <thead><tr>
                <th [attr.aria-sort]="aria('estudiante')"><button type="button" class="orden" (click)="ordenar('estudiante')">Estudiante {{ flecha('estudiante') }}</button></th>
                <th>Sesión</th>
                <th class="num" [attr.aria-sort]="aria('intento')"><button type="button" class="orden" (click)="ordenar('intento')">Intento {{ flecha('intento') }}</button></th>
                <th class="num" [attr.aria-sort]="aria('correctas')"><button type="button" class="orden" (click)="ordenar('correctas')">Correctas {{ flecha('correctas') }}</button></th>
                <th class="num">Incorrectas</th><th class="num">Sin responder</th>
                <th class="num" [attr.aria-sort]="aria('nota')"><button type="button" class="orden" (click)="ordenar('nota')">Nota {{ flecha('nota') }}</button></th><th></th></tr></thead>
              <tbody>
                @for (r of visibles(); track r.id) {
                  <tr [class.activa]="seleccionado()?.id === r.id">
                    <td><strong>{{ r.estudianteNombre }}</strong><br /><span class="ayuda">{{ r.estudianteCorreo }}</span></td>
                    <td>{{ r.practicaNombre }}</td>
                    <td class="num">{{ r.numeroIntento }}</td>
                    <td class="num">{{ r.correctas }}</td><td class="num">{{ r.incorrectas }}</td><td class="num">{{ r.noRespondidas }}</td>
                    <td class="num"><strong>{{ r.notaFinal | number: '1.1-2' }}</strong> / {{ r.escalaMaxima }}
                      @if (r.oficial) { <br /><span class="etiqueta ok">Oficial</span> }</td>
                    <td><button type="button" class="boton-suave boton-chico" (click)="abrir(r)">{{ seleccionado()?.id === r.id ? 'Cerrar' : 'Detalle' }}</button></td>
                  </tr>
                } @empty { <tr><td colspan="8" class="suave">Ningún intento coincide con la búsqueda.</td></tr> }
              </tbody>
            </table>
          </div>
          @if (paginas() > 1) {
            <div class="paginacion">
              <button type="button" class="boton-suave boton-chico" [disabled]="pagina() === 1" (click)="pagina.set(pagina() - 1)">← Anterior</button>
              <span>Página {{ pagina() }} de {{ paginas() }}</span>
              <button type="button" class="boton-suave boton-chico" [disabled]="pagina() === paginas()" (click)="pagina.set(pagina() + 1)">Siguiente →</button>
            </div>
          }
        </section>
      }

      @if (seleccionado(); as r) {
        <section class="cristal bloque">
          <div class="nav-calificar">
            <button type="button" class="boton-suave boton-chico" [disabled]="indiceActual() <= 0" (click)="mover(-1)" aria-label="Estudiante anterior">←</button>
            <h3>{{ r.estudianteNombre }} · intento {{ r.numeroIntento }}</h3>
            <span class="ayuda">{{ indiceActual() + 1 }} de {{ filas().length }} · Alt + ← / →</span>
            <button type="button" class="boton-suave boton-chico" [disabled]="indiceActual() >= filas().length - 1" (click)="mover(1)" aria-label="Estudiante siguiente">→</button>
          </div>
          <p class="meta">
            <span>Peso obtenido {{ r.pesoObtenido }} de {{ r.pesoTotal }}</span>
            <span>Calculado {{ r.fechaCalculo | date: 'd MMM y, HH:mm' }}</span>
            @if (r.oficial) { <span><strong>Nota oficial: {{ r.notaOficial | number: '1.1-2' }}</strong></span> }
          </p>
          <ul class="lista-simple">
            @for (d of r.detallePreguntas; track d.preguntaId) {
              <li class="fila-item">
                <span class="texto">{{ d.enunciado }} <span class="ayuda">(peso {{ d.peso }})</span></span>
                <span [class]="'etiqueta ' + (!d.respondida ? '' : d.correcta ? 'ok' : 'mal')">{{ !d.respondida ? 'Sin responder' : d.correcta ? 'Correcta' : 'Incorrecta' }}</span>
              </li>
            }
          </ul>
          @if (puedeCalificar() && !r.oficial) {
            <div class="acciones"><button type="button" class="boton-primario" [disabled]="ocupado()" (click)="oficial(r)">Elegir como nota oficial</button></div>
          }

          @if (puedeRetro()) {
            <form class="formulario bloque-interno" (ngSubmit)="retroalimentar(r)">
              <h4>Retroalimentación para el estudiante</h4>
              @for (t of retros(); track t.id) { <div class="fila-item"><span class="texto">{{ t.texto }}</span><span class="ayuda">{{ t.fechaCreacion | date: 'd MMM, HH:mm' }}</span></div> }
              <div class="plantillas">
                <label class="campo-f">Usar una plantilla
                  <select (change)="usarPlantilla($any($event.target).value); $any($event.target).value = ''" aria-label="Plantilla de mensaje">
                    <option value="">Elegir…</option>
                    @for (pl of plantillas(); track pl) { <option [value]="pl">{{ pl.length > 60 ? pl.slice(0, 60) + '…' : pl }}</option> }
                  </select></label>
                <button type="button" class="boton-suave boton-chico" [disabled]="!textoRetro.trim()" (click)="guardarPlantilla()">Guardar este texto como plantilla</button>
              </div>
              <label class="campo-f">Nueva retroalimentación<textarea name="rt" rows="3" [(ngModel)]="textoRetro" required></textarea></label>
              <div class="acciones"><button class="boton-primario boton-chico" type="submit" [disabled]="ocupado() || !textoRetro.trim()">Enviar retroalimentación</button></div>
            </form>
          }
        </section>
      }

      @if (notas().length) {
        <section class="cristal bloque">
          <h3>Notas oficiales</h3>
          <div class="tabla-envoltorio">
            <table class="tabla">
              <thead><tr><th>Estudiante (ID)</th><th>Sesión (ID)</th><th class="num">Intento</th><th class="num">Nota</th><th>Elegida</th></tr></thead>
              <tbody>
                @for (n of notas(); track n.id) {
                  <tr><td>{{ n.estudianteId }}</td><td>{{ n.practicaId }}</td><td class="num">{{ n.numeroIntento }}</td><td class="num"><strong>{{ n.notaOficial | number: '1.1-2' }}</strong></td><td>{{ n.fechaSeleccion | date: 'd MMM y, HH:mm' }}</td></tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      }
    </div>
  `,
  styles: `
    .resumen-estadistico { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 1rem; }
    .kpi { padding: 1.1rem 1.3rem; display: grid; gap: .2rem; } .kpi .valor { font-weight: 300; font-size: 2.2rem; line-height: 1; color: var(--lila-950); font-variant-numeric: tabular-nums; }
    :host-context([data-tema='noche']) .kpi .valor { color: #f1ecff; }
    .barra-tabla { display: flex; flex-wrap: wrap; gap: .8rem 1.2rem; align-items: flex-end; margin-bottom: .8rem; } .buscador { min-width: 240px; }
    .orden { all: unset; cursor: pointer; font-weight: 600; } .orden:focus-visible { outline: 3px solid #7a5fd0; border-radius: 4px; }
    tr.activa { background: rgba(139,111,224,.14); }
    .paginacion { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: .9rem; }
    .nav-calificar { display: flex; align-items: center; gap: .8rem; flex-wrap: wrap; } .nav-calificar h3 { margin: 0; }
    .plantillas { display: flex; gap: .8rem; align-items: flex-end; flex-wrap: wrap; }
    .mapa-envoltorio { overflow-x: auto; padding-bottom: .4rem; }
    .mapa { display: grid; gap: 3px; align-items: center; margin-top: .6rem; }
    .m-col { text-align: center; font-size: .72rem; color: var(--tinta-suave); } .m-fila { font-size: .82rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; padding-right: .5rem; }
    .m-cel { height: 26px; border-radius: 7px; background: rgba(120,110,150,.22); transition: transform .15s; animation: m-aparece .5s backwards; }
    .m-cel:hover { transform: scale(1.25); position: relative; z-index: 1; }
    .m-cel.ok { background: linear-gradient(145deg, #7fd3a6, #2f9d6b); } .m-cel.mal { background: linear-gradient(145deg, #f3a0a0, #d9534f); }
    @keyframes m-aparece { from { opacity: 0; transform: scale(.4); } }
    .dos { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 1.2rem; align-items: start; }
  `,
})
export class ResultadosDocente implements OnInit {
  private readonly api = inject(SomaApiService);
  private readonly dialogos = inject(DialogoService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly sesiones = inject(SesionService);
  protected readonly nombre = nombreEstado;

  protected readonly resultados = signal<ResultadoDetalle[]>([]);
  protected readonly notas = signal<NotaOficial[]>([]);
  protected readonly retros = signal<Retroalimentacion[]>([]);
  protected readonly seleccionado = signal<ResultadoDetalle | null>(null);
  protected readonly cargando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected practica: number | null = null;
  protected estudiante: number | null = null;
  protected textoRetro = '';

  protected readonly reportes = inject(ReportesService);
  private readonly toasts = inject(ToastService);
  protected readonly aciertos = signal<{ etiqueta: string; valor: number }[]>([]);
  protected readonly calculando = signal(false);
  protected readonly detalles = signal<ResultadoDetalle[]>([]);
  /** Matriz estudiante × pregunta con el estado de cada respuesta (hasta 40 intentos). */
  protected readonly mapa = computed(() => {
    const d = this.detalles(), cols = new Map<number, { id: number; n: number; texto: string }>();
    for (const r of d) for (const q of r.detallePreguntas) if (!cols.has(q.preguntaId)) cols.set(q.preguntaId, { id: q.preguntaId, n: cols.size + 1, texto: q.enunciado });
    const filas = d.map((r) => ({ nombre: r.estudianteNombre, intento: r.numeroIntento, celdas: Object.fromEntries(r.detallePreguntas.map((q) => [q.preguntaId, !q.respondida ? 'na' : q.correcta ? 'ok' : 'mal'])) as Record<number, string> }));
    return { columnas: [...cols.values()], filas };
  });
  protected etiquetaCelda(v: string | undefined): string { return v === 'ok' ? 'correcta' : v === 'mal' ? 'incorrecta' : 'sin responder'; }
  protected readonly proporciones = computed(() => this.resultados().map((r) => (r.escalaMaxima ? Number(r.notaFinal) / Number(r.escalaMaxima) : 0)));
  protected readonly estadisticas = computed(() => {
    const n = this.resultados().map((r) => Number(r.notaFinal));
    return { promedio: n.length ? n.reduce((a, b) => a + b, 0) / n.length : 0, maxima: n.length ? Math.max(...n) : 0, minima: n.length ? Math.min(...n) : 0 };
  });
  protected readonly busqueda = signal('');
  protected readonly orden = signal<{ col: 'estudiante' | 'intento' | 'correctas' | 'nota'; asc: boolean } | null>(null);
  protected readonly pagina = signal(1);
  private readonly tamano = 10;
  protected readonly filas = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    let f = this.resultados().filter((r) => !q || `${r.estudianteNombre} ${r.estudianteCorreo}`.toLowerCase().includes(q));
    const o = this.orden();
    if (o) {
      const v = (r: ResultadoDetalle): number | string => o.col === 'estudiante' ? r.estudianteNombre.toLowerCase() : o.col === 'intento' ? r.numeroIntento : o.col === 'correctas' ? r.correctas : Number(r.notaFinal);
      f = [...f].sort((a, b) => (v(a) < v(b) ? -1 : v(a) > v(b) ? 1 : 0) * (o.asc ? 1 : -1));
    }
    return f;
  });
  protected readonly paginas = computed(() => Math.max(1, Math.ceil(this.filas().length / this.tamano)));
  protected readonly visibles = computed(() => this.filas().slice((Math.min(this.pagina(), this.paginas()) - 1) * this.tamano, Math.min(this.pagina(), this.paginas()) * this.tamano));
  protected readonly indiceActual = computed(() => this.filas().findIndex((r) => r.id === this.seleccionado()?.id));

  protected buscarTexto(v: string): void { this.busqueda.set(v); this.pagina.set(1); }
  protected ordenar(col: 'estudiante' | 'intento' | 'correctas' | 'nota'): void {
    const o = this.orden(); this.orden.set(o?.col === col ? { col, asc: !o.asc } : { col, asc: true }); this.pagina.set(1);
  }
  protected aria(col: string): 'ascending' | 'descending' | 'none' { const o = this.orden(); return o?.col === col ? (o.asc ? 'ascending' : 'descending') : 'none'; }
  protected flecha(col: string): string { const o = this.orden(); return o?.col === col ? (o.asc ? '▲' : '▼') : ''; }

  /** Calificación rápida: pasa al estudiante anterior o siguiente sin volver a la tabla. */
  protected mover(delta: number): void {
    const i = this.indiceActual() + delta, f = this.filas();
    if (i < 0 || i >= f.length) return;
    this.cargarDetalle(f[i]); this.pagina.set(Math.floor(i / this.tamano) + 1);
  }
  @HostListener('document:keydown', ['$event'])
  protected teclas(e: KeyboardEvent): void {
    if (!this.seleccionado() || !e.altKey) return;
    if (e.key === 'ArrowRight') { e.preventDefault(); this.mover(1); } else if (e.key === 'ArrowLeft') { e.preventDefault(); this.mover(-1); }
  }

  protected exportarCsv(): void {
    const celda = (v: unknown): string => { const s = String(v ?? ''); return /[",\n;]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s; };
    const filas = [['Estudiante', 'Correo', 'Sesión', 'Intento', 'Correctas', 'Incorrectas', 'Sin responder', 'Nota', 'Escala', 'Oficial'],
      ...this.filas().map((r) => [r.estudianteNombre, r.estudianteCorreo, r.practicaNombre, r.numeroIntento, r.correctas, r.incorrectas, r.noRespondidas, r.notaFinal, r.escalaMaxima, r.oficial ? 'Sí' : 'No'])];
    guardarArchivo(new Blob(['\ufeff' + filas.map((f) => f.map(celda).join(';')).join('\r\n')], { type: 'text/csv;charset=utf-8' }), 'resultados-filtrados.csv');
    this.toasts.ok(`Exportadas ${this.filas().length} fila(s).`);
  }

  private static readonly BASE = ['Buen trabajo: tu análisis del caso fue claro y bien argumentado.', 'Revisa las preguntas incorrectas y vuelve a leer el contexto del caso; te servirá para la próxima sesión.', 'Tu resultado es el esperado para esta sesión. Si tienes dudas, escríbeme.'];
  protected readonly plantillas = signal<string[]>(this.leerPlantillas());
  private leerPlantillas(): string[] { try { return [...ResultadosDocente.BASE, ...JSON.parse(localStorage.getItem('soma.plantillas.retro') ?? '[]')]; } catch { return [...ResultadosDocente.BASE]; } }
  protected usarPlantilla(texto: string): void { if (texto) this.textoRetro = texto; }
  protected guardarPlantilla(): void {
    const texto = this.textoRetro.trim(); if (!texto || this.plantillas().includes(texto)) return;
    const propias = this.plantillas().filter((p) => !ResultadosDocente.BASE.includes(p)).concat(texto);
    try { localStorage.setItem('soma.plantillas.retro', JSON.stringify(propias)); } catch { /* sin almacenamiento */ }
    this.plantillas.set([...ResultadosDocente.BASE, ...propias]); this.toasts.ok('Plantilla guardada en este navegador.');
  }

  protected readonly puedeReportes = computed(() => this.sesiones.tienePermiso(Permiso.DescargarReportes));
  protected readonly puedeRetro = computed(() => this.sesiones.tienePermiso(Permiso.RegistrarRetroalimentacion));
  protected readonly puedeCalificar = computed(() => this.sesiones.tienePermiso(Permiso.ConsultarParticipaciones));

  ngOnInit(): void {
    const q = this.ruta.snapshot.queryParamMap;
    if (q.get('practica')) { this.practica = Number(q.get('practica')); }
    if (q.get('estudiante')) { this.estudiante = Number(q.get('estudiante')); }
    this.buscar();
  }

  protected buscar(): void {
    this.cargando.set(true); this.error.set(null); this.seleccionado.set(null);
    this.api.resultadosFiltrados(this.practica, this.estudiante).subscribe({
      next: (r) => { this.resultados.set(r); this.cargando.set(false); this.calcularAciertos(r); },
      error: (e: HttpErrorResponse) => { this.cargando.set(false); this.error.set(mensajeDeError(e)); },
    });
    this.api.notasOficiales(this.practica, this.estudiante).subscribe({ next: (n) => this.notas.set(n), error: () => this.notas.set([]) });
  }

  /** Porcentaje de intentos que acertó cada pregunta; lee el detalle de hasta 40 intentos. */
  private calcularAciertos(lista: ResultadoDetalle[]): void {
    this.aciertos.set([]); this.detalles.set([]);
    if (!lista.length) return;
    this.calculando.set(true);
    forkJoin(lista.slice(0, 40).map((r) => this.api.resultado(r.id))).subscribe({
      next: (detalles) => {
        this.detalles.set(detalles);
        const m = new Map<number, { e: string; ok: number; n: number }>();
        for (const d of detalles) for (const q of d.detallePreguntas) {
          const x = m.get(q.preguntaId) ?? { e: q.enunciado, ok: 0, n: 0 };
          x.n++; if (q.correcta) x.ok++; m.set(q.preguntaId, x);
        }
        this.aciertos.set([...m.values()].map((x, i) => ({ etiqueta: `${i + 1}. ${x.e}`, valor: x.n ? x.ok / x.n : 0 })));
        this.calculando.set(false);
      },
      error: () => this.calculando.set(false),
    });
  }

  protected abrir(r: ResultadoDetalle): void {
    if (this.seleccionado()?.id === r.id) { this.seleccionado.set(null); return; }
    this.cargarDetalle(r);
  }

  private cargarDetalle(r: ResultadoDetalle): void {
    this.error.set(null); this.textoRetro = '';
    this.api.resultado(r.id).subscribe({ next: (d) => this.seleccionado.set(d), error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)) });
    this.api.retroalimentaciones(r.practicaId, r.participacionId).subscribe({ next: (t) => this.retros.set(t), error: () => this.retros.set([]) });
  }

  private ejecutar<T>(op: Observable<T>, mensaje: (r: T) => string, tras?: () => void): void {
    this.error.set(null); this.aviso.set(null); this.ocupado.set(true);
    op.subscribe({
      next: (r) => { this.ocupado.set(false); this.aviso.set(mensaje(r)); this.toasts.ok(mensaje(r)); tras?.(); },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
    });
  }

  protected oficial(r: ResultadoDetalle): void {
    this.ejecutar(this.api.elegirNotaOficial(r.practicaId, r.estudianteId, r.participacionId), () => 'Nota oficial registrada.', () => this.buscar());
  }

  protected evaluar(): void {
    this.ejecutar(this.api.evaluarPendientes(this.practica!), (r) => `Intentos cerrados: ${r.incompletas} incompletos, ${r.ausencias} ausencias, ${r.omitidas} omitidos.`, () => this.buscar());
  }

  protected publicar(): void {
    void this.dialogos.confirmar({ titulo: 'Publicar calificaciones', mensaje: 'Se enviará un correo a cada estudiante con su nota oficial.', confirmar: 'Publicar' }).then((ok) => {
      if (!ok) return;
      this.ejecutar(this.api.publicarCalificaciones(this.practica!), (r) => `Calificaciones publicadas: ${r.enviadas} enviada(s), ${r.omitidasDuplicado} ya enviada(s), ${r.errores} con error.`);
    });
  }

  protected retroalimentar(r: ResultadoDetalle): void {
    this.ejecutar(this.api.registrarRetroalimentacion(r.participacionId, this.textoRetro.trim()), () => 'Retroalimentación enviada.', () => {
      this.textoRetro = '';
      this.api.retroalimentaciones(r.practicaId, r.participacionId).subscribe({ next: (t) => this.retros.set(t) });
    });
  }

  protected descargar(formato: 'pdf' | 'xlsx'): void {
    this.reportes.descargar(formato, this.practica, this.estudiante);
  }

}
