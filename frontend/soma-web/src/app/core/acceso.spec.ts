import { isDevMode } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { accesoDesarrollo, guardiaDesarrollo } from './acceso';

describe('guardia de la pantalla de componentes', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideRouter([])] }));

  it('en desarrollo deja abrir el catálogo y el visor de errores', () => {
    expect(isDevMode()).toBe(true);
    const decision = TestBed.runInInjectionContext(() => guardiaDesarrollo({} as never, {} as never));
    expect(decision).toBe(true);
  });

  it('fuera de desarrollo envía al inicio y no abre la herramienta', () => {
    const decision = TestBed.runInInjectionContext(() => accesoDesarrollo(false));
    expect(decision instanceof UrlTree).toBe(true);
    expect(TestBed.inject(Router).serializeUrl(decision as UrlTree)).toBe('/inicio');
  });
});
