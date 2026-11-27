import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { mensajeDeError } from '../../core/con-operaciones';
import { EcosService } from '../../core/ecos.service';

/** Administración: carga del banco de preguntas de Ecos (JSON normalizado). Cargar la misma versión dos veces no cambia nada. */
@Component({
  selector: 'app-ecos-catalogo',
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2 class="sr-only">Ecos de una decisión · catálogo</h2>
        <p class="suave">Pega el banco de preguntas exportado del juego (campos casoVersion, casoId, nombre, criterios, preguntas).</p></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (aviso(); as a) { <p class="aviso-ok" role="status">{{ a }}</p> }
      <section class="cristal bloque">
        <label for="json">Catálogo (JSON)</label>
        <textarea id="json" rows="14" #t [value]="texto()" (input)="texto.set(t.value)" spellcheck="false" style="width:100%;font-family:monospace"></textarea>
        <div class="acciones">
          <label class="boton-suave archivo">
            <input type="file" class="sr-only" accept=".json,application/json" (change)="leer($event)" aria-label="Elegir archivo JSON" />
            Elegir archivo JSON
          </label>
          <span class="ayuda">{{ archivo() || 'Ningún archivo seleccionado' }}</span>
          <button type="button" class="boton-primario" [disabled]="ocupado() || !texto().trim()" (click)="enviar()">Cargar catálogo</button>
        </div>
      </section>
    </div>
  `,
  styles: `.archivo { cursor: pointer; } .archivo:focus-within { outline: 3px solid rgba(var(--d-138_119_184),.6); outline-offset: 2px; } .acciones { align-items: center; }`,
})
export class EcosCatalogo {
  private readonly ecos = inject(EcosService);
  protected readonly texto = signal('');
  protected readonly error = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly archivo = signal('');

  protected leer(e: Event): void {
    const f = (e.target as HTMLInputElement).files?.[0];
    if (f) { this.archivo.set(f.name); void f.text().then((t) => this.texto.set(t)); }
  }

  protected enviar(): void {
    this.error.set(null);
    this.aviso.set(null);
    let datos: unknown;
    try { datos = JSON.parse(this.texto()); } catch { this.error.set('El texto no es un JSON válido.'); return; }
    this.ocupado.set(true);
    this.ecos.cargarCatalogo(datos).subscribe({
      next: (r) => { this.aviso.set(r.creado ? `Catálogo cargado (versión ${r.casoVersion}).` : 'Esa versión ya estaba cargada; no se cambió nada.'); this.ocupado.set(false); },
      error: (e: HttpErrorResponse) => { this.error.set(mensajeDeError(e)); this.ocupado.set(false); },
    });
  }
}
