import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { SesionService } from '../../core/sesion.service';
import { SesionUsuario } from '../../core/modelos/sesion-usuario';
import { Login } from './login';

describe('Login', () => {
  const sesion = { iniciar: vi.fn(), iniciarEstudiante: vi.fn() };

  async function crear() {
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), { provide: SesionService, useValue: sesion }],
    });
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  async function escribir(fixture: Awaited<ReturnType<typeof crear>>['fixture'], el: HTMLElement, id: string, valor: string) {
    const campo = el.querySelector<HTMLInputElement>(`#${id}`)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
  }

  beforeEach(() => {
    sesion.iniciar.mockReset();
    sesion.iniciarEstudiante.mockReset();
    localStorage.clear();
  });

  it('pide el correo cuando se envía vacío', async () => {
    const { fixture, el } = await crear();
    el.querySelector<HTMLButtonElement>('button.ingresar')!.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(el.querySelector('.aviso.error')?.textContent).toContain('Escribe tu correo institucional.');
    expect(sesion.iniciar).not.toHaveBeenCalled();
  });

  it('rechaza un correo con formato inválido', async () => {
    const { fixture, el } = await crear();
    await escribir(fixture, el, 'correo-login', 'esto-no-es-un-correo');
    el.querySelector<HTMLButtonElement>('button.ingresar')!.click();
    fixture.detectChanges();
    expect(el.querySelector('.aviso.error')?.textContent).toContain('correo válido');
  });

  it('muestra el código de acceso al cambiar a estudiante', async () => {
    const { fixture, el } = await crear();
    expect(el.querySelector('#clave-login')).not.toBeNull();
    const acceso = el.querySelector<HTMLButtonElement>('button.cambio')!;
    expect(el.querySelector('.tarjeta .cambio')).toBe(acceso);
    expect(acceso.textContent).toContain('Ingresar como estudiante');
    expect(acceso.textContent).toContain('Accede con tu código de acceso.');
    acceso.click();
    fixture.detectChanges();
    expect(el.querySelector('#codigo-login')).not.toBeNull();
    expect(el.querySelector('#clave-login')).toBeNull();
  });

  it('inicia sesión con el correo normalizado y navega al inicio', async () => {
    sesion.iniciar.mockReturnValue(of({} as SesionUsuario));
    const { fixture, el } = await crear();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await escribir(fixture, el, 'correo-login', '  Docente@Humboldt.edu.co ');
    await escribir(fixture, el, 'clave-login', 'secreta');
    el.querySelector<HTMLButtonElement>('button.ingresar')!.click();
    expect(sesion.iniciar).toHaveBeenCalledWith('docente@humboldt.edu.co', 'secreta', false);
    expect(navegar).toHaveBeenCalledWith(['/inicio']);
  });

  it('muestra el mensaje del servidor si las credenciales fallan', async () => {
    sesion.iniciar.mockReturnValue(throwError(() => ({ status: 401, error: { mensaje: 'Credenciales inválidas.' } })));
    const { fixture, el } = await crear();
    await escribir(fixture, el, 'correo-login', 'a@b.co');
    await escribir(fixture, el, 'clave-login', 'mala');
    el.querySelector<HTMLButtonElement>('button.ingresar')!.click();
    fixture.detectChanges();
    expect(el.querySelector('.aviso.error')?.textContent).toContain('Credenciales inválidas.');
  });

  it('solo recuerda el correo cuando se marca «Recordarme»', async () => {
    sesion.iniciar.mockReturnValue(of({} as SesionUsuario));
    const { fixture, el } = await crear();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await escribir(fixture, el, 'correo-login', 'a@b.co');
    await escribir(fixture, el, 'clave-login', 'x');
    el.querySelector<HTMLButtonElement>('button.ingresar')!.click();
    expect(localStorage.getItem('soma.ultimoCorreo')).toBeNull();
  });

  it('guarda el correo (y nunca la contraseña) al marcar «Recordarme»', async () => {
    sesion.iniciar.mockReturnValue(of({} as SesionUsuario));
    const { fixture, el } = await crear();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await escribir(fixture, el, 'correo-login', 'A@B.co');
    await escribir(fixture, el, 'clave-login', 'secreta');
    const casilla = el.querySelector<HTMLInputElement>('input[name=recordar]')!;
    casilla.click();
    fixture.detectChanges();
    await fixture.whenStable();
    el.querySelector<HTMLButtonElement>('button.ingresar')!.click();
    expect(localStorage.getItem('soma.ultimoCorreo')).toBe('a@b.co');
    expect(JSON.stringify(localStorage)).not.toContain('secreta');
  });
});
