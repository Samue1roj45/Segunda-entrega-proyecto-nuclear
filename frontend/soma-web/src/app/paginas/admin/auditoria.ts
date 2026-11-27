import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mensajeDeError } from '../../core/con-operaciones';
import { DialogoService } from '../../core/dialogo.service';
import { EntradaAuditoria, VistaPreviaLimpieza } from '../../core/modelos/dominio';
import { SomaApiService } from '../../core/soma-api.service';
import { ToastService } from '../../core/toast.service';

const NOMBRES: Record<string, string> = {
  INGRESO: 'Ingreso', CUENTA_BLOQUEADA: 'Cuenta bloqueada', CASO_ELIMINADO: 'Caso eliminado', CASO_RESTAURADO: 'Caso restaurado', CASO_BORRADO_DEFINITIVO: 'Caso borrado para siempre',
  DOCENTE_CREADO: 'Docente creado', ESTUDIANTE_CREADO: 'Estudiante creado', PERMISOS_ACTUALIZADOS: 'Permisos actualizados',
  USUARIO_ARCHIVADO: 'Cuenta archivada', USUARIO_REACTIVADO: 'Cuenta reactivada', USUARIO_ELIMINADO: 'Cuenta eliminada', USUARIO_ELIMINACION_BLOQUEADA: 'Eliminación de cuenta bloqueada',
  CASO_ARCHIVADO: 'Caso archivado', CASO_RECUPERADO: 'Caso recuperado',
  AUDITORIA_LIMPIADA: 'Historial limpiado', AUDITORIA_LIMPIEZA_FALLIDA: 'Limpieza fallida',
};

/** «2026-11-01» → «1 de noviembre de 2026», sin desfase de zona horaria. */
function fechaLarga(iso: string): string {
  const [a, m, d] = iso.split('-').map(Number);
  return new Date(a, m - 1, d).toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' });
}

function hoyIso(): string {
  const h = new Date();
  return `${h.getFullYear()}-${String(h.getMonth() + 1).padStart(2, '0')}-${String(h.getDate()).padStart(2, '0')}`;
}

/**
 * Registro de auditoría: qué pasó, quién lo hizo y cuándo, con filtro por acción y «Cargar más». Incluye la
 * limpieza por fecha de corte: vista previa de cuántos registros se borran, confirmación y recarga. Solo el administrador.
 */
