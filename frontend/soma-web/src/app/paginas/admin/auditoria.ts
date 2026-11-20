import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mensajeDeError } from '../../core/con-operaciones';
import { EntradaAuditoria } from '../../core/modelos/dominio';
import { SomaApiService } from '../../core/soma-api.service';

const NOMBRES: Record<string, string> = {
  INGRESO: 'Ingreso', CUENTA_BLOQUEADA: 'Cuenta bloqueada', CASO_ELIMINADO: 'Caso eliminado',
  DOCENTE_CREADO: 'Docente creado', PERMISOS_ACTUALIZADOS: 'Permisos actualizados',
};

/** Registro de auditoría: qué pasó, quién lo hizo y cuándo. Solo el administrador. */
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
            <select [ngModel]="filtro()" (ngModelChange)="filtro.set($event)" name="filtro">
              <option value="">Todas</option>
              @for (a of acciones(); track a) { <option [value]="a">{{ nombre(a) }}</option> }
            </select>
          </label>
          <span class="suave">{{ visibles().length }} de {{ entradas().length }} registros recientes</span>
        </div>
      </section>
      @if (cargando()) { <div class="esqueleto" role="status" aria-label="Cargando auditoría"><i></i><i></i><i></i></div> }
      @else if (visibles().length === 0) { <p class="cristal vacio">No hay registros{{ filtro() ? ' con este filtro' : ' todavía' }}.</p> }
      @else {
        <section class="cristal bloque">
          <ul class="lista">
            @for (e of visibles(); track e.id) {
              <li>
                <div class="principal"><strong>{{ e.detalle }}</strong><span class="suave">{{ e.actorCorreo || 'Sistema' }} · {{ e.fecha | date: 'd MMM y, h:mm a' }}</span></div>
                <span class="etiqueta" [class.mal]="e.accion === 'CUENTA_BLOQUEADA'">{{ nombre(e.accion) }}</span>
              </li>
            }
          </ul>
        </section>
      }
    </div>
  `,
})
export class Auditoria implements OnInit {
  private readonly api = inject(SomaApiService);
  protected readonly entradas = signal<EntradaAuditoria[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly filtro = signal('');
  protected readonly acciones = computed(() => [...new Set(this.entradas().map((e) => e.accion))].sort());
  protected readonly visibles = computed(() => this.entradas().filter((e) => !this.filtro() || e.accion === this.filtro()));
  protected nombre(a: string): string { return NOMBRES[a] ?? a; }

  ngOnInit(): void {
    this.api.auditoria(200).subscribe({
      next: (l) => { this.entradas.set(l); this.cargando.set(false); },
      error: (e: HttpErrorResponse) => { this.error.set(mensajeDeError(e)); this.cargando.set(false); },
    });
  }
}
