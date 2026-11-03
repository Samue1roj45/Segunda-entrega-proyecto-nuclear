import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { autenticacionInterceptor, guardiaPermiso, guardiaSesion } from './acceso';
import { DocentesApiService } from './docentes-api.service';
import { Permiso } from './modelos/permiso';
import { Rol } from './modelos/rol';
import { SesionUsuario } from './modelos/sesion-usuario';
import { SesionService } from './sesion.service';

const ESTUDIANTE: SesionUsuario = {
  token: 'tok-est',
  correoInstitucional: 'ana.perez@institucion.edu.co',
  nombreCompleto: 'Ana Pérez',
  rol: Rol.Estudiante,
  permisos: [Permiso.ParticiparEnCasos, Permiso.ConsultarResultadosPropios],
};

const ADMIN: SesionUsuario = {
  token: 'tok-admin',
  correoInstitucional: 'admin.plataforma@institucion.edu.co',
  nombreCompleto: 'Administrador',
  rol: Rol.Administrador,
  permisos: [Permiso.AdministrarDocentes],
};

describe('Módulo 1: sesión, permisos y guardias', () => {
  let http: HttpTestingController;
  let sesiones: SesionService;
  let router: Router;

  function configurar(): void {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([autenticacionInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    sesiones = TestBed.inject(SesionService);
    router = TestBed.inject(Router);
  }

  function evaluar(guardia: ReturnType<typeof guardiaPermiso>): boolean | string {
    const resultado = TestBed.runInInjectionContext(() =>
      guardia({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    return resultado instanceof UrlTree ? router.serializeUrl(resultado) : (resultado as boolean);
  }

  function entrarComo(sesion: SesionUsuario): void {
    sesiones.iniciar(sesion.correoInstitucional, 'clave').subscribe();
    http.expectOne('/api/auth/personal/login').flush(sesion);
  }

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.resetTestingModule();
  });

  it('RF-01: el estudiante ingresa con correo y código y queda con su rol', () => {
    configurar();
    sesiones.iniciarEstudiante(ESTUDIANTE.correoInstitucional, 'CODIGO_VALIDO_01').subscribe();

    const solicitud = http.expectOne('/api/auth/estudiantes/login');
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body).toEqual({
      correoInstitucional: ESTUDIANTE.correoInstitucional,
      codigoAutorizacion: 'CODIGO_VALIDO_01',
    });
    solicitud.flush(ESTUDIANTE);

    expect(sesiones.autenticado()).toBe(true);
    expect(sesiones.rol()).toBe(Rol.Estudiante);
    expect(sesiones.tienePermiso(Permiso.ParticiparEnCasos)).toBe(true);
    expect(sesiones.tienePermiso(Permiso.GestionarCasos)).toBe(false);
    expect(sesiones.esAdministrador()).toBe(false);
  });

  it('RF-01: si el servidor rechaza las credenciales no se crea la sesión', () => {
    configurar();
    sesiones.iniciarEstudiante(ESTUDIANTE.correoInstitucional, 'CODIGO_ERRADO').subscribe({ error: () => undefined });
    http
      .expectOne('/api/auth/estudiantes/login')
      .flush({ mensaje: 'Correo o código de autorización inválidos.' }, { status: 401, statusText: 'Unauthorized' });

    expect(sesiones.autenticado()).toBe(false);
    expect(sessionStorage.length).toBe(0);
  });

  it('descarta una sesión guardada cuyo token ya venció', () => {
    sessionStorage.setItem('simulapsi.sesion', JSON.stringify({ ...ADMIN, expiraEn: '2000-01-01T00:00:00Z' }));
    configurar();
    expect(sesiones.autenticado()).toBe(false);
  });

  it('RF-02: sin sesión las guardias llevan al inicio de sesión', () => {
    configurar();
    expect(evaluar(guardiaSesion)).toBe('/login');
    expect(evaluar(guardiaPermiso(Permiso.AdministrarDocentes))).toBe('/login');
  });

  it('RF-02: con sesión pero sin el permiso la guardia lleva a acceso denegado', () => {
    configurar();
    sesiones.iniciarEstudiante(ESTUDIANTE.correoInstitucional, 'CODIGO_VALIDO_01').subscribe();
    http.expectOne('/api/auth/estudiantes/login').flush(ESTUDIANTE);

    expect(evaluar(guardiaSesion)).toBe(true);
    expect(evaluar(guardiaPermiso(Permiso.GestionarCasos))).toBe('/acceso-denegado');
    expect(evaluar(guardiaPermiso(Permiso.ParticiparEnCasos))).toBe(true);
  });

  it('RF-03: la administración de docentes envía el token y la lista de permisos', () => {
    configurar();
    entrarComo(ADMIN);
    expect(sesiones.esAdministrador()).toBe(true);

    TestBed.inject(DocentesApiService).asignarPermisos(7, [Permiso.DescargarReportes]).subscribe();

    const solicitud = http.expectOne('/api/admin/docentes/7/permisos');
    expect(solicitud.request.method).toBe('PUT');
    expect(solicitud.request.headers.get('Authorization')).toBe('Bearer tok-admin');
    expect(solicitud.request.body).toEqual({ permisos: [Permiso.DescargarReportes] });
    solicitud.flush({});
    http.verify();
  });
});
