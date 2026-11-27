import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mensajeDeError } from '../../core/con-operaciones';
import { DialogoService } from '../../core/dialogo.service';
import { EcosService, EstadoJuego, EstudianteDocente } from '../../core/ecos.service';
import { InvitacionDocente, InvitacionesService, ResultadoInvitacion } from '../../core/invitaciones.service';
import { ToastService } from '../../core/toast.service';

const ESTADOS: Record<EstadoJuego, { texto: string; clase: string }> = {
  SIN_JUGAR: { texto: 'Sin jugar', clase: 'sin-iniciar' },
  CODIGO_GENERADO: { texto: 'Código generado', clase: 'borrador' },
  JUGANDO: { texto: 'Jugando', clase: 'en-curso' },
  TERMINADO: { texto: 'Terminó', clase: 'ok' },
};

/** "Mis estudiantes": invitar por correo, ver quién aceptó, su avance en Ecos, dar otro intento o quitarlos de la lista. */
@Component({
  selector: 'app-mis-estudiantes',
  imports: [DatePipe, DecimalPipe, FormsModule],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2 class="sr-only">Mis estudiantes</h2>
        <p class="suave">Invítalos por correo: al abrir el enlace crean su cuenta con ese mismo correo y quedan en tu lista.</p></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }

      <section class="cristal bloque">
        <h3>Invitar estudiantes</h3>
        <label class="campo-f">Correos institucionales (uno por línea, o separados por coma o espacio)
          <textarea rows="3" [(ngModel)]="texto" name="correos" placeholder="ana.perez@institucion.edu.co" spellcheck="false"></textarea>
        </label>
        <div class="acciones">
          <button type="button" class="boton-primario" [disabled]="enviando() || !texto.trim()" (click)="invitar()">{{ enviando() ? 'Enviando…' : 'Enviar invitaciones' }}</button>
        </div>
        @if (resultados().length) {
          <ul class="lista-simple">
            @for (r of resultados(); track r.correo) {
              <li class="fila-item">
                <span class="texto"><strong>{{ r.correo }}</strong><br /><span class="ayuda">{{ r.mensaje }}</span></span>
                <span [class]="'etiqueta ' + (esOk(r) ? 'ok' : 'mal')">{{ nombreResultado(r.resultado) }}</span>
                @if (r.enlace) { <button type="button" class="boton-suave boton-chico" (click)="copiar(r.enlace)">Copiar enlace</button> }
              </li>
            }
          </ul>
          <p class="ayuda">Si un correo no llega, copia el enlace y envíaselo tú. Es personal: solo sirve para ese correo y vale 7 días.</p>
        }
      </section>

      <section class="cristal bloque">
        <h3>Estudiantes</h3>
        @if (estudiantes() === null) { <div class="esqueleto" role="status" aria-label="Cargando"><i></i><i></i></div> }
        @else if (estudiantes()!.length === 0) { <p class="vacio">Todavía no tienes estudiantes. Invita a los primeros arriba.</p> }
        @else {
          <div class="tabla-envoltura"><table class="tabla">
            <thead><tr><th>Estudiante</th><th>Ecos</th><th>Intentos</th><th>Mejor nota</th><th></th></tr></thead>
            <tbody>
              @for (e of estudiantes(); track e.id) {
                <tr>
                  <td><strong>{{ e.nombre }}</strong><br /><span class="ayuda">{{ e.correo }}</span></td>
                  <td><span [class]="'etiqueta ' + estado(e).clase">{{ estado(e).texto }}</span></td>
                  <td>{{ e.intentosUsados }} de {{ e.intentosMaximos }} @if (e.repeticiones) { <span class="ayuda">(+{{ e.repeticiones }})</span> }</td>
                  <td>@if (e.mejorNota !== null) { <strong>{{ e.mejorNota | number: '1.1-1' }}</strong> <span class="ayuda">{{ e.nivel }}</span> } @else { — }</td>
                  <td class="acciones-fila">
                    <button type="button" class="boton-suave boton-chico" (click)="darIntento(e)">Dar otro intento</button>
                    <button type="button" class="boton-suave boton-chico" (click)="quitar(e)">Quitar de mi lista</button>
                  </td>
                </tr>
              }
            </tbody>
          </table></div>
        }
      </section>

      <section class="cristal bloque">
        <h3>Invitaciones</h3>
        @if (invitaciones() === null) { <div class="esqueleto" role="status" aria-label="Cargando"><i></i></div> }
        @else if (invitaciones()!.length === 0) { <p class="vacio">Aún no has enviado invitaciones.</p> }
        @else {
          <div class="tabla-envoltura"><table class="tabla">
            <thead><tr><th>Correo</th><th>Estado</th><th>Enviada</th><th>Vence</th><th></th></tr></thead>
            <tbody>
              @for (i of invitaciones(); track i.id) {
                <tr>
                  <td>{{ i.correo }}@if (i.estudiante) { <br /><span class="ayuda">{{ i.estudiante }}</span> }</td>
                  <td><span [class]="'etiqueta ' + claseInvitacion(i.estado)">{{ nombreInvitacion(i.estado) }}</span></td>
                  <td>{{ i.creada | date: 'd MMM y, HH:mm' }}</td>
                  <td>{{ i.vence | date: 'd MMM y' }}</td>
                  <td class="acciones-fila">
                    @if (i.estado === 'PENDIENTE' || i.estado === 'VENCIDA') {
                      <button type="button" class="boton-suave boton-chico" (click)="reenviar(i)">{{ i.estado === 'VENCIDA' ? 'Enviar de nuevo' : 'Reenviar' }}</button>
                    }
                    @if (i.estado === 'PENDIENTE') { <button type="button" class="boton-suave boton-chico" (click)="cancelar(i)">Cancelar</button> }
                  </td>
                </tr>
              }
            </tbody>
          </table></div>
        }
      </section>
    </div>
  `,
  styles: `.tabla-envoltura { overflow-x: auto; } .acciones-fila { display: flex; gap: .4rem; flex-wrap: wrap; }`,
})
export class MisEstudiantes implements OnInit {
  private readonly ecos = inject(EcosService);
  private readonly api = inject(InvitacionesService);
  private readonly dialogos = inject(DialogoService);
  private readonly toasts = inject(ToastService);

  protected texto = '';
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly resultados = signal<ResultadoInvitacion[]>([]);
  protected readonly estudiantes = signal<EstudianteDocente[] | null>(null);
  protected readonly invitaciones = signal<InvitacionDocente[] | null>(null);

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    this.ecos.misEstudiantes().subscribe({ next: (l) => this.estudiantes.set(l), error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)) });
    this.api.listar().subscribe({ next: (l) => this.invitaciones.set(l), error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)) });
  }

  protected invitar(): void {
    const correos = this.texto.split(/[\s,;]+/).map((c) => c.trim()).filter(Boolean);
    if (!correos.length) { return; }
    this.enviando.set(true);
    this.error.set(null);
    this.api.invitar(correos).subscribe({
      next: (r) => { this.resultados.set(r); this.enviando.set(false); this.texto = ''; this.cargar(); },
      error: (e: HttpErrorResponse) => { this.error.set(mensajeDeError(e)); this.enviando.set(false); },
    });
  }

  protected esOk(r: ResultadoInvitacion): boolean { return ['ENVIADA', 'REENVIADA', 'VINCULADO'].includes(r.resultado); }
  protected nombreResultado(r: string): string {
    return ({ ENVIADA: 'Enviada', REENVIADA: 'Reenviada', VINCULADO: 'Agregado', CORREO_INVALIDO: 'Correo inválido', DOMINIO_INVALIDO: 'Dominio no permitido', NO_ES_ESTUDIANTE: 'No es estudiante' } as Record<string, string>)[r] ?? r;
  }
  protected estado(e: EstudianteDocente): { texto: string; clase: string } { return ESTADOS[e.estadoActual]; }
  protected nombreInvitacion(e: string): string { return ({ PENDIENTE: 'Pendiente', ACEPTADA: 'Aceptada', CANCELADA: 'Cancelada', VENCIDA: 'Vencida' } as Record<string, string>)[e] ?? e; }
  protected claseInvitacion(e: string): string { return ({ PENDIENTE: 'borrador', ACEPTADA: 'ok', CANCELADA: 'archivado', VENCIDA: 'mal' } as Record<string, string>)[e] ?? ''; }

  protected copiar(enlace: string): void {
    void navigator.clipboard?.writeText(enlace).then(() => this.toasts.ok('Enlace copiado'), () => this.toasts.error('No se pudo copiar'));
  }

  protected reenviar(i: InvitacionDocente): void {
    this.api.reenviar(i.id).subscribe({
      next: (r) => { this.resultados.set([r]); this.toasts.ok(r.mensaje); this.cargar(); },
      error: (e: HttpErrorResponse) => this.toasts.error(mensajeDeError(e)),
    });
  }

  protected cancelar(i: InvitacionDocente): void {
    void this.dialogos.confirmar({ titulo: 'Cancelar invitación', mensaje: `El enlace enviado a ${i.correo} dejará de funcionar.`, confirmar: 'Cancelar invitación', cancelar: 'Volver', peligro: true })
      .then((ok) => { if (ok) { this.api.cancelar(i.id).subscribe({ next: () => { this.toasts.ok('Invitación cancelada'); this.cargar(); }, error: (e: HttpErrorResponse) => this.toasts.error(mensajeDeError(e)) }); } });
  }

  protected darIntento(e: EstudianteDocente): void {
    void this.dialogos.confirmar({ titulo: 'Dar otro intento', mensaje: `${e.nombre} podrá jugar Ecos una vez más.`, detalle: [`Intentos ahora: ${e.intentosMaximos}`, `Después: ${e.intentosMaximos + 1}`], confirmar: 'Dar intento', cancelar: 'Volver' })
      .then((ok) => { if (ok) { this.ecos.concederRepeticion(e.id).subscribe({ next: () => { this.toasts.ok('Intento adicional concedido'); this.cargar(); }, error: (x: HttpErrorResponse) => this.toasts.error(mensajeDeError(x)) }); } });
  }

  protected quitar(e: EstudianteDocente): void {
    void this.dialogos.confirmar({ titulo: 'Quitar de mi lista', mensaje: `${e.nombre} saldrá de tu lista de estudiantes.`, detalle: ['Su cuenta y sus resultados NO se borran: solo deja de aparecer aquí.', 'Puedes invitarlo de nuevo cuando quieras.'], confirmar: 'Quitar', cancelar: 'Volver', peligro: true })
      .then((ok) => { if (ok) { this.ecos.retirarEstudiante(e.id).subscribe({ next: () => { this.toasts.ok('Estudiante quitado de tu lista'); this.cargar(); }, error: (x: HttpErrorResponse) => this.toasts.error(mensajeDeError(x)) }); } });
  }
}
