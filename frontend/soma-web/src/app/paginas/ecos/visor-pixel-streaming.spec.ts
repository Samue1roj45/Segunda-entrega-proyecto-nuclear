import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MensajeVisor, SesionParaJuego, VisorPixelStreaming } from './visor-pixel-streaming';

const ORIGEN = 'https://juego.ejemplo.edu.co';

@Component({
  imports: [VisorPixelStreaming],
  template: `<app-visor-pixel-streaming [url]="url" [origen]="origen" [sesion]="sesion()" (mensaje)="recibidos.push($event)" />`,
})
class Anfitrion {
  url = ORIGEN + '/?StreamerId=Ecos';
  origen = ORIGEN;
  sesion = signal<SesionParaJuego | null>(null);
  recibidos: MensajeVisor[] = [];
}

describe('VisorPixelStreaming', () => {
  function crear() {
    const f = TestBed.createComponent(Anfitrion);
    f.detectChanges();
    const marco = f.nativeElement.querySelector('iframe') as HTMLIFrameElement;
    const enviados: { datos: unknown; origen: string }[] = [];
    // El iframe no carga nada en las pruebas: se espía lo que SOMA le enviaría al reproductor.
    marco.contentWindow!.postMessage = ((datos: unknown, origen: string) => { enviados.push({ datos, origen }); }) as typeof window.postMessage;
    return { f, marco, enviados, anfitrion: f.componentInstance };
  }

  function delReproductor(marco: HTMLIFrameElement, datos: unknown, origen = ORIGEN) {
    window.dispatchEvent(new MessageEvent('message', { data: datos, origin: origen, source: marco.contentWindow }));
  }

  it('entrega el código al reproductor solo cuando el puente responde, y solo a su origen', () => {
    const { f, marco, enviados, anfitrion } = crear();
    anfitrion.sesion.set({ sesionId: 's1', codigo: 'K7Q2-9XMP', api: 'https://soma.ejemplo/api/ecos/v1' });
    f.detectChanges();
    expect(enviados.some((e) => (e.datos as { tipo?: string }).tipo === 'sesion')).toBe(false);

    delReproductor(marco, { fuente: 'ecos-ps', tipo: 'listo', version: 1 });
    f.detectChanges();
    const sesion = enviados.find((e) => (e.datos as { tipo?: string }).tipo === 'sesion');
    expect(sesion?.origen).toBe(ORIGEN);
    expect(sesion?.datos).toEqual({ fuente: 'soma', tipo: 'sesion', sesionId: 's1', codigo: 'K7Q2-9XMP', api: 'https://soma.ejemplo/api/ecos/v1' });
    expect(anfitrion.recibidos).toContainEqual({ tipo: 'puente', instalado: true });
    f.destroy();
  });

  it('reenvía los datos del juego y descarta mensajes de otros orígenes', () => {
    const { f, marco, anfitrion } = crear();
    delReproductor(marco, { fuente: 'ecos-ps', tipo: 'juego', datos: { acto: 3, punto: 'E1_Q1' } }, 'https://intruso.ejemplo');
    delReproductor(marco, { fuente: 'otra-cosa', tipo: 'juego', datos: { acto: 9 } });
    expect(anfitrion.recibidos.filter((m) => m.tipo === 'juego')).toHaveLength(0);

    delReproductor(marco, { fuente: 'ecos-ps', tipo: 'juego', datos: { acto: 3, punto: 'E1_Q1' } });
    delReproductor(marco, { fuente: 'ecos-ps', tipo: 'estado', estado: 'video' });
    expect(anfitrion.recibidos).toContainEqual({ tipo: 'juego', datos: { acto: 3, punto: 'E1_Q1' } });
    expect(anfitrion.recibidos).toContainEqual({ tipo: 'estado', estado: 'video' });
    f.destroy();
  });
});
