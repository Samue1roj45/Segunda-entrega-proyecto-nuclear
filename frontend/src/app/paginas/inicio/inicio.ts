import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OPCIONES_MENU } from '../../core/menu';
import { Rol } from '../../core/modelos/rol';
import { SesionService } from '../../core/sesion.service';

const NOMBRE_ROL: Readonly<Record<Rol, string>> = {
  [Rol.Estudiante]: 'Estudiante',
  [Rol.Docente]: 'Docente',
  [Rol.Administrador]: 'Administrador',
};

/** Inicio después de autenticarse: muestra solo las opciones que permiten el rol y los permisos (RF-02). */
@Component({
  selector: 'app-inicio',
  imports: [RouterLink],
  template: `
    @if (sesiones.sesion(); as sesion) {
      <div class="pila">
        <div>
          <h1>Hola, {{ sesion.nombreCompleto }}</h1>
          <p class="suave">{{ nombreRol() }} · {{ sesion.correoInstitucional }}</p>
        </div>

        <div class="opciones">
          @for (opcion of opciones(); track opcion.permiso) {
            <div class="tarjeta pila-corta opcion-menu">
              <h2>{{ opcion.titulo }}</h2>
              <p class="suave">{{ opcion.descripcion }}</p>
              @if (opcion.ruta) {
                <a class="ir" [routerLink]="opcion.ruta">Abrir</a>
              } @else {
                <span class="estado pendiente">Disponible al integrar el módulo {{ opcion.modulo }}</span>
              }
            </div>
          } @empty {
            <p class="vacio">Su cuenta aún no tiene permisos asignados. Solicítelos al administrador.</p>
          }
        </div>
      </div>
    }
  `,
  styles: `
    .opciones { display: grid; grid-template-columns: repeat(auto-fill, minmax(15rem, 1fr)); gap: 1rem; }
    .opcion-menu h2 { margin: 0; }
    .ir { font-weight: 600; margin-top: auto; }
    .pendiente { color: var(--texto-suave); background: var(--gris-fondo); align-self: flex-start; }
  `,
})
export class Inicio {
  protected readonly sesiones = inject(SesionService);
  protected readonly opciones = computed(() =>
    OPCIONES_MENU.filter((opcion) => this.sesiones.tienePermiso(opcion.permiso)),
  );
  protected readonly nombreRol = computed(() => {
    const rol = this.sesiones.rol();
    return rol ? NOMBRE_ROL[rol] : '';
  });
}
