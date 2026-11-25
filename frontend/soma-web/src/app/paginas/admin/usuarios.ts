import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { mensajeDeError } from '../../core/con-operaciones';
import { DialogoService } from '../../core/dialogo.service';
import { UsuarioAdmin } from '../../core/modelos/dominio';
import { SesionService } from '../../core/sesion.service';
import { SomaApiService } from '../../core/soma-api.service';
import { ToastService } from '../../core/toast.service';

const ROLES: Record<UsuarioAdmin['rol'], string> = { ESTUDIANTE: 'Estudiante', DOCENTE: 'Docente', ADMINISTRADOR: 'Administrador' };

/**
 * Administración de cuentas: activas y archivadas, con acciones según el estado. Las reglas (cuenta propia,
 * último administrador, historial que impide borrar) las aplica el servidor; esta pantalla solo pregunta,
 * muestra la respuesta y vuelve a cargar la lista desde el servidor después de cada cambio.
 */
@Component({
  selector: 'app-usuarios',
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2 class="sr-only">Usuarios</h2></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }

      <section class="cristal bloque">
        <div class="pestanas" role="tablist" aria-label="Estado de las cuentas">
          <button type="button" role="tab" id="pestana-activos" [class.activa]="activos()" [attr.aria-selected]="activos()" (click)="verActivos(true)">Activos</button>
          <button type="button" role="tab" id="pestana-archivados" [class.activa]="!activos()" [attr.aria-selected]="!activos()" (click)="verActivos(false)">Archivados</button>
        </div>
        <p class="ayuda">{{ activos()
          ? 'Cuentas que pueden iniciar sesión. Archivar quita el acceso y conserva todo el historial; eliminar solo es posible si la cuenta no tiene registros académicos ni comunicaciones.'
          : 'Cuentas sin acceso. Su historial (casos, sesiones, participaciones, notas) se conserva y se puede consultar. Puedes reactivarlas o, si no tienen historial, eliminarlas.' }}</p>
        <div class="filtros">
          <label class="campo-f">Rol
            <select name="rol" [ngModel]="rol()" (ngModelChange)="cambiarRol($event)">
              <option value="">Todos</option><option value="DOCENTE">Docentes</option>
              <option value="ESTUDIANTE">Estudiantes</option><option value="ADMINISTRADOR">Administradores</option>
            </select>
          </label>
          <label class="campo-f">Buscar
            <input type="search" name="buscar" placeholder="Nombre o correo" [ngModel]="busqueda()" (ngModelChange)="busqueda.set($event)" />
          </label>
          <span class="suave">{{ visibles().length }} {{ visibles().length === 1 ? 'cuenta' : 'cuentas' }}</span>
        </div>
        <p class="ayuda">Los permisos de cada docente activo se asignan en <a routerLink="/docentes">Docentes</a>.</p>
      </section>

      @if (cargando()) { <div class="esqueleto" role="status" aria-label="Cargando usuarios"><i></i><i></i><i></i></div> }
      @else if (visibles().length === 0) {
        <p class="cristal vacio">{{ activos() ? 'No hay cuentas activas' : 'No hay cuentas archivadas' }}{{ rol() || busqueda() ? ' con este filtro' : '' }}.</p>
      } @else {
        <section class="cristal bloque">
          <ul class="lista-simple" [attr.aria-label]="activos() ? 'Cuentas activas' : 'Cuentas archivadas'">
            @for (u of visibles(); track u.id) {
              <li class="fila-item fila-persona">
                <span class="quien"><strong>{{ u.nombreCompleto }}</strong><small>{{ u.correoInstitucional }}</small></span>
                <span class="estado-persona">
                  <span class="etiqueta">{{ rolTexto(u.rol) }}</span>
                  <span class="ayuda">Desde {{ u.fechaCreacion | date: 'd MMM y' }}</span>
                </span>
                <span class="acciones">
                  @if (esPropia(u)) {
                    <span class="ayuda">Tu cuenta</span>
                  } @else if (u.activo) {
                    <button type="button" class="boton-suave boton-chico" [disabled]="trabajando() === u.id" [attr.aria-busy]="trabajando() === u.id" (click)="archivar(u)">Archivar</button>
                    <button type="button" class="boton-peligro boton-chico" [disabled]="trabajando() === u.id" (click)="eliminar(u)">Eliminar</button>
                  } @else {
                    <button type="button" class="boton-primario boton-chico" [disabled]="trabajando() === u.id" [attr.aria-busy]="trabajando() === u.id" (click)="reactivar(u)">Reactivar</button>
                    <button type="button" class="boton-peligro boton-chico" [disabled]="trabajando() === u.id" (click)="eliminar(u)">Eliminar definitivamente</button>
                  }
                </span>
              </li>
            }
          </ul>
        </section>
      }
    </div>
  `,
})
export class Usuarios implements OnInit {
  private readonly api = inject(SomaApiService);
  private readonly dialogos = inject(DialogoService);
  private readonly toasts = inject(ToastService);
  private readonly sesion = inject(SesionService);

  protected readonly cuentas = signal<UsuarioAdmin[]>([]);
  protected readonly activos = signal(true);
  protected readonly rol = signal('');
  protected readonly busqueda = signal('');
  protected readonly cargando = signal(true);
  protected readonly trabajando = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly visibles = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    return q ? this.cuentas().filter((u) => u.nombreCompleto.toLowerCase().includes(q) || u.correoInstitucional.includes(q)) : this.cuentas();
  });

  ngOnInit(): void { this.cargar(); }

  protected rolTexto(r: UsuarioAdmin['rol']): string { return ROLES[r] ?? r; }
  protected esPropia(u: UsuarioAdmin): boolean { return u.correoInstitucional === this.sesion.sesion()?.correoInstitucional; }
  protected verActivos(valor: boolean): void { if (this.activos() !== valor) { this.activos.set(valor); this.cargar(); } }
  protected cambiarRol(valor: string): void { this.rol.set(valor); this.cargar(); }

  private cargar(): void {
    this.cargando.set(true);
    this.api.usuarios(this.activos(), this.rol() || undefined).subscribe({
      next: (l) => { this.cuentas.set(l); this.error.set(null); this.cargando.set(false); },
      error: (e: HttpErrorResponse) => { this.error.set(mensajeDeError(e)); this.cargando.set(false); },
    });
  }

  protected archivar(u: UsuarioAdmin): void {
    void this.dialogos.confirmar({
      titulo: 'Archivar cuenta',
      mensaje: `${u.nombreCompleto} (${u.correoInstitucional}, ${this.rolTexto(u.rol).toLowerCase()}) dejará de poder iniciar sesión.`,
      detalle: ['Su historial se conserva completo: casos, sesiones, participaciones, notas y retroalimentaciones.', 'Podrás reactivar la cuenta desde «Archivados».'],
      confirmar: 'Archivar cuenta',
    }).then((ok) => {
      if (!ok) { return; }
      this.ejecutar(u, this.api.archivarUsuario(u.id), `Cuenta de ${u.nombreCompleto} archivada.`);
    });
  }

  protected reactivar(u: UsuarioAdmin): void {
    void this.dialogos.confirmar({
      titulo: 'Reactivar cuenta',
      mensaje: `${u.nombreCompleto} (${u.correoInstitucional}) podrá volver a iniciar sesión con los mismos permisos que tenía.`,
      confirmar: 'Reactivar',
    }).then((ok) => {
      if (!ok) { return; }
      this.ejecutar(u, this.api.reactivarUsuario(u.id), `Cuenta de ${u.nombreCompleto} reactivada.`);
    });
  }

  /** Primero pregunta al servidor si se puede; si no, explica por qué y ofrece archivar en su lugar. */
  protected eliminar(u: UsuarioAdmin): void {
    this.trabajando.set(u.id);
    this.api.evaluarEliminacionUsuario(u.id).subscribe({
      next: (ev) => {
        this.trabajando.set(null);
        if (!ev.permitida) {
          // Con historial, la alternativa segura es archivar (si aún está activa); si ya está archivada, se queda así.
          const ofreceArchivar = u.activo && ev.registros.length > 0;
          void this.dialogos.confirmar({
            titulo: 'No se puede eliminar',
            mensaje: ev.motivo,
            detalle: ev.registros.map((r) => `${r.descripcion}: ${r.cantidad}`),
            confirmar: ofreceArchivar ? 'Archivar en su lugar' : 'Entendido',
            cancelar: ofreceArchivar ? 'Cancelar' : 'Cerrar',
          }).then((ok) => { if (ok && ofreceArchivar) { this.ejecutar(u, this.api.archivarUsuario(u.id), `Cuenta de ${u.nombreCompleto} archivada.`); } });
          return;
        }
        void this.dialogos.confirmar({
          titulo: 'Eliminar cuenta definitivamente',
          mensaje: `Se eliminará la cuenta de ${u.nombreCompleto} (${u.correoInstitucional}, ${this.rolTexto(u.rol).toLowerCase()}).`,
          detalle: ['No tiene registros académicos ni comunicaciones que conservar.', 'Se borran también sus permisos, avisos y preferencias. La auditoría conserva su correo.', 'Esta acción no se puede deshacer.'],
          confirmar: 'Eliminar definitivamente',
          peligro: true,
        }).then((ok) => {
          if (!ok) { return; }
          this.ejecutar(u, this.api.eliminarUsuario(u.id), `Cuenta de ${u.nombreCompleto} eliminada.`);
        });
      },
      error: (e: HttpErrorResponse) => { this.trabajando.set(null); this.toasts.error(mensajeDeError(e)); },
    });
  }

  private ejecutar(u: UsuarioAdmin, operacion: Observable<unknown>, mensaje: string): void {
    this.trabajando.set(u.id);
    operacion.subscribe({
      next: () => { this.trabajando.set(null); this.toasts.ok(mensaje); this.cargar(); },
      error: (e: HttpErrorResponse) => { this.trabajando.set(null); this.toasts.error(mensajeDeError(e)); this.cargar(); },
    });
  }
}
