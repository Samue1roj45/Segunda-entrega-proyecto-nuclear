import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { EcosEstudiante, apiParaJuego } from './ecos-estudiante';

describe('EcosEstudiante', () => {
  it('muestra el código y deja el enlace ecos:// intacto (sin el prefijo "unsafe:")', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const f = TestBed.createComponent(EcosEstudiante);
    f.detectChanges();
    http.expectOne('/api/ecos/v1/mis-intentos').flush({ caso: 'Caso', disponible: true, maxIntentos: 1, usados: 0, restantes: 1, intentos: [] });
    http.expectOne('/api/ecos/v1/pixel-streaming').flush({ activo: false, url: '', origen: '' });
    f.detectChanges();

    (f.nativeElement.querySelector('button.boton-primario') as HTMLButtonElement).click();
    http.expectOne('/api/ecos/v1/sesiones').flush({
      sesionId: 's1', codigo: 'K7Q2-9XMP', intento: 1, venceEn: new Date(Date.now() + 600000).toISOString(),
      enlace: 'ecos://iniciar?sesion=K7Q2-9XMP&api=http://localhost:8080/api/ecos/v1',
    });
    http.expectOne('/api/ecos/v1/mis-intentos').flush({ caso: 'Caso', disponible: false, maxIntentos: 1, usados: 0, restantes: 1, intentos: [] });
    f.detectChanges();

    const el = f.nativeElement as HTMLElement;
    expect(el.querySelector('.codigo')?.textContent).toContain('K7Q2-9XMP');
    expect(el.querySelector('a.boton-primario')?.getAttribute('href')).toBe('ecos://iniciar?sesion=K7Q2-9XMP&api=http://localhost:8080/api/ecos/v1');
    expect(el.querySelector('pre')?.textContent).toContain('-EcosApi=http://localhost:8080/api/ecos/v1');
    f.destroy();
  });

  it('con Pixel Streaming muestra el juego dentro de la página y no el enlace ecos://', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const f = TestBed.createComponent(EcosEstudiante);
    f.detectChanges();
    http.expectOne('/api/ecos/v1/mis-intentos').flush({ caso: 'Caso', disponible: true, maxIntentos: 1, usados: 0, restantes: 1, intentos: [] });
    http.expectOne('/api/ecos/v1/pixel-streaming').flush({ activo: true, url: 'https://juego.ejemplo.edu.co/?StreamerId=Ecos', origen: 'https://juego.ejemplo.edu.co' });
    f.detectChanges();
    const el = f.nativeElement as HTMLElement;
    expect(el.textContent).toContain('aquí mismo, dentro de esta página');

    (el.querySelector('button.boton-primario') as HTMLButtonElement).click();
    http.expectOne('/api/ecos/v1/sesiones').flush({
      sesionId: 's1', codigo: 'K7Q2-9XMP', intento: 1, venceEn: new Date(Date.now() + 600000).toISOString(),
      enlace: 'ecos://iniciar?sesion=K7Q2-9XMP&api=http://localhost:8080/api/ecos/v1',
    });
    http.expectOne('/api/ecos/v1/mis-intentos').flush({ caso: 'Caso', disponible: false, maxIntentos: 1, usados: 0, restantes: 1, intentos: [] });
    f.detectChanges();

    expect(el.querySelector('app-visor-pixel-streaming iframe')?.getAttribute('src')).toBe('https://juego.ejemplo.edu.co/?StreamerId=Ecos');
    expect(el.querySelector('a.boton-primario')).toBeNull();
    expect(el.querySelector('.en-vivo')?.textContent).toContain('Esperando que el juego se conecte');
    f.destroy();
  });

  it('al juego por streaming le pasa la API de la misma página (en localhost, la del backend)', () => {
    const backend = 'http://localhost:8080/api/ecos/v1';
    expect(apiParaJuego({ hostname: 'front-lbjo.onrender.com', origin: 'https://front-lbjo.onrender.com' }, backend))
      .toBe('https://front-lbjo.onrender.com/api/ecos/v1');
    expect(apiParaJuego({ hostname: 'localhost', origin: 'http://localhost:4200' }, backend)).toBe(backend);
    expect(apiParaJuego({ hostname: '127.0.0.1', origin: 'http://127.0.0.1:4200' }, backend)).toBe(backend);
  });
});
