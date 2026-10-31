import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { AuthService } from '../core/auth/auth.service';
import { Rol } from '../core/models/usuario.model';
import { Resultado } from '../core/models/practicas.model';
import { SimulacionService } from '../core/services/simulacion.service';

@Component({
  selector: 'app-resultados',
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    MatTableModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatSnackBarModule,
  ],
  template: `
    <section class="page">
      <header>
        <h1>{{ esEstudiante() ? 'Mis resultados' : 'Resultados' }}</h1>
        <p class="subtitle">
          @if (esEstudiante()) {
            Consulta tus calificaciones, detalle de respuestas y retroalimentación.
          } @else {
            Calificaciones por participación. Haz click para ver el detalle y dejar feedback.
          }
        </p>
      </header>

      @if (loading()) {
        <mat-progress-bar mode="indeterminate" />
      }

      @if (!loading() && resultadosVisibles().length === 0) {
        <p class="empty">
          @if (esEstudiante()) {
            Aún no tienes resultados publicados.
          } @else {
            Aún no hay resultados. Los estudiantes deben completar sus prácticas primero.
          }
        </p>
      }

      <mat-accordion>
        @for (r of resultadosVisibles(); track r.id) {
          <mat-expansion-panel>
            <mat-expansion-panel-header>
              <mat-panel-title>
                @if (esEstudiante()) {
                  {{ r.practica_nombre }}
                } @else {
                  {{ r.estudiante_nombre }} ({{ r.estudiante_correo }})
                }
              </mat-panel-title>
              <mat-panel-description>
                @if (esEstudiante()) {
                  <strong>{{ r.nota_final }}/100</strong> · {{ r.fecha_calculo | date:'mediumDate' }}
                } @else {
                  {{ r.practica_nombre }} · <strong>{{ r.nota_final }}/100</strong> ·
                  {{ r.correctas }} ok {{ r.incorrectas }} err {{ r.no_respondidas }} sin responder
                }
              </mat-panel-description>
            </mat-expansion-panel-header>

            <p class="meta">
              Calculado: {{ r.fecha_calculo | date:'medium' }} · Peso obtenido:
              {{ r.peso_obtenido }}/{{ r.peso_total }}
            </p>

            <h3>Detalle pregunta por pregunta</h3>
            <ul class="detalle">
              @for (d of r.detalle_preguntas; track d.pregunta_id; let i = $index) {
                <li>
                  <strong>{{ i + 1 }}. {{ d.enunciado }}</strong> (peso {{ d.peso }})
                  @if (d.respondida && d.respuesta_elegida) {
                    <div [class.ok]="d.respuesta_elegida.es_correcta" [class.err]="!d.respuesta_elegida.es_correcta">
                      Respuesta: {{ d.respuesta_elegida.texto }}
                      @if (d.respuesta_elegida.es_correcta) {
                        <span>Correcta</span>
                      } @else {
                        <span>Incorrecta</span>
                      }
                    </div>
                  } @else {
                    <div class="ne">No respondida</div>
                  }
                </li>
              }
            </ul>

            <h3>{{ esEstudiante() ? 'Retroalimentación' : 'Feedback docente' }}</h3>
            @if (esEstudiante()) {
              <p class="feedback-readonly">
                {{ r.feedback_docente || 'Aún no hay retroalimentación docente para este resultado.' }}
              </p>
            } @else {
              <mat-form-field appearance="outline" class="feedback">
                <textarea
                  matInput
                  rows="3"
                  [(ngModel)]="feedbackEdit[r.id]"
                  [placeholder]="r.feedback_docente || 'Escribe tu feedback aquí...'"></textarea>
              </mat-form-field>
              <button mat-flat-button color="primary" (click)="guardarFeedback(r)">
                <mat-icon>save</mat-icon>
                Guardar feedback
              </button>
            }
          </mat-expansion-panel>
        }
      </mat-accordion>
    </section>
  `,
  styles: [`
    .page {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    h1 {
      margin: 0;
      font-size: 1.5rem;
      font-weight: 500;
    }

    .subtitle {
      color: var(--mat-sys-on-surface-variant);
      margin: 0.25rem 0 0;
      font-size: 0.9rem;
    }

    .empty {
      padding: 2rem;
      text-align: center;
      color: var(--mat-sys-on-surface-variant);
      background: var(--mat-sys-surface-container);
      border-radius: 12px;
      margin: 0;
    }

    .meta {
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.85rem;
      margin: 0.5rem 0;
    }

    h3 {
      font-size: 1rem;
      font-weight: 500;
      margin: 1rem 0 0.5rem;
    }

    .detalle {
      padding-left: 1.25rem;
    }

    .detalle li {
      margin-bottom: 0.5rem;
    }

    .ok {
      color: var(--mat-sys-primary);
    }

    .err {
      color: var(--mat-sys-error);
    }

    .ne {
      color: var(--mat-sys-on-surface-variant);
      font-style: italic;
    }

    .feedback,
    .feedback-readonly {
      width: 100%;
    }

    .feedback-readonly {
      padding: 0.9rem 1rem;
      box-sizing: border-box;
      border-radius: 8px;
      background: var(--mat-sys-surface-container-low);
      color: var(--mat-sys-on-surface-variant);
      line-height: 1.5;
    }

    button {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
    }
  `],
})
export class Resultados implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly servicio = inject(SimulacionService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly resultados = signal<Resultado[]>([]);
  readonly feedbackEdit: Record<number, string> = {};

  readonly esEstudiante = computed(() => this.auth.rol() === Rol.Estudiante);
  readonly resultadosVisibles = computed(() => {
    if (!this.esEstudiante()) return this.resultados();
    const email = this.auth.usuario()?.email;
    return this.resultados().filter((resultado) => !email || resultado.estudiante_correo === email);
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.servicio.listarResultados().subscribe({
      next: (r) => {
        this.resultados.set(r.results);
        for (const res of r.results) this.feedbackEdit[res.id] = res.feedback_docente;
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  guardarFeedback(r: Resultado): void {
    if (this.esEstudiante()) return;
    const fb = this.feedbackEdit[r.id] ?? '';
    this.servicio.guardarFeedback(r.id, fb).subscribe({
      next: () => this.snackBar.open('Feedback guardado.', 'OK', { duration: 2500 }),
      error: () => this.snackBar.open('No se pudo guardar.', 'OK', { duration: 3500 }),
    });
  }
}
