import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { claseEstado, nombreEstado } from '../../compartido/formato';
import { mensajeDeError } from '../../core/con-operaciones';
import { CasoResumen, SomaApiService } from '../../core/soma-api.service';

/** Módulo 2: casos de estudio del docente. Lista con filtro por estado e importación de un caso en formato DSL (JSON). */
@Component({
  selector: 'app-casos-lista',
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina">
        <h2>Casos de estudio</h2>
        <div class="acciones">
          <button type="button" class="boton-suave" (click)="importando.set(!importando())">Importar caso (DSL)</button>
          <a routerLink="/casos/nuevo" class="boton-primario enlace-boton">Nuevo caso</a>
        </div>
      </div>

      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (aviso(); as a) { <p class="aviso-ok" role="status">{{ a }}</p> }

      @if (importando()) {
        <section class="cristal bloque">
          <form class="formulario" (ngSubmit)="importar()">
            <h3>Importar un caso</h3>
            <p class="ayuda">Elige un archivo .json exportado desde SOMA o pega aquí su contenido. Se crea como borrador.</p>
            <input type="file" accept=".json,application/json" (change)="leerArchivo($event)" />
            <label class="campo-f">Contenido DSL
              <textarea name="dsl" [(ngModel)]="textoDsl" rows="8" placeholder='{"dsl": "...", "version": "...", "caso": { ... }}'></textarea>
            </label>
            <div class="acciones">
              <button class="boton-primario" type="submit" [disabled]="ocupado() || !textoDsl.trim()">Importar</button>
              <button class="boton-suave" type="button" (click)="importando.set(false)">Cancelar</button>
            </div>
          </form>
        </section>
      }

      <section class="cristal bloque">
        <div class="filtros">
          <label class="campo-f">Estado
            <select [ngModel]="estado()" (ngModelChange)="cambiarEstado($event)" name="estado">
              <option value="">Todos</option><option value="BORRADOR">Borrador</option>
              <option value="PUBLICADO">Publicado</option><option value="ARCHIVADO">Archivado</option>
            </select>
          </label>
        </div>
      </section>

      @if (cargando()) { <p class="suave">Cargando casos…</p> }
      @else if (casos().length === 0) { <p class="cristal vacio">Todavía no hay casos{{ estado() ? ' en este estado' : '' }}. Crea el primero con «Nuevo caso».</p> }
      @else {
        <div class="tarjetas">
          @for (c of casos(); track c.id) {
            <article class="cristal tarjeta-item">
              <span class="etiqueta" [class]="'etiqueta ' + clase(c.estado)">{{ nombre(c.estado) }}</span>
              <h3><a [routerLink]="['/casos', c.id]">{{ c.nombre }}</a></h3>
              <div class="meta">
                @if (c.tiempoEstimadoMinutos) { <span>{{ c.tiempoEstimadoMinutos }} min estimados</span> }
                @if (c.fechaActualizacion) { <span>Actualizado {{ c.fechaActualizacion | date: 'd MMM y' }}</span> }
                @if (c.fechaPublicacion) { <span>Publicado {{ c.fechaPublicacion | date: 'd MMM y' }}</span> }
              </div>
              <div class="acciones"><a [routerLink]="['/casos', c.id]" class="boton-suave boton-chico enlace-boton">Abrir</a></div>
            </article>
          }
        </div>
      }
    </div>
  `,
})
export class CasosLista implements OnInit {
  private readonly api = inject(SomaApiService);
  private readonly router = inject(Router);

  protected readonly casos = signal<CasoResumen[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly estado = signal('');
  protected readonly importando = signal(false);
  protected readonly ocupado = signal(false);
  protected textoDsl = '';
  protected readonly nombre = nombreEstado;
  protected readonly clase = claseEstado;

  ngOnInit(): void { this.cargar(); }

  protected cambiarEstado(valor: string): void { this.estado.set(valor); this.cargar(); }

  private cargar(): void {
    this.cargando.set(true);
    this.api.casosPorEstado(this.estado() || undefined).subscribe({
      next: (c) => { this.casos.set(c); this.cargando.set(false); },
      error: (e: HttpErrorResponse) => { this.error.set(mensajeDeError(e)); this.cargando.set(false); },
    });
  }

  protected leerArchivo(evento: Event): void {
    const archivo = (evento.target as HTMLInputElement).files?.[0];
    if (!archivo) { return; }
    void archivo.text().then((t) => (this.textoDsl = t));
  }

  protected importar(): void {
    let documento: unknown;
    try { documento = JSON.parse(this.textoDsl); } catch { this.error.set('El contenido no es un JSON válido.'); return; }
    this.error.set(null);
    this.ocupado.set(true);
    this.api.importarDsl(documento).subscribe({
      next: (r) => {
        this.ocupado.set(false);
        this.aviso.set(r.advertencias.length ? `Caso importado con ${r.advertencias.length} advertencia(s): ${r.advertencias.join(' · ')}` : 'Caso importado como borrador.');
        void this.router.navigate(['/casos', r.caso.id]);
      },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
    });
  }
}
