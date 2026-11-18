import { AfterViewInit, Component, ElementRef, NgZone, OnDestroy, inject, signal, viewChild } from '@angular/core';
import type { MotorEscena } from './motor';

/**
 * Fondo tridimensional de SOMA (Three.js: cristal con transmisión, agua reflectante, HDRI de Poly Haven).
 * Si el navegador no tiene WebGL2 se muestra la imagen estática. Con "reducir movimiento" se dibuja un solo cuadro.
 */
@Component({
  selector: 'soma-fondo-3d',
  template: `<img class="respaldo" src="fondo-login.jpg" alt="" /><canvas #lienzo aria-hidden="true"></canvas>`,
  styles: `
    :host { position: fixed; inset: 0; z-index: 0; display: block; background: #d8cdee; }
    .respaldo, canvas { position: absolute; inset: 0; display: block; width: 100%; height: 100%; }
    .respaldo { object-fit: cover; }
  `,
})
export class Fondo3d implements AfterViewInit, OnDestroy {
  private readonly lienzo = viewChild.required<ElementRef<HTMLCanvasElement>>('lienzo');
  private readonly zona = inject(NgZone);
  protected readonly sinWebgl = signal(false);
  private motor?: MotorEscena;
  private destruido = false;

  async ngAfterViewInit(): Promise<void> {
    const canvas = this.lienzo().nativeElement;
    const prueba = document.createElement('canvas').getContext('webgl2');
    if (!prueba) { canvas.style.display = 'none'; return; }
    const quieto = matchMedia('(prefers-reduced-motion: reduce)').matches || location.search.includes('still');
    try {
      const { iniciarEscena } = await import('./motor');
      const motor = await this.zona.runOutsideAngular(() => iniciarEscena(canvas, { animar: !quieto }));
      if (this.destruido) motor.detener(); else this.motor = motor;
    } catch (e) {
      console.warn('No se pudo iniciar la escena 3D; se usa la imagen estática.', e);
      canvas.style.display = 'none';
    }
  }

  ngOnDestroy(): void { this.destruido = true; this.motor?.detener(); }
}
