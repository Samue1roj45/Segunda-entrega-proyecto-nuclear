import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { Permiso } from '../../core/modelos/permiso';
import { Rol } from '../../core/modelos/rol';
import { SesionService } from '../../core/sesion.service';
import { Login } from './login';

@Component({ template: '' })
class Vacia {}

describe('Login (RF-01)', () => {
  let fixture: ComponentFixture<Login>;
  let http: HttpTestingController;
  let raiz: HTMLElement;

  beforeEach(async () => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([{ path: 'inicio', component: Vacia }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Login);
    raiz = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  async function escribir(nombre: string, valor: string): Promise<void> {
    const campo = raiz.querySelector<HTMLInputElement>(`input[name="${nombre}"]`)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  async function enviar(): Promise<void> {
    raiz.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  async function elegirPersonal(): Promise<void> {
    raiz.querySelectorAll<HTMLButtonElement>('[role="tab"]')[1].click();
    await fixture.whenStable();
  }

  it('CP-RF01-05: con campos vacíos indica los obligatorios y no envía la solicitud', async () => {
    await enviar();

    const texto = raiz.textContent ?? '';
    expect(texto).toContain('El correo institucional es obligatorio.');
    expect(texto).toContain('El código de autorización es obligatorio.');
    http.expectNone('/api/auth/estudiantes/login');
  });

  it('CP-RF01-01: el estudiante ingresa con correo y código y va a su inicio', async () => {
    await escribir('correo', '  ana.perez@institucion.edu.co ');
    await escribir('codigo', 'CODIGO_VALIDO_01');
    await enviar();

    const solicitud = http.expectOne('/api/auth/estudiantes/login');
    expect(solicitud.request.body).toEqual({
      correoInstitucional: 'ana.perez@institucion.edu.co',
      codigoAutorizacion: 'CODIGO_VALIDO_01',
    });
    solicitud.flush({
      token: 't',
      correoInstitucional: 'ana.perez@institucion.edu.co',
      nombreCompleto: 'Ana Pérez',
      rol: Rol.Estudiante,
      permisos: [Permiso.ParticiparEnCasos],
    });
    await fixture.whenStable();

    expect(TestBed.inject(SesionService).rol()).toBe(Rol.Estudiante);
    expect(TestBed.inject(Router).url).toBe('/inicio');
  });

  it('CP-RF01-02: muestra el mensaje genérico del servidor y no crea la sesión', async () => {
    await escribir('correo', 'ana.perez@institucion.edu.co');
    await escribir('codigo', 'CODIGO_ERRADO');
    await enviar();

    http
      .expectOne('/api/auth/estudiantes/login')
      .flush({ mensaje: 'Correo o código de autorización inválidos.' }, { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();

    expect(raiz.querySelector('[role="alert"]')?.textContent).toContain('Correo o código de autorización inválidos.');
    expect(TestBed.inject(SesionService).autenticado()).toBe(false);
  });

  it('el personal ingresa con correo y contraseña', async () => {
    await elegirPersonal();
    expect(raiz.querySelector('input[name="codigo"]')).toBeNull();

    await escribir('correo', 'admin.plataforma@institucion.edu.co');
    await escribir('clave', 'Clave-Admin-01');
    await enviar();

    const solicitud = http.expectOne('/api/auth/personal/login');
    expect(solicitud.request.body).toEqual({
      correoInstitucional: 'admin.plataforma@institucion.edu.co',
      clave: 'Clave-Admin-01',
    });
    solicitud.flush({
      token: 't',
      correoInstitucional: 'admin.plataforma@institucion.edu.co',
      nombreCompleto: 'Administrador',
      rol: Rol.Administrador,
      permisos: [Permiso.AdministrarDocentes],
    });
    await fixture.whenStable();
    expect(TestBed.inject(SesionService).esAdministrador()).toBe(true);
  });
});