@Component({
  selector: 'app-auditoria',
  imports: [DatePipe, FormsModule],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2 class="sr-only">Auditoría</h2></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      <section class="cristal bloque">
        <div class="filtros">
          <label class="campo-f">Acción
            <select [ngModel]="filtro()" (ngModelChange)="cambiarFiltro($event)" name="filtro">
              <option value="">Todas</option>
              @for (a of acciones(); track a) { <option [value]="a">{{ nombre(a) }}</option> }
            </select>
          </label>
          <span class="suave">{{ entradas().length }} registros mostrados</span>
        </div>
      </section>
      <section class="cristal bloque limpieza" aria-labelledby="titulo-limpieza">
        <h3 id="titulo-limpieza">Limpiar historial de auditoría</h3>
        <p class="ayuda">Elimina los registros anteriores a la fecha que elijas. Los registros de esa fecha en adelante se conservan, y la auditoría sigue registrando con normalidad. La propia limpieza queda registrada con tu nombre, la fecha de corte y la cantidad eliminada.</p>
        <div class="filtros">
          <label class="campo-f">Eliminar registros anteriores al
            <input type="date" name="corte" [max]="hoy" [ngModel]="corte()" (ngModelChange)="cambiarCorte($event)" />
          </label>
          <button type="button" class="boton-suave" id="limpieza-calcular" [disabled]="!corte() || calculando()" [attr.aria-busy]="calculando()" (click)="calcular()">Calcular</button>
        </div>
        @if (vistaPrevia(); as v) {
          <p class="aviso-info" role="status" id="limpieza-resumen">
            @if (v.registros === 0) { No hay registros anteriores al {{ fecha(v.antesDe) }}. No hay nada que limpiar. }
            @else { Se eliminarán <strong>{{ v.registros }}</strong> {{ v.registros === 1 ? 'registro anterior' : 'registros anteriores' }} al {{ fecha(v.antesDe) }}. Los posteriores se conservan. }
          </p>
          @if (v.registros > 0) {
            <div class="acciones"><button type="button" class="boton-peligro" id="limpieza-ejecutar" [disabled]="limpiando()" [attr.aria-busy]="limpiando()" (click)="limpiar(v.antesDe, v.registros)">Limpiar historial</button></div>
          }
        }
      </section>
      @if (cargando()) { <div class="esqueleto" role="status" aria-label="Cargando auditoría"><i></i><i></i><i></i></div> }
      @else if (visibles().length === 0) { <p class="cristal vacio">No hay registros{{ filtro() ? ' con este filtro' : ' todavía' }}.</p> }
      @else {
        <section class="cristal bloque">
          <ul class="lista-simple lista-auditoria">
            @for (e of visibles(); track e.id) {
              <li class="fila-item">
                <span class="quien"><strong>{{ e.detalle }}</strong><small>{{ e.actorCorreo || 'Sistema' }} · {{ e.fecha | date: 'd MMM y, h:mm a' }}</small></span>
                <span class="etiqueta" [class.mal]="e.accion === 'CUENTA_BLOQUEADA'">{{ nombre(e.accion) }}</span>
              </li>
            }
          </ul>
          @if (hayMas()) { <button type="button" class="boton-suave boton-chico" id="auditoria-mas" (click)="masRegistros()">Cargar más</button> }
        </section>
      }
    </div>
  `,
})
export class Auditoria implements OnInit {
  private readonly api = inject(SomaApiService);
  private static readonly TAMANO = 50;
  protected readonly entradas = signal<EntradaAuditoria[]>([]);
  protected readonly cargando = signal(true);
  protected readonly hayMas = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly filtro = signal('');
  protected readonly acciones = signal<string[]>([]);
  protected readonly visibles = computed(() => this.entradas());
  protected nombre(a: string): string { return NOMBRES[a] ?? a; }
  private readonly dialogos = inject(DialogoService);
  private readonly toasts = inject(ToastService);
  protected readonly hoy = hoyIso();
  protected readonly corte = signal('');
  protected readonly vistaPrevia = signal<VistaPreviaLimpieza | null>(null);
  protected readonly calculando = signal(false);
  protected readonly limpiando = signal(false);
  protected readonly fecha = fechaLarga;

  ngOnInit(): void {
    this.cargarAcciones();
    this.cargar(true);
  }

  private cargarAcciones(): void {
    this.api.accionesAuditadas().subscribe({ next: (a) => this.acciones.set(a), error: () => this.acciones.set([]) });
  }

  /** Cambiar la fecha invalida la cifra anterior: hay que volver a calcular antes de limpiar. */
  protected cambiarCorte(valor: string): void { this.corte.set(valor ?? ''); this.vistaPrevia.set(null); }

  protected calcular(): void {
    const corte = this.corte();
    if (!corte) { return; }
    this.calculando.set(true);
    this.api.vistaPreviaLimpiezaAuditoria(corte).subscribe({
      next: (v) => { this.calculando.set(false); this.error.set(null); this.vistaPrevia.set(v); },
      error: (e: HttpErrorResponse) => { this.calculando.set(false); this.vistaPrevia.set(null); this.error.set(mensajeDeError(e)); },
    });
  }

  protected limpiar(antesDe: string, registros: number): void {
    void this.dialogos.confirmar({
      titulo: 'Limpiar historial de auditoría',
      mensaje: `Se eliminarán los registros anteriores al ${fechaLarga(antesDe)}. Esta operación no podrá deshacerse.`,
      detalle: [`Registros que se eliminarán según el último cálculo: ${registros}.`, 'Se conservan los registros de esa fecha en adelante.', 'La limpieza quedará registrada con tu nombre, la fecha de corte y la cantidad eliminada.'],
      confirmar: 'Limpiar historial',
      peligro: true,
    }).then((ok) => {
      if (!ok) { return; }
      this.limpiando.set(true);
      this.api.limpiarAuditoria(antesDe).subscribe({
        next: (r) => {
          this.limpiando.set(false);
          this.vistaPrevia.set(null);
          this.toasts.ok(`Historial limpiado: se eliminaron ${r.eliminados} registros anteriores al ${fechaLarga(r.antesDe)}.`);
          this.cargarAcciones();
          this.cargar(true);
        },
        error: (e: HttpErrorResponse) => { this.limpiando.set(false); this.error.set(mensajeDeError(e)); },
      });
    });
  }

  protected cambiarFiltro(valor: string): void { this.filtro.set(valor); this.cargar(true); }

  protected masRegistros(): void { this.cargar(false); }

  private cargar(reiniciar: boolean): void {
    if (reiniciar) { this.cargando.set(true); }
    const desde = reiniciar ? 0 : this.entradas().length;
    this.api.auditoria(Auditoria.TAMANO, desde, this.filtro()).subscribe({
      next: (l) => {
        this.entradas.set(reiniciar ? l : [...this.entradas(), ...l]);
        this.hayMas.set(l.length === Auditoria.TAMANO);
        this.cargando.set(false);
      },
      error: (e: HttpErrorResponse) => { this.error.set(mensajeDeError(e)); this.cargando.set(false); },
    });
  }
}
