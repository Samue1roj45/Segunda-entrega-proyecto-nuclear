import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { claseEstado, nombreEstado } from '../../compartido/formato';
import { DialogoService } from '../../core/dialogo.service';
import { mensajeDeError } from '../../core/con-operaciones';
import { Permiso } from '../../core/modelos/permiso';
import { ReportesService } from '../../core/reportes.service';
import { SesionService } from '../../core/sesion.service';
import { SesionDetalle } from '../../core/modelos/dominio';
import { SomaApiService } from '../../core/soma-api.service';

/** Módulo 3: sesiones programadas del docente, con inicio y cierre según el estado. */
@Component({
  selector: 'app-sesiones-lista',
  imports: [DatePipe, RouterLink],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina">
        <h2 class="sr-only">Mis sesiones</h2>
        <a routerLink="/sesiones/calendario" class="boton-suave enlace-boton">Ver calendario</a>
        <a routerLink="/sesiones/nueva" class="boton-primario enlace-boton">Programar sesión</a>
      </div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (aviso(); as a) { <p class="aviso-ok" role="status">{{ a }}</p> }
      @if (cargando()) { <div class="esqueleto" role="status" aria-label="Cargando sesiones"><i></i><i></i><i></i></div> }
      @else if (sesiones().length === 0) { <p class="cristal vacio">Aún no has programado sesiones. Elige un caso publicado y convoca a tus estudiantes.</p> }
      @else {
        <div class="tarjetas">
          @for (s of sesiones(); track s.idPractica) {
            <article class="cristal tarjeta-item">
              <span [class]="'etiqueta ' + clase(s.estado)">{{ nombre(s.estado) }}</span>
              <h3><a [routerLink]="['/sesiones', s.idPractica]">{{ s.nombreCaso }}</a></h3>
              <p class="suave">{{ s.espacioAcademico }}</p>
              <div class="meta">
                <span>{{ s.fechaInicio | date: 'd MMM y, HH:mm' }} → {{ s.fechaFin | date: 'd MMM, HH:mm' }}</span>
                <span>{{ s.tiempoMaximoMinutos }} min</span>
                @if (s.lugar) { <span>{{ s.lugar }}</span> }
              </div>
              <div class="acciones">
                <a [routerLink]="['/sesiones', s.idPractica]" class="boton-suave boton-chico enlace-boton">Abrir</a>
                @if (s.accionesPermitidas.includes('INICIAR')) { <button type="button" class="boton-primario boton-chico" [disabled]="ocupado()" (click)="iniciar(s)">Iniciar</button> }
                @if (s.accionesPermitidas.includes('FINALIZAR')) { <button type="button" class="boton-peligro boton-chico" [disabled]="ocupado()" (click)="finalizar(s)">Finalizar</button> }
                @if (puedeReportes()) {
                  <button type="button" class="boton-suave boton-chico" [disabled]="reportes.generando('pdf', s.idPractica)" (click)="reportes.descargar('pdf', s.idPractica)" title="Descargar reporte PDF">{{ reportes.generando('pdf', s.idPractica) ? 'Generando…' : 'PDF' }}</button>
                  <button type="button" class="boton-suave boton-chico" [disabled]="reportes.generando('xlsx', s.idPractica)" (click)="reportes.descargar('xlsx', s.idPractica)" title="Descargar reporte Excel">{{ reportes.generando('xlsx', s.idPractica) ? 'Generando…' : 'Excel' }}</button>
                }
              </div>
            </article>
          }
        </div>
      }
    </div>
  `,
})
export class SesionesLista implements OnInit {
  private readonly api = inject(SomaApiService);
  private readonly dialogos = inject(DialogoService);
  protected readonly reportes = inject(ReportesService);
  private readonly sesionUsuario = inject(SesionService);
  protected readonly puedeReportes = computed(() => this.sesionUsuario.tienePermiso(Permiso.DescargarReportes));
  protected readonly sesiones = signal<SesionDetalle[]>([]);
  protected readonly cargando = signal(true);
  protected readonly ocupado = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly nombre = nombreEstado;
  protected readonly clase = claseEstado;

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    this.cargando.set(true);
    this.api.sesionesDetalle().subscribe({
      next: (s) => { this.sesiones.set(s); this.cargando.set(false); },
      error: (e: HttpErrorResponse) => { this.error.set(mensajeDeError(e)); this.cargando.set(false); },
    });
  }

  protected iniciar(s: SesionDetalle): void { this.mover(s, true); }
  protected finalizar(s: SesionDetalle): void {
    void this.dialogos.confirmar({ titulo: 'Finalizar la sesión', mensaje: `«${s.nombreCaso}»: los estudiantes que estén en curso quedarán cerrados.`, confirmar: 'Finalizar', peligro: true }).then((ok) => { if (ok) this.mover(s, false); });
  }

  private mover(s: SesionDetalle, iniciar: boolean): void {
    this.ocupado.set(true); this.error.set(null);
    (iniciar ? this.api.iniciarSesion(s.idPractica) : this.api.finalizarSesion(s.idPractica)).subscribe({
      next: () => { this.ocupado.set(false); this.aviso.set(iniciar ? 'Sesión iniciada.' : 'Sesión finalizada.'); this.cargar(); },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
    });
  }
}
