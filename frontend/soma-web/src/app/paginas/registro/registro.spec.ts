import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { Registro } from './registro';

function montar(token = 'tok123') {
  TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => token } } } }],
  });
  const http = TestBed.inject(HttpTestingController);
  const f = TestBed.createComponent(Registro);
  f.detectChanges();
  return { f, http, el: f.nativeElement as HTMLElement };
}

describe('Registro (invitación)', () => {
  it('muestra el correo de la invitación sin dejar editarlo', () => {
    const { f, http, el } = montar();
    http.expectOne('/api/invitaciones/publica/tok123').flush({ correo: 'sofia@institucion.edu.co', docente: 'Carlos Gómez', vence: '2026-12-01T10:00:00' });
    f.detectChanges();
    const correo = el.querySelector('input[type=email]') as HTMLInputElement;
    expect(correo.value).toBe('sofia@institucion.edu.co');
    expect(correo.readOnly).toBe(true);
    expect(el.textContent).toContain('Carlos Gómez');
    expect(el.textContent).toContain('Crea tu cuenta');
  });

  it('explica por qué no sirve un enlace vencido', () => {
    const { f, http, el } = montar();
    http.expectOne('/api/invitaciones/publica/tok123').flush({ codigo: 'INVITACION_VENCIDA', mensaje: 'Esta invitación venció. Pídele a tu docente que te la reenvíe.' }, { status: 410, statusText: 'Gone' });
    f.detectChanges();
    expect(el.querySelector('[role=alert]')?.textContent).toContain('venció');
    expect(el.querySelector('form')).toBeNull();
  });

  it('no envía si las contraseñas no coinciden', () => {
    const { f, http, el } = montar();
    http.expectOne('/api/invitaciones/publica/tok123').flush({ correo: 'sofia@institucion.edu.co', docente: 'Carlos', vence: '2026-12-01T10:00:00' });
    f.detectChanges();
    const c = f.componentInstance as unknown as Record<string, unknown>;
    Object.assign(c, { nombre: 'Sofía Nueva', documento: '1012345678', nacimiento: '2003-05-17', clave: 'Ecos-2026-Fuerte', clave2: 'Distinta-2026', acepto: true });
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    f.detectChanges();
    expect(el.querySelector('[role=alert]')?.textContent).toContain('no coinciden');
    http.expectNone('/api/invitaciones/publica/tok123/aceptar');
  });
});
