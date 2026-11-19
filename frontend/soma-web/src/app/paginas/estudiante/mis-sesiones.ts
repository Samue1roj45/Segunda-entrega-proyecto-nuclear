import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { claseEstado, nombreEstado } from '../../compartido/formato';
import { mensajeDeError } from '../../core/con-operaciones';
import { Convocatoria, EstadoIngreso, ParticipacionLista } from '../../core/modelos/dominio';
import { SomaApiService } from '../../core/soma-api.service';

/** Convocatorias del estudiante (RF-20): a qué sesiones fue invitado, si puede entrar ahora y cuántos intentos le quedan. */
@Component({
  selector: 'app-mis-sesiones',
  imports: [DatePipe],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2 class="sr-only">Mis casos</h2></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (cargando()) { <p class="suave">Cargando tus convocatorias…</p> }
      @else if (lista().length === 0) { <p class="cristal vacio">Todavía no tienes sesiones asignadas. Cuando tu docente te convoque, aparecerán aquí.</p> }
      @else {
        <div class="tarjetas">
          @for (c of lista(); track c.idAutorizacion) {
            <article class="cristal tarjeta-item">
              <span [class]="'etiqueta ' + clase(c.estadoSesion)">{{ nombre(c.estadoSesion) }}</span>
              <h3>{{ c.nombreCaso }}</h3>
              <p class="suave">{{ c.espacioAcademico }}</p>
              <div class="meta">
                <span>{{ c.fechaInicio | date: 'd MMM y, HH:mm' }} → {{ c.fechaFin | date: 'd MMM, HH:mm' }}</span>
                <span>{{ c.tiempoMaximoMinutos }} min</span>
                @if (c.lugar) { <span>{{ c.lugar }}</span> }
                <span>Intentos {{ c.intentosUsados }} de {{ c.intentosPermitidos }}</span>
              </div>
              @if (c.mensajePersonalizado) { <p class="aviso-info">{{ c.mensajePersonalizado }}</p> }
              @if (c.urlAcceso) { <p class="ayuda">Enlace: <a [href]="c.urlAcceso" target="_blank" rel="noopener noreferrer">{{ c.urlAcceso }}</a></p> }
              @if (ingreso()[c.idAutorizacion]; as i) {
                @if (!i.permitido) { <p class="alerta-error">{{ i.motivo ?? 'Todavía no puedes ingresar.' }}</p> }
                @else if (i.limiteIngreso) { <p class="aviso-ok">Puedes ingresar hasta {{ i.limiteIngreso | date: 'HH:mm' }}.</p> }
              }
              <div class="acciones">
                @if (enCurso(c.idPractica); as p) {
                  <button type="button" class="boton-primario" (click)="continuar(p)">Continuar mi intento</button>
                } @else {
                  <button type="button" class="boton-primario" [disabled]="ocupado()" (click)="entrar(c)">
                    {{ c.intentosUsados > 0 ? 'Intentar de nuevo' : 'Comenzar' }}
                  </button>
                }
                <button type="button" class="boton-suave" [disabled]="ocupado()" (click)="consultar(c)">¿Puedo ingresar?</button>
              </div>
            </article>
          }
        </div>
      }
    </div>
  `,
})
export class MisSesiones implements OnInit {
  private readonly api = inject(SomaApiService);
  private readonly router = inject(Router);
  protected readonly lista = signal<Convocatoria[]>([]);
  protected readonly propias = signal<ParticipacionLista[]>([]);
  protected readonly ingreso = signal<Record<number, EstadoIngreso>>({});
  protected readonly cargando = signal(true);
  protected readonly ocupado = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly nombre = nombreEstado;
  protected readonly clase = claseEstado;

  ngOnInit(): void {
    this.api.convocatorias().subscribe({
      next: (c) => { this.lista.set(c); this.cargando.set(false); },
      error: (e: HttpErrorResponse) => { this.cargando.set(false); this.error.set(mensajeDeError(e)); },
    });
    this.api.participacionesFiltradas({}).subscribe({ next: (p) => this.propias.set(p), error: () => this.propias.set([]) });
  }

  protected enCurso(practica: number): number | null {
    return this.propias().find((p) => p.practicaId === practica && p.estado === 'EN_CURSO')?.id ?? null;
  }

  protected continuar(id: number): void { void this.router.navigate(['/participar', id]); }

  protected consultar(c: Convocatoria): void {
    this.error.set(null);
    this.api.estadoIngreso(c.idAutorizacion).subscribe({
      next: (i) => this.ingreso.update((m) => ({ ...m, [c.idAutorizacion]: i })),
      error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)),
    });
  }

  protected entrar(c: Convocatoria): void {
    this.error.set(null); this.ocupado.set(true);
    this.api.estadoIngreso(c.idAutorizacion).subscribe({
      next: (i) => {
        this.ingreso.update((m) => ({ ...m, [c.idAutorizacion]: i }));
        if (!i.permitido) { this.ocupado.set(false); return; }
        this.api.iniciarParticipacion(c.idAutorizacion).subscribe({
          next: (p) => { this.ocupado.set(false); void this.router.navigate(['/participar', p.id]); },
          error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
        });
      },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
    });
  }
}
