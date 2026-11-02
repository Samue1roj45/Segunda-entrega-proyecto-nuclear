import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CasosApiService } from '../../core/casos-api.service';
import { ConOperaciones } from '../../core/con-operaciones';
import { CasoDetalle } from '../../core/modelos/caso';
import { SeccionDatos } from './seccion-datos';
import { SeccionEscenarios } from './seccion-escenarios';
import { SeccionRubrica } from './seccion-rubrica';
import { SeccionPublicacion } from './seccion-publicacion';

type Pestana = 'DATOS' | 'ESCENARIOS' | 'RUBRICA' | 'PUBLICACION';

/** Constructor del caso por secciones. Cada sección guarda en el servidor y aquí se recarga el caso. */
@Component({
  selector: 'app-constructor-caso',
  imports: [RouterLink, SeccionDatos, SeccionEscenarios, SeccionRubrica, SeccionPublicacion],
  template: `
    <div class="pila">
      <a routerLink="/docente/casos">← Volver a mis casos</a>

      @if (error(); as mensaje) { <p class="alerta alerta-error" role="alert">{{ mensaje }}</p> }

      @if (caso(); as actual) {
        <div class="fila separada">
          <h1 class="titulo">{{ actual.nombre ?? '(Sin nombre)' }}</h1>
          <span class="estado estado-{{ actual.estado }}">{{ actual.estado }}</span>
        </div>

        @if (!editable()) {
          <p class="alerta alerta-info">Este caso está {{ actual.estado }}: su contenido es de solo lectura para no alterar evaluaciones ni sesiones.</p>
        }

        <nav class="pestanas" aria-label="Secciones del caso">
          @for (item of pestanas; track item.id) {
            <button type="button" [class.activa]="pestana() === item.id"
                    [attr.aria-current]="pestana() === item.id ? 'page' : null"
                    (click)="pestana.set(item.id)">{{ item.etiqueta }}</button>
          }
        </nav>

        @switch (pestana()) {
          @case ('DATOS') { <app-seccion-datos [caso]="actual" [editable]="editable()" (cambio)="recargar()" /> }
          @case ('ESCENARIOS') { <app-seccion-escenarios [caso]="actual" [editable]="editable()" (cambio)="recargar()" /> }
          @case ('RUBRICA') { <app-seccion-rubrica [caso]="actual" [editable]="editable()" (cambio)="recargar()" /> }
          @case ('PUBLICACION') {
            <app-seccion-publicacion [caso]="actual" (cambio)="recargar()" (irA)="pestana.set($event)" />
          }
        }
      } @else if (ocupado()) {
        <p class="vacio">Cargando caso…</p>
      }
    </div>
  `,
  styles: `
    .titulo { overflow-wrap: anywhere; min-width: 0; }
    .pestanas { display: flex; gap: .4rem; flex-wrap: wrap; border-bottom: 1px solid var(--borde); padding-bottom: .6rem; }
    .pestanas button { border-color: transparent; background: transparent; }
    .pestanas button.activa { background: var(--verde-fondo); color: var(--verde); }
  `,
})
export class ConstructorCaso extends ConOperaciones implements OnInit {
  private readonly api = inject(CasosApiService);

  /** Parámetro :id de la ruta. */
  readonly id = input.required<string>();
  protected readonly caso = signal<CasoDetalle | null>(null);
  protected readonly pestana = signal<Pestana>('DATOS');
  protected readonly editable = computed(() => this.caso()?.estado === 'BORRADOR');
  protected readonly pestanas: { id: Pestana; etiqueta: string }[] = [
    { id: 'DATOS', etiqueta: '1. Datos del caso' },
    { id: 'ESCENARIOS', etiqueta: '2. Escenarios y preguntas' },
    { id: 'RUBRICA', etiqueta: '3. Rúbrica' },
    { id: 'PUBLICACION', etiqueta: '4. Validar y publicar' },
  ];

  ngOnInit(): void {
    this.recargar();
  }

  protected recargar(): void {
    this.operar(this.api.consultar(Number(this.id())), (caso) => this.caso.set(caso));
  }
}
