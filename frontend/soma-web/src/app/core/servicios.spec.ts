import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DialogoService } from './dialogo.service';
import { PreferenciasService } from './preferencias.service';
import { cacheInterceptor, limpiarCache, reintentoInterceptor } from './red';
import { RegistroErroresService } from './registro-errores.service';
import { SomaApiService } from './soma-api.service';
import { TemaService } from './tema.service';

describe('DialogoService', () => {
  it('confirmar resuelve true al aceptar y false al cancelar', async () => {
    const d = TestBed.inject(DialogoService);
    const a = d.confirmar({ titulo: 'x' }); expect(d.actual()?.titulo).toBe('x'); d.cerrar(true);
    expect(await a).toBe(true); expect(d.actual()).toBeNull();
    const b = d.confirmar({ titulo: 'y' }); d.cerrar(false);
    expect(await b).toBe(false);
  });
  it('pedirTexto devuelve el texto o null', async () => {
    const d = TestBed.inject(DialogoService);
    const a = d.pedirTexto({ titulo: 't' }); d.cerrar('hola'); expect(await a).toBe('hola');
    const b = d.pedirTexto({ titulo: 't' }); d.cerrar(null); expect(await b).toBeNull();
  });
});

describe('PreferenciasService', () => {
  beforeEach(() => { localStorage.clear(); document.documentElement.removeAttribute('style'); document.documentElement.removeAttribute('data-contraste'); });
  it('aplica y recuerda tamaño de letra y contraste', () => {
    const p = TestBed.inject(PreferenciasService);
    p.fijarLetra(2); p.fijarContraste(true);
    expect(document.documentElement.style.fontSize).toBe('125%');
    expect(document.documentElement.hasAttribute('data-contraste')).toBe(true);
    expect(localStorage.getItem('soma.pref.letra')).toBe('2');
  });
});

describe('TemaService', () => {
  beforeEach(() => localStorage.clear());
  it('alterna entre día y noche y lo aplica al documento', () => {
    const t = TestBed.inject(TemaService);
    t.tema.set('dia'); t.alternar();
    expect(t.tema()).toBe('noche'); expect(document.documentElement.dataset['tema']).toBe('noche');
    t.quitar(); expect(document.documentElement.dataset['tema']).toBeUndefined();
  });

  it('el modo automático sigue al sistema y una elección explícita lo reemplaza', () => {
    const t = TestBed.inject(TemaService);
    t.fijarModo('auto');
    expect(t.modo()).toBe('auto');
    expect(['dia', 'noche']).toContain(t.tema());
    t.fijarModo('noche');
    expect(t.modo()).toBe('noche'); expect(t.tema()).toBe('noche');
    expect(localStorage.getItem('soma.tema')).toBe('noche');
  });
});

describe('PreferenciasService (cuenta)', () => {
  beforeEach(() => localStorage.clear());
  it('exporta e importa solo valores válidos', () => {
    const p = TestBed.inject(PreferenciasService);
    p.importar({ letra: 2, acento: 3, quieto: 1, avatar: 99, acento_raro: 'x', contraste: 'si' });
    expect(p.letra()).toBe(2); expect(p.acento()).toBe(3); expect(p.sinMovimiento()).toBe(true);
    expect(p.avatar()).not.toBe(99); expect(p.contraste()).toBe(false);
    expect(p.exportar()['letra']).toBe(2);
  });
});

describe('RegistroErroresService', () => {
  beforeEach(() => localStorage.clear());
  it('guarda como máximo 50 errores y genera un informe', () => {
    const r = TestBed.inject(RegistroErroresService);
    for (let i = 0; i < 60; i++) r.registrar('interfaz', `fallo ${i}`);
    expect(r.leer().length).toBe(50); expect(r.informe()).toContain('fallo 59');
    r.borrar(); expect(r.leer()).toEqual([]);
  });
});

