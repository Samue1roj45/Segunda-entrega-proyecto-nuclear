import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import * as M from './modelos/dominio';

/** Quita los filtros vacíos antes de armar la consulta. */
function filtros(f: Record<string, string | number | null | undefined>): HttpParams {
  let p = new HttpParams();
  for (const [k, v] of Object.entries(f)) { if (v !== null && v !== undefined && v !== '') { p = p.set(k, String(v)); } }
  return p;
}

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

  // ---------- Casos (módulo 2) ----------
  casosPorEstado(estado?: string): Observable<CasoResumen[]> { return this.http.get<CasoResumen[]>('/api/casos', { params: filtros({ estado }) }); }
  caso(id: number): Observable<M.CasoDetalle> { return this.http.get<M.CasoDetalle>(`/api/casos/${id}`); }
  crearCaso(d: M.DatosCaso): Observable<M.CasoDetalle> { return this.http.post<M.CasoDetalle>('/api/casos', d); }
  editarCaso(id: number, d: M.DatosCaso): Observable<M.CasoDetalle> { return this.http.put<M.CasoDetalle>(`/api/casos/${id}`, d); }
  validarCaso(id: number): Observable<M.InformeValidacion> { return this.http.get<M.InformeValidacion>(`/api/casos/${id}/validacion`); }
  publicarCaso(id: number): Observable<M.CasoDetalle> { return this.http.post<M.CasoDetalle>(`/api/casos/${id}/publicacion`, null); }
  archivarCaso(id: number): Observable<M.CasoDetalle> { return this.http.post<M.CasoDetalle>(`/api/casos/${id}/archivo`, null); }
  eliminarCaso(id: number): Observable<unknown> { return this.http.delete(`/api/casos/${id}`); }
  exportarDsl(id: number): Observable<unknown> { return this.http.get<unknown>(`/api/casos/${id}/dsl`); }
  importarDsl(documento: unknown): Observable<M.ResultadoImportacion> { return this.http.post<M.ResultadoImportacion>('/api/casos/importacion-dsl', documento); }

  crearEscenario(caso: number, d: { nombre: string; contexto: string }): Observable<M.Escenario> { return this.http.post<M.Escenario>(`/api/casos/${caso}/escenarios`, d); }
  editarEscenario(caso: number, id: number, d: { nombre: string; contexto: string }): Observable<M.Escenario> { return this.http.put<M.Escenario>(`/api/casos/${caso}/escenarios/${id}`, d); }
  borrarEscenario(caso: number, id: number): Observable<void> { return this.http.delete<void>(`/api/casos/${caso}/escenarios/${id}`); }

  crearPregunta(caso: number, escenario: number, enunciado: string): Observable<M.Pregunta> { return this.http.post<M.Pregunta>(`/api/casos/${caso}/escenarios/${escenario}/preguntas`, { enunciado }); }
  editarPregunta(caso: number, id: number, enunciado: string): Observable<M.Pregunta> { return this.http.put<M.Pregunta>(`/api/casos/${caso}/preguntas/${id}`, { enunciado }); }
  borrarPregunta(caso: number, id: number): Observable<void> { return this.http.delete<void>(`/api/casos/${caso}/preguntas/${id}`); }

  crearRespuesta(caso: number, pregunta: number, d: Partial<M.Respuesta>): Observable<M.Respuesta> { return this.http.post<M.Respuesta>(`/api/casos/${caso}/preguntas/${pregunta}/respuestas`, d); }
  editarRespuesta(caso: number, id: number, d: Partial<M.Respuesta>): Observable<M.Respuesta> { return this.http.put<M.Respuesta>(`/api/casos/${caso}/respuestas/${id}`, d); }
  borrarRespuesta(caso: number, id: number): Observable<void> { return this.http.delete<void>(`/api/casos/${caso}/respuestas/${id}`); }
  marcarCorrecta(caso: number, pregunta: number, idRespuesta: number): Observable<unknown> { return this.http.put(`/api/casos/${caso}/preguntas/${pregunta}/respuesta-correcta`, { idRespuesta }); }
  guardarJustificacion(caso: number, idRespuesta: number, justificacion: string): Observable<unknown> { return this.http.put(`/api/casos/${caso}/respuestas/${idRespuesta}/justificacion`, { justificacion }); }

  rubrica(caso: number): Observable<M.Rubrica> { return this.http.get<M.Rubrica>(`/api/casos/${caso}/rubrica`); }
  guardarRubrica(caso: number, d: { nombre: string; descripcion: string }): Observable<M.Rubrica> { return this.http.put<M.Rubrica>(`/api/casos/${caso}/rubrica`, d); }
  borrarRubrica(caso: number): Observable<void> { return this.http.delete<void>(`/api/casos/${caso}/rubrica`); }
  crearCriterio(caso: number, d: { criterio: string; parametros: string[]; rango: M.Rango }): Observable<M.Criterio> { return this.http.post<M.Criterio>(`/api/casos/${caso}/rubrica/criterios`, d); }
  editarCriterio(caso: number, id: number, d: { criterio: string; parametros: string[]; rango: M.Rango }): Observable<M.Criterio> { return this.http.put<M.Criterio>(`/api/casos/${caso}/rubrica/criterios/${id}`, d); }
  borrarCriterio(caso: number, id: number): Observable<void> { return this.http.delete<void>(`/api/casos/${caso}/rubrica/criterios/${id}`); }

  herramientasCaso(caso: number): Observable<M.Herramienta[]> { return this.http.get<M.Herramienta[]>(`/api/casos/${caso}/herramientas`); }
  agregarHerramienta(caso: number, d: { nombre: string; tipo: M.TipoHerramienta; descripcion: string | null; contenido: string | null; orden: number | null }): Observable<M.Herramienta> { return this.http.post<M.Herramienta>(`/api/casos/${caso}/herramientas`, d); }
  quitarHerramienta(caso: number, id: number): Observable<void> { return this.http.delete<void>(`/api/casos/${caso}/herramientas/${id}`); }

  // ---------- Sesiones (módulo 3) ----------
  sesionDetalle(id: number): Observable<M.SesionDetalle> { return this.http.get<M.SesionDetalle>(`/api/sesiones/${id}`); }
  sesionesDetalle(): Observable<M.SesionDetalle[]> { return this.http.get<M.SesionDetalle[]>('/api/sesiones'); }
  agendarSesion(d: M.AgendarSesion): Observable<M.SesionCreada> { return this.http.post<M.SesionCreada>('/api/sesiones', d); }
  modificarSesion(id: number, d: Omit<M.AgendarSesion, 'idCaso' | 'idsEstudiantes'>): Observable<M.SesionDetalle> { return this.http.put<M.SesionDetalle>(`/api/sesiones/${id}`, d); }
  convocados(id: number): Observable<M.Convocado[]> { return this.http.get<M.Convocado[]>(`/api/sesiones/${id}/estudiantes`); }
  agregarConvocado(id: number, estudiante: number): Observable<unknown> { return this.http.post(`/api/sesiones/${id}/estudiantes/${estudiante}`, null); }
  retirarConvocado(id: number, estudiante: number): Observable<void> { return this.http.delete<void>(`/api/sesiones/${id}/estudiantes/${estudiante}`); }
  intentos(autorizacion: number): Observable<M.Intentos> { return this.http.get<M.Intentos>(`/api/autorizaciones/${autorizacion}/intentos`); }
  autorizarRepeticion(autorizacion: number, motivo: string | null): Observable<M.Intentos> { return this.http.post<M.Intentos>(`/api/autorizaciones/${autorizacion}/repeticiones`, motivo ? { motivo } : {}); }

  // ---------- Estudiantes (para convocar) ----------
  estudiantes(): Observable<M.EstudianteResumen[]> { return this.http.get<M.EstudianteResumen[]>('/api/estudiantes'); }

  // ---------- Notificaciones ----------
  avisos(): Observable<{ avisos: M.Aviso[]; noLeidos: number }> { return this.http.get<{ avisos: M.Aviso[]; noLeidos: number }>('/api/avisos'); }
  marcarAvisoLeido(id: number): Observable<unknown> { return this.http.post(`/api/avisos/${id}/leido`, null); }
  marcarAvisosLeidos(): Observable<unknown> { return this.http.post('/api/avisos/leidos', null); }
  auditoria(limite = 100): Observable<M.EntradaAuditoria[]> { return this.http.get<M.EntradaAuditoria[]>('/api/auditoria', { params: { limite } }); }
  notificaciones(practica?: number): Observable<M.Notificacion[]> { return this.http.get<M.Notificacion[]>('/api/notificaciones', { params: filtros({ practica }) }); }
  enviarConvocatoria(practica: number): Observable<M.ResumenEnvio> { return this.http.post<M.ResumenEnvio>(`/api/notificaciones/practica/${practica}/convocatoria`, null); }
  notificarCambio(practica: number, motivo: string): Observable<M.ResumenEnvio> { return this.http.post<M.ResumenEnvio>(`/api/notificaciones/practica/${practica}/cambio`, { motivo }); }

  // ---------- Evaluación (módulo 5) ----------
  participacionesFiltradas(f: { practica?: number | null; estudiante?: number | null; estado?: string | null; numeroIntento?: number | null; fechaDesde?: string | null; fechaHasta?: string | null }): Observable<M.ParticipacionLista[]> {
    return this.http.get<M.ParticipacionLista[]>('/api/participaciones', { params: filtros(f) });
  }
  resultadosFiltrados(practica?: number | null, estudiante?: number | null): Observable<M.ResultadoDetalle[]> {
    return this.http.get<M.ResultadoDetalle[]>('/api/resultados', { params: filtros({ practica, estudiante }) });
  }
  resultado(id: number): Observable<M.ResultadoDetalle> { return this.http.get<M.ResultadoDetalle>(`/api/resultados/${id}`); }
  notasOficiales(practica?: number | null, estudiante?: number | null): Observable<M.NotaOficial[]> {
    return this.http.get<M.NotaOficial[]>('/api/resultados/notas-oficiales', { params: filtros({ practica, estudiante }) });
  }
  elegirNotaOficial(practicaId: number, estudianteId: number, participacionId: number): Observable<M.NotaOficial> {
    return this.http.post<M.NotaOficial>('/api/resultados/nota-oficial', { practicaId, estudianteId, participacionId });
  }
  evaluarPendientes(practica: number): Observable<M.PendientesEvaluadas> { return this.http.post<M.PendientesEvaluadas>(`/api/resultados/practica/${practica}/evaluar-pendientes`, null); }
  publicarCalificaciones(practica: number): Observable<M.ResumenEnvio> { return this.http.post<M.ResumenEnvio>(`/api/resultados/practica/${practica}/publicar-calificaciones`, null); }
  retroalimentaciones(practica?: number | null, participacion?: number | null): Observable<M.Retroalimentacion[]> {
    return this.http.get<M.Retroalimentacion[]>('/api/retroalimentaciones', { params: filtros({ practica, participacion }) });
  }
  registrarRetroalimentacion(participacionId: number, texto: string): Observable<M.Retroalimentacion> {
    return this.http.post<M.Retroalimentacion>('/api/retroalimentaciones', { participacionId, texto });
  }
  /** Descarga de reportes: el navegador necesita el archivo como binario. */
  reporte(formato: 'pdf' | 'xlsx', practica?: number | null, estudiante?: number | null): Observable<Blob> {
    return this.http.get(`/api/reportes/evaluacion.${formato}`, { params: filtros({ practica, estudiante }), responseType: 'blob' });
  }

  // ---------- Administración ----------
  docentes(): Observable<M.Docente[]> { return this.http.get<M.Docente[]>('/api/admin/docentes'); }
  crearDocente(nombreCompleto: string, correoInstitucional: string): Observable<M.Docente> { return this.http.post<M.Docente>('/api/admin/docentes', { nombreCompleto, correoInstitucional }); }
  asignarPermisos(id: number, permisos: string[]): Observable<M.Docente> { return this.http.put<M.Docente>(`/api/admin/docentes/${id}/permisos`, { permisos }); }

  // ---------- Estudiante ----------
  convocatorias(): Observable<M.Convocatoria[]> { return this.http.get<M.Convocatoria[]>('/api/autorizaciones'); }
  estadoIngreso(autorizacion: number): Observable<M.EstadoIngreso> { return this.http.get<M.EstadoIngreso>(`/api/autorizaciones/${autorizacion}/ingreso`); }
  iniciarParticipacion(autorizacionId: number): Observable<M.ParticipacionLista> { return this.http.post<M.ParticipacionLista>('/api/participaciones/iniciar', { autorizacionId }); }
  contexto(participacion: number): Observable<M.Contexto> { return this.http.get<M.Contexto>(`/api/participaciones/${participacion}/contexto`); }
  responder(participacion: number, pregunta: number, idRespuesta: number): Observable<unknown> { return this.http.put(`/api/participaciones/${participacion}/preguntas/${pregunta}/respuesta`, { idRespuesta }); }
  progreso(participacion: number): Observable<M.Progreso> { return this.http.get<M.Progreso>(`/api/participaciones/${participacion}/progreso`); }
  tiempo(participacion: number): Observable<M.Tiempo> { return this.http.get<M.Tiempo>(`/api/participaciones/${participacion}/tiempo`); }
  herramientasParticipacion(participacion: number): Observable<M.Herramienta[]> { return this.http.get<M.Herramienta[]>(`/api/participaciones/${participacion}/herramientas`); }
  finalizarParticipacion(participacion: number): Observable<M.Finalizacion> { return this.http.post<M.Finalizacion>(`/api/participaciones/${participacion}/finalizar`, null); }
  participacion(id: number): Observable<M.ParticipacionLista> { return this.http.get<M.ParticipacionLista>(`/api/participaciones/${id}`); }
}
