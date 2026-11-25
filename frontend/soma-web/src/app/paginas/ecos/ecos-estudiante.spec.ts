import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { EcosEstudiante } from './ecos-estudiante';

describe('EcosEstudiante', () => {
  it('muestra el código y deja el enlace ecos:// intacto (sin el prefijo "unsafe:")', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const f = TestBed.createComponent(EcosEstudiante);
    f.detectChanges();
    http.expectOne('/api/ecos/v1/mis-intentos').flush({ caso: 'Caso', disponible: true, maxIntentos: 1, usados: 0, restantes: 1, intentos: [] });
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
});
