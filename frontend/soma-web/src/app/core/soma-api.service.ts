import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

/** GET /api/sesiones (módulo 3, RF-18). */
export interface Sesion {
  readonly idPractica: number;
  readonly idCaso: number;
  readonly nombreCaso: string;
  readonly espacioAcademico: string;
  readonly fechaInicio: string;
  readonly fechaFin: string;
  readonly tiempoMaximoMinutos: number;
  readonly lugar: string | null;
  readonly estado: 'SIN_INICIAR' | 'EN_CURSO' | 'FINALIZADO';
  readonly accionesPermitidas: readonly string[];
}

/** GET /api/casos (módulo 2). */
export interface CasoResumen {
  readonly id: number;
  readonly nombre: string;
  readonly tiempoEstimadoMinutos: number | null;
  readonly estado: 'BORRADOR' | 'PUBLICADO';
  readonly fechaActualizacion: string | null;
  readonly fechaPublicacion: string | null;
}

/** GET /api/participaciones (módulo 5). */
export interface Participacion {
  readonly id: number;
  readonly practicaNombre: string;
  readonly estudianteNombre: string;
  readonly numeroIntento: number;
  readonly inicio: string | null;
  readonly estado: string;
}

/** GET /api/resultados (módulo 5). */
export interface Resultado {
  readonly id: number;
  readonly practicaNombre: string;
  readonly estudianteNombre: string;
  readonly correctas: number;
  readonly incorrectas: number;
  readonly noRespondidas: number;
  readonly escalaMaxima: number;
  readonly notaFinal: number;
}

/** Lecturas que muestra el panel. Cada endpoint sigue protegido por permisos en el servidor. */
@Injectable({ providedIn: 'root' })
export class SomaApiService {
  private readonly http = inject(HttpClient);

  sesiones(): Observable<Sesion[]> {
    return this.http.get<Sesion[]>('/api/sesiones');
  }

  casos(): Observable<CasoResumen[]> {
    return this.http.get<CasoResumen[]>('/api/casos');
  }

  participaciones(): Observable<Participacion[]> {
    return this.http.get<Participacion[]>('/api/participaciones');
  }

  resultados(): Observable<Resultado[]> {
    return this.http.get<Resultado[]>('/api/resultados');
  }

  iniciarSesion(id: number): Observable<Sesion> {
    return this.http.post<Sesion>(`/api/sesiones/${id}/iniciar`, null);
  }

  finalizarSesion(id: number): Observable<Sesion> {
    return this.http.post<Sesion>(`/api/sesiones/${id}/finalizar`, null);
  }
}