describe('interceptores de red', () => {
  beforeEach(() => {
    limpiarCache();
    TestBed.configureTestingModule({ providers: [provideHttpClient(withInterceptors([cacheInterceptor, reintentoInterceptor])), provideHttpClientTesting()] });
  });
  it('en listas muestra lo guardado y refresca por detrás; un POST vacía la memoria', () => {
    const http = TestBed.inject(HttpClient), ctl = TestBed.inject(HttpTestingController);
    const vistos: unknown[] = [];
    http.get('/api/casos').subscribe((v) => vistos.push(v)); ctl.expectOne('/api/casos').flush(['a']);
    http.get('/api/casos').subscribe((v) => vistos.push(v)); ctl.expectOne('/api/casos').flush(['a', 'b']);
    expect(vistos).toEqual([['a'], ['a'], ['a', 'b']]);
    http.post('/api/casos', {}).subscribe(); ctl.expectOne({ method: 'POST', url: '/api/casos' }).flush({});
    const tras: unknown[] = [];
    http.get('/api/casos').subscribe((v) => tras.push(v)); ctl.expectOne('/api/casos').flush(['c']);
    expect(tras).toEqual([['c']]);
  });
  it('no guarda en memoria las rutas que no son listas', () => {
    const http = TestBed.inject(HttpClient), ctl = TestBed.inject(HttpTestingController);
    http.get('/api/casos/7').subscribe(); ctl.expectOne('/api/casos/7').flush({});
    const n: unknown[] = [];
    http.get('/api/casos/7').subscribe((v) => n.push(v)); ctl.expectOne('/api/casos/7').flush({ x: 1 });
    expect(n.length).toBe(1);
  });
});

describe('SomaApiService (cuentas, casos archivados y limpieza de auditoría)', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }));
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('lista cuentas archivadas por rol y usa las rutas de archivo, reactivación y eliminación', () => {
    const api = TestBed.inject(SomaApiService);
    const http = TestBed.inject(HttpTestingController);
    api.usuarios(false, 'DOCENTE').subscribe();
    const lista = http.expectOne((r) => r.url === '/api/admin/usuarios');
    expect(lista.request.params.get('activos')).toBe('false');
    expect(lista.request.params.get('rol')).toBe('DOCENTE');
    lista.flush([]);
    api.archivarUsuario(7).subscribe(); http.expectOne({ method: 'POST', url: '/api/admin/usuarios/7/archivo' }).flush({});
    api.reactivarUsuario(7).subscribe(); http.expectOne({ method: 'POST', url: '/api/admin/usuarios/7/reactivacion' }).flush({});
    api.evaluarEliminacionUsuario(7).subscribe(); http.expectOne({ method: 'GET', url: '/api/admin/usuarios/7/eliminacion' }).flush({ permitida: true, motivo: '', registros: [] });
    api.eliminarUsuario(7).subscribe(); http.expectOne({ method: 'DELETE', url: '/api/admin/usuarios/7' }).flush(null);
  });

  it('recupera un caso archivado con su propia ruta (no con la de publicar)', () => {
    const api = TestBed.inject(SomaApiService);
    api.recuperarCaso(3).subscribe();
    TestBed.inject(HttpTestingController).expectOne({ method: 'POST', url: '/api/casos/3/recuperacion' }).flush({});
  });

  it('la limpieza pide vista previa por fecha y siempre envía la confirmación explícita', () => {
    const api = TestBed.inject(SomaApiService);
    const http = TestBed.inject(HttpTestingController);
    api.vistaPreviaLimpiezaAuditoria('2026-11-01').subscribe();
    const previa = http.expectOne((r) => r.url === '/api/auditoria/limpieza' && r.method === 'GET');
    expect(previa.request.params.get('antesDe')).toBe('2026-11-01');
    previa.flush({ antesDe: '2026-11-01', registros: 3 });
    api.limpiarAuditoria('2026-11-01').subscribe();
    const limpieza = http.expectOne({ method: 'POST', url: '/api/auditoria/limpieza' });
    expect(limpieza.request.body).toEqual({ antesDe: '2026-11-01', confirmada: true });
    limpieza.flush({ antesDe: '2026-11-01', eliminados: 3, fecha: '2026-11-25T10:00:00' });
  });
});
