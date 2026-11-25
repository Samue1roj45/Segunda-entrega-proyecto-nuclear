import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface SesionEcos { sesionId: string; codigo: string; enlace: string; venceEn: string; intento: number; }
export interface IntentoEcos {
  sesionId: string; intento: number; estado: 'CREADA' | 'CANJEADA' | 'CERRADA' | 'VENCIDA'; creada: string;
  puntaje: number | null; nota: number | null; nivel: string | null; desenlace: string | null; fin: string | null;
}
export interface EstadoEcos { caso: string | null; disponible: boolean; maxIntentos: number; usados: number; restantes: number; intentos: IntentoEcos[]; }
export interface ResultadoEcosDocente {
  sesionId: string; estudiante: string; intento: number; puntaje: number; nota: number; nivel: string; desenlace: string;
  protagonista: string; segundos: number; revision: boolean; fin: string; fortalezas: string[]; mejoras: string[];
}

/** Contrato ecos-soma/1 desde la web: crear la sesión de juego, ver intentos y (docente) resultados. El juego habla directo con el backend. */
@Injectable({ providedIn: 'root' })
export class EcosService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/ecos/v1';

  estado(): Observable<EstadoEcos> { return this.http.get<EstadoEcos>(`${this.base}/mis-intentos`); }
  crearSesion(): Observable<SesionEcos> { return this.http.post<SesionEcos>(`${this.base}/sesiones`, null); }
  resultadosDocente(): Observable<ResultadoEcosDocente[]> { return this.http.get<ResultadoEcosDocente[]>(`${this.base}/resultados`); }
  cargarCatalogo(catalogo: unknown): Observable<{ casoVersion: string; creado: boolean }> {
    return this.http.post<{ casoVersion: string; creado: boolean }>(`${this.base}/catalogo`, catalogo);
  }
}
