import { HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { autenticacionInterceptor } from './acceso';
import { CasosApiService } from './casos-api.service';
import { mensajeDeError } from './con-operaciones';
import { Permiso } from './modelos/permiso';
import { Rol } from './modelos/rol';
import { SesionService } from './sesion.service';

const SESION = {
  token: 'abc',
  correoInstitucional: 'carlos.gomez@institucion.edu.co',
  nombreCompleto: 'Carlos Gómez',
  rol: Rol.Docente,
  permisos: [Permiso.GestionarCasos],
};

describe('Acceso al módulo de casos', () => {
  let http: HttpTestingController;
  let sesiones: SesionService;
  let api: CasosApiService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'login', children: [] }]),
        provideHttpClient(withInterceptors([autenticacionInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    sesiones = TestBed.inject(SesionService);
    api = TestBed.inject(CasosApiService);
  });

  function iniciarSesion(): void {
    sesiones.iniciar(SESION.correoInstitucional, 'clave').subscribe();
    http.expectOne('/api/auth/personal/login').flush(SESION);
  }

  it('reconoce el permiso de gestionar casos y envía el token', () => {
    iniciarSesion();
    expect(sesiones.puedeGestionarCasos()).toBe(true);

    api.listar().subscribe();

    const solicitud = http.expectOne('/api/casos');
    expect(solicitud.request.headers.get('Authorization')).toBe('Bearer abc');
    solicitud.flush([]);
  });

  it('un docente sin el permiso no puede gestionar casos', () => {
    sesiones.iniciar(SESION.correoInstitucional, 'clave').subscribe();
    http.expectOne('/api/auth/personal/login').flush({ ...SESION, permisos: [] });

    expect(sesiones.puedeGestionarCasos()).toBe(false);
  });

  it('cierra la sesión cuando el servidor responde 401', () => {
    iniciarSesion();

    api.listar().subscribe({ error: () => undefined });
    http.expectOne('/api/casos').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(sesiones.sesion()).toBeNull();
    expect(sessionStorage.length).toBe(0);
  });

  it('muestra el mensaje del servidor y uno propio si no hay conexión', () => {
    expect(mensajeDeError(new HttpErrorResponse({ status: 409, error: { mensaje: 'El caso está PUBLICADO' } })))
      .toBe('El caso está PUBLICADO');
    expect(mensajeDeError(new HttpErrorResponse({ status: 0 }))).toContain('No se pudo conectar');
  });
});
