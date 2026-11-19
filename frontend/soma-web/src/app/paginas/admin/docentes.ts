import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mensajeDeError } from '../../core/con-operaciones';
import { Docente } from '../../core/modelos/dominio';
import { Permiso } from '../../core/modelos/permiso';
import { SomaApiService } from '../../core/soma-api.service';

const PERMISOS_DOCENTE: { clave: Permiso; titulo: string; ayuda: string }[] = [
  { clave: Permiso.GestionarCasos, titulo: 'Gestionar casos', ayuda: 'Crear, editar, publicar y archivar casos.' },
  { clave: Permiso.ProgramarSesiones, titulo: 'Programar sesiones', ayuda: 'Convocar estudiantes e iniciar o cerrar sesiones.' },
  { clave: Permiso.ConsultarParticipaciones, titulo: 'Consultar participaciones', ayuda: 'Ver intentos, resultados y notas oficiales.' },
  { clave: Permiso.DescargarReportes, titulo: 'Descargar reportes', ayuda: 'Exportar la evaluación en PDF y Excel.' },
  { clave: Permiso.RegistrarRetroalimentacion, titulo: 'Retroalimentar', ayuda: 'Escribir comentarios a los estudiantes.' },
  { clave: Permiso.AutorizarRepeticion, titulo: 'Autorizar repeticiones', ayuda: 'Conceder intentos adicionales.' },
];

/** Administración de docentes (RF-02): alta de docentes y asignación de permisos. Solo el administrador. */
@Component({
  selector: 'app-docentes',
  imports: [FormsModule],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2>Docentes</h2></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (aviso(); as a) { <p class="aviso-ok" role="status">{{ a }}</p> }

      <section class="cristal bloque">
        <form class="formulario" (ngSubmit)="crear()">
          <h3>Nuevo docente</h3>
          <div class="rejilla-2">
            <label class="campo-f">Nombre completo<input name="n" [(ngModel)]="nombre" required maxlength="150" /></label>
            <label class="campo-f">Correo institucional<input name="c" type="email" [(ngModel)]="correo" required maxlength="150" /></label>
          </div>
          <div class="acciones"><button class="boton-primario" type="submit" [disabled]="ocupado()">Crear docente</button></div>
        </form>
      </section>

      @if (cargando()) { <p class="suave">Cargando docentes…</p> }
      @else if (docentes().length === 0) { <p class="cristal vacio">Todavía no hay docentes registrados.</p> }
      @for (d of docentes(); track d.id) {
        <section class="cristal bloque">
          <div class="cabecera-pagina">
            <div><h3>{{ d.nombreCompleto }}</h3><p class="suave">{{ d.correoInstitucional }}</p></div>
            <span [class]="'etiqueta ' + (d.activo ? 'ok' : 'archivado')">{{ d.activo ? 'Activo' : 'Inactivo' }}</span>
          </div>
          <div class="rejilla-2">
            @for (p of permisos; track p.clave) {
              <label class="fila-item" style="cursor:pointer">
                <span class="texto"><strong>{{ p.titulo }}</strong><br /><span class="ayuda">{{ p.ayuda }}</span></span>
                <input type="checkbox" [checked]="tiene(d, p.clave)" (change)="alternar(d, p.clave, $any($event.target).checked)" />
              </label>
            }
          </div>
          <div class="acciones"><button type="button" class="boton-primario boton-chico" [disabled]="ocupado()" (click)="guardar(d)">Guardar permisos</button></div>
        </section>
      }
    </div>
  `,
})
export class Docentes implements OnInit {
  private readonly api = inject(SomaApiService);
  protected readonly permisos = PERMISOS_DOCENTE;
  protected readonly docentes = signal<Docente[]>([]);
  protected readonly cargando = signal(true);
  protected readonly ocupado = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected nombre = '';
  protected correo = '';
  private readonly cambios = new Map<number, Set<string>>();

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    this.api.docentes().subscribe({
      next: (d) => { this.docentes.set(d); this.cambios.clear(); this.cargando.set(false); },
      error: (e: HttpErrorResponse) => { this.cargando.set(false); this.error.set(mensajeDeError(e)); },
    });
  }

  private actuales(d: Docente): Set<string> {
    let s = this.cambios.get(d.id);
    if (!s) { s = new Set(d.permisos); this.cambios.set(d.id, s); }
    return s;
  }

  protected tiene(d: Docente, p: Permiso): boolean { return this.actuales(d).has(p); }
  protected alternar(d: Docente, p: Permiso, activo: boolean): void { const s = this.actuales(d); if (activo) { s.add(p); } else { s.delete(p); } }

  protected crear(): void {
    this.error.set(null); this.aviso.set(null); this.ocupado.set(true);
    this.api.crearDocente(this.nombre.trim(), this.correo.trim().toLowerCase()).subscribe({
      next: (d) => { this.ocupado.set(false); this.aviso.set(`Docente ${d.nombreCompleto} creado. Asígnale los permisos que necesita.`); this.nombre = ''; this.correo = ''; this.cargar(); },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
    });
  }

  protected guardar(d: Docente): void {
    this.error.set(null); this.aviso.set(null); this.ocupado.set(true);
    this.api.asignarPermisos(d.id, [...this.actuales(d)]).subscribe({
      next: () => { this.ocupado.set(false); this.aviso.set(`Permisos de ${d.nombreCompleto} actualizados.`); this.cargar(); },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
    });
  }
}
