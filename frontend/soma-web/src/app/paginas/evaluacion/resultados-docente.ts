import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
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
  imports: [DatePipe, DecimalPipe, FormsModule],
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
              <button type="button" class="boton-suave" [disabled]="ocupado()" (click)="descargar('pdf')">Reporte PDF</button>
              <button type="button" class="boton-suave" [disabled]="ocupado()" (click)="descargar('xlsx')">Reporte Excel</button>
            }
          </div>
          <p class="ayuda">«Cerrar intentos pendientes» evalúa los intentos incompletos o sin responder. «Publicar calificaciones» envía la nota oficial a cada estudiante por correo.</p>
        }
      </section>

      @if (cargando()) { <p class="suave">Cargando…</p> }
      @else if (resultados().length === 0) { <p class="cristal vacio">No hay resultados todavía para esa búsqueda.</p> }
      @else {
        <section class="cristal bloque">
          <div class="tabla-envoltorio">
            <table class="tabla">
              <thead><tr><th>Estudiante</th><th>Sesión</th><th class="num">Intento</th><th class="num">Correctas</th><th class="num">Incorrectas</th><th class="num">Sin responder</th><th class="num">Nota</th><th></th></tr></thead>
              <tbody>
                @for (r of resultados(); track r.id) {
                  <tr>
                    <td><strong>{{ r.estudianteNombre }}</strong><br /><span class="ayuda">{{ r.estudianteCorreo }}</span></td>
                    <td>{{ r.practicaNombre }}</td>
                    <td class="num">{{ r.numeroIntento }}</td>
                    <td class="num">{{ r.correctas }}</td><td class="num">{{ r.incorrectas }}</td><td class="num">{{ r.noRespondidas }}</td>
                    <td class="num"><strong>{{ r.notaFinal | number: '1.1-2' }}</strong> / {{ r.escalaMaxima }}
                      @if (r.oficial) { <br /><span class="etiqueta ok">Oficial</span> }</td>
                    <td><button type="button" class="boton-suave boton-chico" (click)="abrir(r)">{{ seleccionado()?.id === r.id ? 'Cerrar' : 'Detalle' }}</button></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      }

      @if (seleccionado(); as r) {
        <section class="cristal bloque">
          <h3>{{ r.estudianteNombre }} · intento {{ r.numeroIntento }}</h3>
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
})
export class ResultadosDocente implements OnInit {
  private readonly api = inject(SomaApiService);
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
      next: (r) => { this.resultados.set(r); this.cargando.set(false); },
      error: (e: HttpErrorResponse) => { this.cargando.set(false); this.error.set(mensajeDeError(e)); },
    });
    this.api.notasOficiales(this.practica, this.estudiante).subscribe({ next: (n) => this.notas.set(n), error: () => this.notas.set([]) });
  }

  protected abrir(r: ResultadoDetalle): void {
    if (this.seleccionado()?.id === r.id) { this.seleccionado.set(null); return; }
    this.error.set(null); this.textoRetro = '';
    this.api.resultado(r.id).subscribe({ next: (d) => this.seleccionado.set(d), error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)) });
    this.api.retroalimentaciones(r.practicaId, r.participacionId).subscribe({ next: (t) => this.retros.set(t), error: () => this.retros.set([]) });
  }

  private ejecutar<T>(op: Observable<T>, mensaje: (r: T) => string, tras?: () => void): void {
    this.error.set(null); this.aviso.set(null); this.ocupado.set(true);
    op.subscribe({
      next: (r) => { this.ocupado.set(false); this.aviso.set(mensaje(r)); tras?.(); },
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
    if (!confirm('¿Publicar las calificaciones? Se enviará un correo a cada estudiante.')) { return; }
    this.ejecutar(this.api.publicarCalificaciones(this.practica!), (r) => `Calificaciones publicadas: ${r.enviadas} enviada(s), ${r.omitidasDuplicado} ya enviada(s), ${r.errores} con error.`);
  }

  protected retroalimentar(r: ResultadoDetalle): void {
    this.ejecutar(this.api.registrarRetroalimentacion(r.participacionId, this.textoRetro.trim()), () => 'Retroalimentación enviada.', () => {
      this.textoRetro = '';
      this.api.retroalimentaciones(r.practicaId, r.participacionId).subscribe({ next: (t) => this.retros.set(t) });
    });
  }

  protected descargar(formato: 'pdf' | 'xlsx'): void {
    this.error.set(null); this.ocupado.set(true);
    this.api.reporte(formato, this.practica, this.estudiante).subscribe({
      next: (b) => { this.ocupado.set(false); guardarArchivo(b, `evaluacion.${formato}`); },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(e.status === 403 ? 'No tienes permiso para descargar reportes.' : 'No se pudo generar el reporte.'); },
    });
  }
}
