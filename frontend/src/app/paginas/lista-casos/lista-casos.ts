import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CasosApiService } from '../../core/casos-api.service';
import { ConOperaciones } from '../../core/con-operaciones';
import { CasoResumen, EstadoCaso } from '../../core/modelos/caso';

/** Panel del docente: sus casos de estudio y la creación de uno nuevo. */
@Component({
  selector: 'app-lista-casos',
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <div class="pila">
      <div>
        <h1>Casos de estudio</h1>
        <p class="suave">Cree, valide y publique los casos de sus prácticas académicas.</p>
      </div>

      <div class="resumen">
        @for (item of resumen(); track item.etiqueta) {
          <div class="tarjeta">
            <p class="suave">{{ item.etiqueta }}</p>
            <strong class="cifra">{{ item.total }}</strong>
          </div>
        }
      </div>

      <form class="tarjeta fila" (ngSubmit)="crear()">
        <label class="campo">Nombre del caso nuevo
          <input name="nombre" maxlength="150" placeholder="Puede completarlo después" [(ngModel)]="nombreNuevo" />
        </label>
        <button type="submit" class="primario" [disabled]="ocupado()">Crear borrador</button>
      </form>

      @if (error(); as mensaje) { <p class="alerta alerta-error" role="alert">{{ mensaje }}</p> }

      <section class="tarjeta">
        <div class="fila separada encabezado">
          <h2>Mis casos</h2>
          <label class="fila suave">Estado
            <select class="filtro" name="filtro" [ngModel]="filtro()" (ngModelChange)="filtrar($event)">
              <option value="">Todos</option>
              <option value="BORRADOR">Borrador</option>
              <option value="PUBLICADO">Publicado</option>
              <option value="ARCHIVADO">Archivado</option>
            </select>
          </label>
        </div>

        @if (cargando()) {
          <p class="vacio">Cargando casos…</p>
        } @else {
          @for (caso of casos(); track caso.id) {
            <div class="caso fila separada">
              <div class="pila-corta datos">
                <strong>{{ caso.nombre ?? '(Sin nombre)' }}</strong>
                <span class="suave">Actualizado {{ caso.fechaActualizacion | date: 'dd/MM/yyyy HH:mm' }}</span>
              </div>
              <div class="fila">
                <span class="estado estado-{{ caso.estado }}">{{ caso.estado }}</span>
                <a [routerLink]="['/docente/casos', caso.id]">{{ caso.estado === 'BORRADOR' ? 'Editar' : 'Revisar' }}</a>
              </div>
            </div>
          } @empty {
            <p class="vacio">No hay casos para mostrar.</p>
          }
        }
      </section>
    </div>
  `,
  styles: `
    .resumen { display: grid; grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr)); gap: 1rem; }
    .cifra { font-size: 1.6rem; color: var(--marino); }
    .encabezado h2 { margin: 0; }
    .encabezado { margin-bottom: .5rem; }
    .filtro { width: auto; }
    .caso { padding: .7rem 0; border-top: 1px solid var(--borde); }
    .datos { flex: 1 1 14rem; min-width: 0; overflow-wrap: anywhere; }
  `,
})
export class ListaCasos extends ConOperaciones implements OnInit {
  private readonly api = inject(CasosApiService);
  private readonly router = inject(Router);

  protected readonly casos = signal<CasoResumen[]>([]);
  private readonly todos = signal<CasoResumen[]>([]);
  protected readonly cargando = signal(true);
  protected readonly filtro = signal<EstadoCaso | ''>('');
  protected nombreNuevo = '';

  protected readonly resumen = computed(() => {
    const contar = (estado: EstadoCaso) => this.todos().filter((c) => c.estado === estado).length;
    return [
      { etiqueta: 'Casos creados', total: this.todos().length },
      { etiqueta: 'Borradores', total: contar('BORRADOR') },
      { etiqueta: 'Publicados', total: contar('PUBLICADO') },
      { etiqueta: 'Archivados', total: contar('ARCHIVADO') },
    ];
  });

  ngOnInit(): void {
    this.operar(this.api.listar(), (casos) => {
      this.todos.set(casos);
      this.casos.set(casos);
      this.cargando.set(false);
    }, undefined, () => this.cargando.set(false));
  }

  protected filtrar(estado: EstadoCaso | ''): void {
    this.filtro.set(estado);
    this.cargando.set(true);
    this.operar(this.api.listar(estado || undefined), (casos) => {
      this.casos.set(casos);
      this.cargando.set(false);
    }, undefined, () => this.cargando.set(false));
  }

  protected crear(): void {
    this.operar(this.api.crear({ nombre: this.nombreNuevo || null }), (caso) => {
      void this.router.navigate(['/docente/casos', caso.id]);
    });
  }
}
