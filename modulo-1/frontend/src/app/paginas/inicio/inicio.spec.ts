import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { Permiso } from '../../core/modelos/permiso';
import { Rol } from '../../core/modelos/rol';
import { SesionUsuario } from '../../core/modelos/sesion-usuario';
import { AccesoDenegado } from '../acceso-denegado/acceso-denegado';
import { Inicio } from './inicio';

function sesion(rol: Rol, permisos: Permiso[]): SesionUsuario {
  return { token: 't', correoInstitucional: 'usuario@institucion.edu.co', nombreCompleto: 'Usuario', rol, permisos };
}

async function mostrarInicio(datos: SesionUsuario): Promise<HTMLElement> {
  sessionStorage.setItem('soma.sesion', JSON.stringify(datos));
  TestBed.configureTestingModule({ imports: [Inicio], providers: [provideRouter([]), provideHttpClient()] });
  const fixture = TestBed.createComponent(Inicio);
  await fixture.whenStable();
  return fixture.nativeElement as HTMLElement;
}

function titulos(raiz: HTMLElement): string[] {
  return Array.from(raiz.querySelectorAll('.opcion-menu h2')).map((h) => h.textContent?.trim() ?? '');
}

describe('Menú de inicio según rol y permisos (RF-02)', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.resetTestingModule();
  });

  it('CP-RF02-01: el estudiante ve solo sus funciones', async () => {
    const raiz = await mostrarInicio(
      sesion(Rol.Estudiante, [Permiso.ParticiparEnCasos, Permiso.ConsultarResultadosPropios]),
    );
    expect(titulos(raiz)).toEqual(['Participar en casos', 'Mis resultados']);
    expect(raiz.textContent).toContain('Estudiante');
  });

  it('CP-RF02-03: el docente con permiso ve la gestión de casos (pantalla del módulo 2)', async () => {
    const raiz = await mostrarInicio(sesion(Rol.Docente, [Permiso.GestionarCasos]));
    expect(titulos(raiz)).toEqual(['Casos de estudio']);
    expect(raiz.textContent).toContain('Disponible al integrar el módulo 2');
  });

  it('CP-RF02-04: el docente sin permisos no ve opciones y recibe una indicación', async () => {
    const raiz = await mostrarInicio(sesion(Rol.Docente, []));
    expect(titulos(raiz)).toEqual([]);
    expect(raiz.textContent).toContain('aún no tiene permisos asignados');
  });

  it('el administrador ve la administración de docentes', async () => {
    const raiz = await mostrarInicio(sesion(Rol.Administrador, [Permiso.AdministrarDocentes]));
    expect(titulos(raiz)).toEqual(['Docentes y permisos']);
    expect(raiz.querySelector('a.ir')?.getAttribute('href')).toBe('/admin/docentes');
  });

  it('la página de acceso denegado informa que no tiene permiso', async () => {
    TestBed.configureTestingModule({ imports: [AccesoDenegado], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(AccesoDenegado);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No tiene permiso para acceder');
  });
});
