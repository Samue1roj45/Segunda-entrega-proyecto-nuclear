import { Component, computed, inject } from '@angular/core';
import { Permiso } from '../../core/modelos/permiso';
import { SesionService } from '../../core/sesion.service';
import { ModoTema, TemaService } from '../../core/tema.service';
import { ToastService } from '../../core/toast.service';
import { ACENTOS, PreferenciasService, COLORES_AVATAR } from '../../core/preferencias.service';

const NOMBRES: Record<string, string> = {
  [Permiso.ParticiparEnCasos]: 'Participar en casos', [Permiso.ConsultarResultadosPropios]: 'Consultar mis resultados',
  [Permiso.GestionarCasos]: 'Gestionar casos', [Permiso.ProgramarSesiones]: 'Programar sesiones',
  [Permiso.ConsultarParticipaciones]: 'Consultar participaciones', [Permiso.DescargarReportes]: 'Descargar reportes',
  [Permiso.RegistrarRetroalimentacion]: 'Registrar retroalimentación', [Permiso.AutorizarRepeticion]: 'Autorizar repeticiones', [Permiso.EliminarCasos]: 'Eliminar casos',
  [Permiso.AdministrarDocentes]: 'Administrar docentes',
};

/** Perfil del usuario: sus datos de la sesión, sus permisos y preferencias de esta aplicación (se guardan en este navegador). */
@Component({
  selector: 'app-perfil',
  template: `
    <div class="pagina">
      <section class="cristal cabeza">
        <span class="avatar-grande" [style.background]="degradado()" aria-hidden="true">{{ iniciales() }}</span>
        <div>
          <h2>{{ sesion()?.nombreCompleto }}</h2>
          <p class="suave">{{ sesion()?.correoInstitucional }}</p>
          <span class="etiqueta">{{ rol() }}</span>
        </div>
      </section>

      <div class="rejilla">
        <section class="cristal bloque">
          <h3>Apariencia</h3>
          <div class="fila"><span>Modo</span>
            <div class="pestanas" role="group" aria-label="Modo de color">
              <button type="button" [class.activa]="temas.modo() === 'auto'" (click)="poner('auto', $event)" title="Sigue el modo de tu dispositivo">Automático</button>
              <button type="button" [class.activa]="temas.modo() === 'dia'" (click)="poner('dia', $event)">Día</button>
              <button type="button" [class.activa]="temas.modo() === 'noche'" (click)="poner('noche', $event)">Noche</button>
            </div>
          </div>
          <div class="fila"><span>Color de la plataforma</span>
            <div class="colores" role="group" aria-label="Color de la plataforma">
              @for (a of acentos; track a.id) {
                <button type="button" class="color" [class.sel]="prefs.acento() === $index" [style.background]="a.muestra" (click)="prefs.fijarAcento($index)" [attr.aria-label]="a.nombre" [title]="a.nombre"></button>
              }
            </div>
          </div>
          <div class="fila"><span>Color de tu avatar</span>
            <div class="colores" role="group" aria-label="Color del avatar">
              @for (c of colores; track $index) {
                <button type="button" class="color" [class.sel]="prefs.avatar() === $index" [style.background]="'linear-gradient(135deg,' + c[0] + ',' + c[1] + ')'" (click)="prefs.fijarAvatar($index)" [attr.aria-label]="'Color ' + ($index + 1)"></button>
              }
            </div>
          </div>
          <label class="fila interruptor"><span>Animación de bienvenida al entrar</span><input type="checkbox" [checked]="prefs.bienvenida()" (change)="prefs.fijarBienvenida($any($event.target).checked)" /></label>
          <label class="fila"><span>Tamaño de letra</span>
            <select [value]="prefs.letra()" (change)="prefs.fijarLetra(+$any($event.target).value)" aria-label="Tamaño de letra">
              <option value="0">Normal</option><option value="1">Grande</option><option value="2">Muy grande</option></select></label>
          <label class="fila interruptor"><span>Alto contraste</span><input type="checkbox" [checked]="prefs.contraste()" (change)="prefs.fijarContraste($any($event.target).checked)" /></label>
          <label class="fila interruptor"><span>Reducir animaciones</span><input type="checkbox" [checked]="prefs.sinMovimiento()" (change)="prefs.fijarSinMovimiento($any($event.target).checked)" /></label>
          <label class="fila interruptor"><span>Sonidos suaves (clics y avisos)</span><input type="checkbox" [checked]="prefs.sonido()" (change)="prefs.fijarSonido($any($event.target).checked)" /></label>
          <label class="fila interruptor"><span>Mostrar la campana de notificaciones</span><input type="checkbox" [checked]="prefs.campana()" (change)="prefs.fijarCampana($any($event.target).checked)" /></label>
          <p class="ayuda">Estas preferencias se guardan solo en este navegador.</p>
        </section>

        <section class="cristal bloque">
          <h3>Tus permisos</h3>
          <ul class="lista-simple">
            @for (p of permisos(); track p) { <li class="fila-item"><span class="texto">{{ p }}</span><span class="etiqueta ok">Activo</span></li> }
          </ul>
          <p class="ayuda">Los permisos los asigna el administrador de la plataforma. Para cambiar la contraseña o los datos de la cuenta, escribe al administrador: la plataforma todavía no ofrece ese cambio por sí misma.</p>
        </section>
      </div>
    </div>
  `,
  styles: `
    .cabeza { display: flex; align-items: center; gap: 1.4rem; padding: 1.6rem 1.8rem; flex-wrap: wrap; }
    .avatar-grande { width: 5.2rem; height: 5.2rem; border-radius: 50%; display: grid; place-items: center; color: #fff; font-size: 1.7rem; font-weight: 600; box-shadow: 0 0 0 4px rgba(255,255,255,.75), 0 16px 26px -14px rgba(var(--d-59_45_92),.7); }
    .rejilla { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 1.2rem; align-items: start; }
    .fila { display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
    .colores { display: flex; gap: .5rem; } .color { width: 1.9rem; height: 1.9rem; border-radius: 50%; border: 2px solid transparent; transition: transform .2s; }
    .color:hover { transform: scale(1.12); } .color.sel { border-color: #fff; box-shadow: 0 0 0 2px var(--lila-700); }
    .interruptor { cursor: pointer; } .interruptor input { width: 1.2rem; height: 1.2rem; accent-color: var(--lila-700); }
  `,
})
export class Perfil {
  protected readonly sesion = inject(SesionService).sesion;
  protected readonly temas = inject(TemaService);
  protected readonly prefs = inject(PreferenciasService);
  private readonly toasts = inject(ToastService);
  protected readonly colores = COLORES_AVATAR;

  protected readonly iniciales = computed(() => (this.sesion()?.nombreCompleto ?? '').split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join(''));
  protected readonly acentos = ACENTOS;
  protected readonly degradado = computed(() => { const c = COLORES_AVATAR[this.prefs.avatar()]; return `linear-gradient(135deg, ${c[0]}, ${c[1]})`; });
  protected readonly rol = computed(() => ({ ADMINISTRADOR: 'Administrador', DOCENTE: 'Docente', ESTUDIANTE: 'Estudiante' } as Record<string, string>)[this.sesion()?.rol ?? ''] ?? '');
  protected readonly permisos = computed(() => (this.sesion()?.permisos ?? []).map((p) => NOMBRES[p] ?? p));

  protected poner(t: ModoTema, e: MouseEvent): void {
    if (this.temas.modo() === t) return;
    const r = (e.currentTarget as HTMLElement).getBoundingClientRect();
    this.temas.alternar({ x: r.left + r.width / 2, y: r.top + r.height / 2 }, t);
    this.toasts.info(t === 'noche' ? 'Modo noche activado' : t === 'dia' ? 'Modo día activado' : 'El modo sigue a tu dispositivo');
  }
}
