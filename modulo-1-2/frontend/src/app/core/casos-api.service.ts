import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CasoDetalle,
  CasoResumen,
  DatosCaso,
  DatosCriterio,
  DatosRespuesta,
  EstadoCaso,
  InformeValidacion,
} from './modelos/caso';

/** Cliente de la API del Módulo 2. */
@Injectable({ providedIn: 'root' })
export class CasosApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/casos';

  listar(estado?: EstadoCaso): Observable<CasoResumen[]> {
    const params = estado ? new HttpParams().set('estado', estado) : undefined;
    return this.http.get<CasoResumen[]>(this.base, { params });
  }

  crear(datos: Partial<DatosCaso>): Observable<CasoDetalle> {
    return this.http.post<CasoDetalle>(this.base, datos);
  }

  consultar(idCaso: number): Observable<CasoDetalle> {
    return this.http.get<CasoDetalle>(`${this.base}/${idCaso}`);
  }

  actualizar(idCaso: number, datos: DatosCaso): Observable<CasoDetalle> {
    return this.http.put<CasoDetalle>(`${this.base}/${idCaso}`, datos);
  }

  agregarEscenario(idCaso: number, nombre: string | null, contexto: string | null): Observable<unknown> {
    return this.http.post(`${this.base}/${idCaso}/escenarios`, { nombre, contexto });
  }

  actualizarEscenario(idCaso: number, id: number, nombre: string | null, contexto: string | null): Observable<unknown> {
    return this.http.put(`${this.base}/${idCaso}/escenarios/${id}`, { nombre, contexto });
  }

  eliminarEscenario(idCaso: number, id: number): Observable<unknown> {
    return this.http.delete(`${this.base}/${idCaso}/escenarios/${id}`);
  }

  agregarPregunta(idCaso: number, idEscenario: number, enunciado: string | null): Observable<unknown> {
    return this.http.post(`${this.base}/${idCaso}/escenarios/${idEscenario}/preguntas`, { enunciado });
  }

  actualizarPregunta(idCaso: number, id: number, enunciado: string | null): Observable<unknown> {
    return this.http.put(`${this.base}/${idCaso}/preguntas/${id}`, { enunciado });
  }

  eliminarPregunta(idCaso: number, id: number): Observable<unknown> {
    return this.http.delete(`${this.base}/${idCaso}/preguntas/${id}`);
  }

  agregarRespuesta(idCaso: number, idPregunta: number, datos: DatosRespuesta): Observable<unknown> {
    return this.http.post(`${this.base}/${idCaso}/preguntas/${idPregunta}/respuestas`, datos);
  }

  actualizarRespuesta(idCaso: number, id: number, datos: DatosRespuesta): Observable<unknown> {
    return this.http.put(`${this.base}/${idCaso}/respuestas/${id}`, datos);
  }

  eliminarRespuesta(idCaso: number, id: number): Observable<unknown> {
    return this.http.delete(`${this.base}/${idCaso}/respuestas/${id}`);
  }

  marcarCorrecta(idCaso: number, idPregunta: number, idRespuesta: number): Observable<unknown> {
    return this.http.put(`${this.base}/${idCaso}/preguntas/${idPregunta}/respuesta-correcta`, { idRespuesta });
  }

  registrarRubrica(idCaso: number, nombre: string | null, descripcion: string | null): Observable<unknown> {
    return this.http.put(`${this.base}/${idCaso}/rubrica`, { nombre, descripcion });
  }

  retirarRubrica(idCaso: number): Observable<unknown> {
    return this.http.delete(`${this.base}/${idCaso}/rubrica`);
  }

  agregarCriterio(idCaso: number, datos: DatosCriterio): Observable<unknown> {
    return this.http.post(`${this.base}/${idCaso}/rubrica/criterios`, datos);
  }

  actualizarCriterio(idCaso: number, id: number, datos: DatosCriterio): Observable<unknown> {
    return this.http.put(`${this.base}/${idCaso}/rubrica/criterios/${id}`, datos);
  }

  eliminarCriterio(idCaso: number, id: number): Observable<unknown> {
    return this.http.delete(`${this.base}/${idCaso}/rubrica/criterios/${id}`);
  }

  validar(idCaso: number): Observable<InformeValidacion> {
    return this.http.get<InformeValidacion>(`${this.base}/${idCaso}/validacion`);
  }

  publicar(idCaso: number): Observable<CasoDetalle> {
    return this.http.post<CasoDetalle>(`${this.base}/${idCaso}/publicacion`, null);
  }

  archivar(idCaso: number): Observable<CasoDetalle> {
    return this.http.post<CasoDetalle>(`${this.base}/${idCaso}/archivo`, null);
  }
}
