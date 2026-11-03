import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ConOperaciones } from '../../core/con-operaciones';
import { DocentesApiService } from '../../core/docentes-api.service';
import { DESCRIPCION_PERMISO, Docente, PERMISOS_ASIGNABLES } from '../../core/modelos/docente';
import { Permiso } from '../../core/modelos/permiso';

/**
 * RF-03: el administrador crea docentes y les asigna o retira permisos. La pantalla solo la abre quien
 * tiene ADMINISTRAR_DOCENTES, pero la protección real es el Proxy del backend, que responde 403 a los demás.
 */
@Component({
  selector: 'app-admin-docentes',
  imports: [FormsModule],
  template: `
    <div class="pila">
      <div>
        <h1>Docentes y permisos</h1>
        <p class="suave">Cree las cuentas de los docentes y defina qué funciones puede usar cada uno.</p>
      </div>

      <form #formulario="ngForm" class="tarjeta pila-corta" novalidate (ngSubmit)="crear(formulario)">
        <h2>Nuevo docente</h2>
        <div class="fila campos">
          <label class="campo">Nombre completo
            <input name="nombre" required maxlength="150" [(ngModel)]="nombre" #campoNombre="ngModel" />
            @if (enviado() && campoNombre.invalid) {
              <span class="falta" role="alert">El nombre completo es obligatorio.</span>
            }
          </label>
          <label class="campo">Correo institucional
            <input type="email" name="correo" required maxlength="150" [(ngModel)]="correo" #campoCorreo="ngModel" />
            @if (enviado() && campoCorreo.invalid) {
              <span class="falta" role="alert">El correo es obligatorio.</span>
            }
          </label>
        </div>
        <div class="fila fin">
          <button type="submit" class="primario" [disabled]="ocupado()">Crear docente</button>
        </div>
      </form>

      @if (error(); as mensaje) { <p class="alerta alerta-error" role="alert">{{ mensaje }}</p> }
      @if (aviso(); as mensaje) { <p class="alerta alerta-exito" role="status">{{ mensaje }}</p> }

      <section class="tarjeta pila">
        <h2>Docentes registrados</h2>
        @if (cargando()) {
          <p class="vacio">Cargando docentes…</p>
        } @else {
          @for (docente of docentes(); track docente.id) {
            <div class="docente pila-corta">
              <div class="fila separada">
                <div class="pila-corta">
                  <strong>{{ docente.nombreCompleto }}</strong>
                  <span class="suave">{{ docente.correoInstitucional }}</span>
                </div>
                <span class="estado" [class.activo]="docente.activo">{{ docente.activo ? 'Activo' : 'Inactivo' }}</span>
              </div>
              <fieldset class="permisos">
                <legend class="suave">Permisos</legend>
                @for (permiso of asignables; track permiso) {
                  <label class="fila permiso">
                    <input type="checkbox" [name]="'p-' + docente.id + '-' + permiso"
                           [checked]="marcado(docente, permiso)" (change)="alternar(docente, permiso)" />
                    {{ descripcion[permiso] }}
                  </label>
                }
              </fieldset>
              <div class="fila fin">
                <button type="button" class="exito" [disabled]="ocupado() || !cambiado(docente)"
                        (click)="guardarPermisos(docente)">Guardar permisos</button>
              </div>
            </div>
          } @empty {
            <p class="vacio">Aún no hay docentes registrados.</p>
          }
        }
      </section>
    </div>
  `,
  styles: `
    .campos { align-items: flex-start; }
    .falta { color: var(--rojo); font-weight: 400; font-size: .8rem; }
    .docente { border-top: 1px solid var(--borde); padding-top: .9rem; }
    .estado { color: var(--texto-suave); background: var(--gris-fondo); }
    .estado.activo { color: var(--verde); background: var(--verde-fondo); }
    .permisos { border: 0; padding: 0; margin: 0; display: grid; grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr)); gap: .3rem .9rem; }
    .permiso { font-size: .9rem; gap: .45rem; flex-wrap: nowrap; }
    .permiso input { width: auto; }
  `,
})
export class AdminDocentes extends ConOperaciones implements OnInit {
  private readonly api = inject(DocentesApiService);
  protected readonly asignables = PERMISOS_ASIGNABLES;
  protected readonly descripcion = DESCRIPCION_PERMISO;
  protected readonly docentes = signal<Docente[]>([]);
  protected readonly cargando = signal(true);
  protected readonly enviado = signal(false);
  /** Permisos marcados y aún no guardados, por docente. */
  private readonly borradores = signal<ReadonlyMap<number, readonly Permiso[]>>(new Map());
  protected nombre = '';
  protected correo = '';

  ngOnInit(): void {
    this.api.listar().subscribe({
      next: (docentes) => {
        this.docentes.set(docentes);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set('No se pudo cargar la lista de docentes.');
      },
    });
  }

  /** Con datos incompletos no se envía la solicitud (CP-RF03-02); el duplicado lo informa el servidor. */
  protected crear(formulario: NgForm): void {
    this.enviado.set(true);
    if (formulario.invalid) {
      return;
    }
    const datos = { nombreCompleto: this.nombre.trim(), correoInstitucional: this.correo.trim() };
    this.operar(this.api.crear(datos), (docente) => {
      this.docentes.update((lista) => [...lista, docente]);
      this.enviado.set(false);
      formulario.resetForm();
    }, 'Docente creado. Asígnele los permisos que necesite.');
  }

  protected marcado(docente: Docente, permiso: Permiso): boolean {
    return this.permisosDe(docente).includes(permiso);
  }

  protected alternar(docente: Docente, permiso: Permiso): void {
    const actuales = this.permisosDe(docente);
    const nuevos = actuales.includes(permiso) ? actuales.filter((p) => p !== permiso) : [...actuales, permiso];
    this.borradores.update((mapa) => new Map(mapa).set(docente.id, nuevos));
  }

  protected cambiado(docente: Docente): boolean {
    const borrador = this.borradores().get(docente.id);
    return !!borrador && !mismoConjunto(borrador, docente.permisos);
  }

  /** CP-RF03-04 y CP-RF03-05: reemplaza el conjunto de permisos del docente. */
  protected guardarPermisos(docente: Docente): void {
    const permisos = this.permisosDe(docente);
    this.operar(this.api.asignarPermisos(docente.id, permisos), (actualizado) => {
      this.docentes.update((lista) => lista.map((d) => (d.id === actualizado.id ? actualizado : d)));
      this.borradores.update((mapa) => {
        const copia = new Map(mapa);
        copia.delete(docente.id);
        return copia;
      });
    }, `Permisos de ${docente.nombreCompleto} actualizados. Se aplican en su siguiente inicio de sesión.`);
  }

  private permisosDe(docente: Docente): readonly Permiso[] {
    return this.borradores().get(docente.id) ?? docente.permisos;
  }
}

function mismoConjunto(a: readonly Permiso[], b: readonly Permiso[]): boolean {
  return a.length === b.length && a.every((p) => b.includes(p));
}
