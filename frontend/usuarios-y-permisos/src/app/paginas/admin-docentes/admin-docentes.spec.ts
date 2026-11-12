import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { Docente } from '../../core/modelos/docente';
import { Permiso } from '../../core/modelos/permiso';
import { AdminDocentes } from './admin-docentes';

const CARLOS: Docente = {
  id: 2,
  correoInstitucional: 'carlos.gomez@institucion.edu.co',
  nombreCompleto: 'Carlos Gómez',
  activo: true,
  permisos: [Permiso.GestionarCasos],
};

describe('Administración de docentes (RF-03)', () => {
  let fixture: ComponentFixture<AdminDocentes>;
  let http: HttpTestingController;
  let raiz: HTMLElement;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [AdminDocentes],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AdminDocentes);
    raiz = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
    http.expectOne('/api/admin/docentes').flush([CARLOS]);
    await fixture.whenStable();
  });

  async function escribir(nombre: string, valor: string): Promise<void> {
    const campo = raiz.querySelector<HTMLInputElement>(`input[name="${nombre}"]`)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  async function crear(): Promise<void> {
    raiz.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  function casilla(permiso: Permiso): HTMLInputElement {
    return raiz.querySelector<HTMLInputElement>(`input[name="p-${CARLOS.id}-${permiso}"]`)!;
  }

  function botonGuardar(): HTMLButtonElement {
    return Array.from(raiz.querySelectorAll<HTMLButtonElement>('button')).find((b) =>
      b.textContent?.includes('Guardar permisos'),
    )!;
  }

  it('lista los docentes con sus permisos marcados', () => {
    expect(raiz.textContent).toContain('Carlos Gómez');
    expect(casilla(Permiso.GestionarCasos).checked).toBe(true);
    expect(casilla(Permiso.DescargarReportes).checked).toBe(false);
    expect(botonGuardar().disabled).toBe(true);
  });

  it('CP-RF03-01: crea el docente y lo agrega a la lista', async () => {
    await escribir('nombre', 'Marta Ruiz');
    await escribir('correo', 'marta.ruiz@institucion.edu.co');
    await crear();

    const solicitud = http.expectOne('/api/admin/docentes');
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body).toEqual({
      nombreCompleto: 'Marta Ruiz',
      correoInstitucional: 'marta.ruiz@institucion.edu.co',
    });
    solicitud.flush({ id: 6, correoInstitucional: 'marta.ruiz@institucion.edu.co', nombreCompleto: 'Marta Ruiz', activo: true, permisos: [] });
    await fixture.whenStable();

    expect(raiz.textContent).toContain('Marta Ruiz');
    expect(raiz.textContent).toContain('Docente creado.');
  });

  it('CP-RF03-02: sin nombre indica que es obligatorio y no envía la solicitud', async () => {
    await escribir('correo', 'marta.ruiz@institucion.edu.co');
    await crear();

    expect(raiz.textContent).toContain('El nombre completo es obligatorio.');
    http.expectNone('/api/admin/docentes');
  });

  it('CP-RF03-03: muestra el mensaje del servidor si el correo ya está registrado', async () => {
    await escribir('nombre', 'Carlos Gómez');
    await escribir('correo', CARLOS.correoInstitucional);
    await crear();

    http
      .expectOne('/api/admin/docentes')
      .flush({ mensaje: 'El correo ya está registrado.' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(raiz.querySelector('.alerta-error')?.textContent).toContain('El correo ya está registrado.');
  });

  it('CP-RF03-04 y 05: asigna un permiso y retira otro en una sola actualización', async () => {
    casilla(Permiso.DescargarReportes).click();
    casilla(Permiso.GestionarCasos).click();
    await fixture.whenStable();
    expect(botonGuardar().disabled).toBe(false);

    botonGuardar().click();
    await fixture.whenStable();

    const solicitud = http.expectOne(`/api/admin/docentes/${CARLOS.id}/permisos`);
    expect(solicitud.request.method).toBe('PUT');
    expect(solicitud.request.body).toEqual({ permisos: [Permiso.DescargarReportes] });
    solicitud.flush({ ...CARLOS, permisos: [Permiso.DescargarReportes] });
    await fixture.whenStable();

    expect(casilla(Permiso.DescargarReportes).checked).toBe(true);
    expect(casilla(Permiso.GestionarCasos).checked).toBe(false);
    expect(botonGuardar().disabled).toBe(true);
    expect(raiz.textContent).toContain('Permisos de Carlos Gómez actualizados.');
  });
});
