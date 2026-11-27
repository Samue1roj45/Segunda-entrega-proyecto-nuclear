import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface SesionEcos { sesionId: string; codigo: string; enlace: string; venceEn: string; intento: number; }
export interface IntentoEcos {
  sesionId: string; intento: number; estado: 'CREADA' | 'CANJEADA' | 'CERRADA' | 'VENCIDA'; creada: string;
  puntaje: number | null; nota: number | null; nivel: string | null; desenlace: string | null; fin: string | null;
  /** Nota oficial: el mejor intento (a igual nota, el primero). */
  oficial: boolean;
}
export interface EstadoEcos { caso: string | null; disponible: boolean; maxIntentos: number; usados: number; restantes: number; intentos: IntentoEcos[]; }
export type EstadoJuego = 'SIN_JUGAR' | 'CODIGO_GENERADO' | 'JUGANDO' | 'TERMINADO';
export interface EstudianteDocente {
  id: number; nombre: string; correo: string; vinculadoEn: string; intentosUsados: number; intentosMaximos: number;
  repeticiones: number; mejorNota: number | null; nivel: string | null; estadoActual: EstadoJuego;
}

/** Contrato ecos-soma/1 desde la web: crear la sesión de juego, ver intentos y (docente) resultados. El juego habla directo con el backend. */
@Injectable({ providedIn: 'root' })
export class EcosService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/ecos/v1';

  estado(): Observable<EstadoEcos> { return this.http.get<EstadoEcos>(`${this.base}/mis-intentos`); }
  crearSesion(): Observable<SesionEcos> { return this.http.post<SesionEcos>(`${this.base}/sesiones`, null); }
  misEstudiantes(): Observable<EstudianteDocente[]> { return this.http.get<EstudianteDocente[]>(`${this.base}/mis-estudiantes`); }
  retirarEstudiante(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/mis-estudiantes/${id}`); }
  concederRepeticion(id: number): Observable<EstudianteDocente> { return this.http.post<EstudianteDocente>(`${this.base}/mis-estudiantes/${id}/repeticion`, null); }
  /** Crea en los casos del docente el caso «Ecos de una decisión» (borrador) con la información del catálogo. */
  crearCaso(): Observable<{ caso: { id: number } }> { return this.http.post<{ caso: { id: number } }>(`${this.base}/crear-caso`, null); }
  cargarCatalogo(catalogo: unknown): Observable<{ casoVersion: string; creado: boolean }> {
    return this.http.post<{ casoVersion: string; creado: boolean }>(`${this.base}/catalogo`, catalogo);
  }
}
