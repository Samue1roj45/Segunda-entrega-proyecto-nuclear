import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EcosDocente } from './ecos-docente';
import { EcosEnVivo } from './ecos-en-vivo';

/** El caso «Ecos de una decisión» visto por el docente: qué es y, dentro de él, el estado en vivo y los resultados. */
@Component({
  selector: 'app-ecos-caso',
  imports: [RouterLink, EcosEnVivo, EcosDocente],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2>Ecos de una decisión</h2>
        <p class="suave">Caso en videojuego (Unreal Engine): seis actos con decisiones. SOMA calcula la nota con las respuestas del estudiante y cuenta su mejor intento.</p></div>
      <section class="cristal bloque">
        <p>Tus estudiantes entran al juego con un código que generan en SOMA. Aquí ves quién está jugando y cómo les fue.</p>
        <div class="acciones"><a class="boton-suave boton-chico enlace-boton" routerLink="/mis-estudiantes">Mis estudiantes (invitar, dar intentos)</a></div>
      </section>
      <div class="pestanas" role="tablist" aria-label="Ecos de una decisión">
        <button type="button" role="tab" [attr.aria-selected]="pestana() === 'vivo'" [class.activa]="pestana() === 'vivo'" (click)="pestana.set('vivo')">En vivo</button>
        <button type="button" role="tab" [attr.aria-selected]="pestana() === 'resultados'" [class.activa]="pestana() === 'resultados'" (click)="pestana.set('resultados')">Resultados</button>
      </div>
      @if (pestana() === 'vivo') { <app-ecos-en-vivo [incrustado]="true" /> } @else { <app-ecos-docente [incrustado]="true" /> }
    </div>
  `,
  styles: `
    .pestanas { display: flex; gap: .4rem; margin: .4rem 0 .8rem; }
    .pestanas button { border: 1px solid rgba(255,255,255,.7); background: rgba(255,255,255,.5); padding: .45rem 1.1rem; border-radius: 999px; cursor: pointer; font: inherit; color: var(--tinta-suave); }
    .pestanas button.activa { background: var(--lila-700); color: #fff; border-color: transparent; }
  `,
})
export class EcosCaso {
  protected readonly pestana = signal<'vivo' | 'resultados'>('vivo');
}
