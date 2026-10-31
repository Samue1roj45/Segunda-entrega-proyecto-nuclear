import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '../../../environments/environment';
import { Paginated } from '../models/academico.model';
import {
  Participacion,
  ProgresoParticipacion,
  Resultado,
} from '../models/practicas.model';

@Injectable({ providedIn: 'root' })
export class SimulacionService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  listarParticipaciones(params: Record<string, string | number> = {}) {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        query.set(key, String(value));
      }
    });
    const suffix = query.toString() ? `?${query.toString()}` : '';
    return this.http.get<Paginated<Participacion>>(`${this.api}/participaciones/${suffix}`);
  }

  iniciarParticipacion() {
    return this.http.post<Participacion>(`${this.api}/participaciones/iniciar/`, {});
  }
  obtenerParticipacion(id: number) {
    return this.http.get<Participacion>(`${this.api}/participaciones/${id}/`);
  }
  responder(id: number, pregunta_id: number, respuesta_id: number) {
    return this.http.post<{ detail: string }>(
      `${this.api}/participaciones/${id}/responder/`,
      { pregunta_id, respuesta_id },
    );
  }
  finalizar(id: number) {
    return this.http.post<{ detail: string; resultado_id: number }>(
      `${this.api}/participaciones/${id}/finalizar/`, {},
    );
  }
  progreso(id: number) {
    return this.http.get<ProgresoParticipacion>(`${this.api}/participaciones/${id}/progreso/`);
  }

  // Resultados
  listarResultados(page = 1) {
    return this.http.get<Paginated<Resultado>>(`${this.api}/resultados/?page=${page}`);
  }
  obtenerResultado(id: number) {
    return this.http.get<Resultado>(`${this.api}/resultados/${id}/`);
  }
  guardarFeedback(id: number, feedback_docente: string) {
    return this.http.post<Resultado>(
      `${this.api}/resultados/${id}/feedback-docente/`,
      { feedback_docente },
    );
  }
}
