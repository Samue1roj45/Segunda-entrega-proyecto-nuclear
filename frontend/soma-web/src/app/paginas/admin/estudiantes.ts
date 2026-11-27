import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { mensajeDeError } from '../../core/con-operaciones';
import { EstudianteAdmin, SomaApiService } from '../../core/soma-api.service';

/**
 * Registro de estudiantes (solo el administrador): nombre y correo institucional. El estudiante no tiene
 * contraseña; ingresa con su correo y el código que recibe cuando un docente lo convoca a una sesión (RF-04, RF-20).
 */
@Component({
  selector: 'app-estudiantes',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2 class="sr-only">Estudiantes</h2></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (aviso(); as a) { <p class="aviso-ok" role="status">{{ a }}</p> }

      <section class="cristal bloque">
        <form class="formulario" (ngSubmit)="crear()">
          <h3>Nuevo estudiante</h3>
          <p class="ayuda">Usa su correo institucional. No necesita contraseña: ingresará con ese correo y el código que recibe al ser convocado a una sesión.</p>
          <div class="rejilla-2">
            <label class="campo-f">Nombre completo<input name="n" [(ngModel)]="nombre" required maxlength="150" /></label>
            <label class="campo-f">Correo institucional<input name="c" type="email" [(ngModel)]="correo" required maxlength="150" /></label>
          </div>
          <div class="acciones"><button class="boton-primario" type="submit" [disabled]="ocupado()">Registrar estudiante</button></div>
        </form>
      </section>

      @if (cargando()) { <div class="esqueleto" role="status" aria-label="Cargando estudiantes"><i></i><i></i><i></i></div> }
      @else if (activos().length === 0) { <p class="cristal vacio">Todavía no hay estudiantes activos.</p> }
      @if (archivados() > 0) { <p class="aviso-info">{{ archivados() }} {{ archivados() === 1 ? 'estudiante archivado no aparece' : 'estudiantes archivados no aparecen' }} aquí. Consúltalos o reactívalos en <a routerLink="/usuarios">Usuarios</a>.</p> }
      @if (activos().length > 0) {
        <section class="cristal bloque">
          <h3>Estudiantes activos ({{ activos().length }})</h3>
          <ul class="lista-simple">
            @for (e of activos(); track e.id) {
              <li><strong>{{ e.nombreCompleto }}</strong><br /><span class="suave">{{ e.correoInstitucional }}</span></li>
            }
          </ul>
        </section>
      }
    </div>
  `,
})
export class Estudiantes implements OnInit {
  private readonly api = inject(SomaApiService);
  private readonly estudiantes = signal<EstudianteAdmin[]>([]);
  protected readonly activos = computed(() => this.estudiantes().filter((e) => e.activo));
  /** Los archivados no inician sesión: se gestionan en «Usuarios». */
  protected readonly archivados = computed(() => this.estudiantes().filter((e) => !e.activo).length);
  protected readonly cargando = signal(true);
  protected readonly ocupado = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected nombre = '';
  protected correo = '';

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    this.api.estudiantesAdmin().subscribe({
      next: (e) => { this.estudiantes.set(e); this.cargando.set(false); },
      error: (e: HttpErrorResponse) => { this.cargando.set(false); this.error.set(mensajeDeError(e)); },
    });
  }

  protected crear(): void {
    this.error.set(null); this.aviso.set(null); this.ocupado.set(true);
    this.api.crearEstudiante(this.nombre.trim(), this.correo.trim().toLowerCase()).subscribe({
      next: (e) => { this.ocupado.set(false); this.aviso.set(`Estudiante ${e.nombreCompleto} registrado. Los docentes ya pueden convocarlo a sus sesiones.`); this.nombre = ''; this.correo = ''; this.cargar(); },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
    });
  }
}
