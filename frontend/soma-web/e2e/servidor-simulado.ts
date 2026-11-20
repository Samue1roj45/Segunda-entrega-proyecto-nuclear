import { Page, Route } from '@playwright/test';

/** Estado de un "backend" mínimo y coherente: lo que el docente hace lo ve el estudiante y al revés. */
export class Estado {
  sesionCreada = false;
  iniciada = false;
  participacion: { id: number; estado: string; respuesta: number | null } | null = null;
  oficial = false;
  publicada = false;
  casoEliminado = false;
  avisosLeidos = false;
  llamadas: string[] = [];
}

const DOCENTE = { token: 't-doc', correoInstitucional: 'carlos.gomez@institucion.edu.co', nombreCompleto: 'Carlos Gómez', rol: 'DOCENTE', expiraEn: '2099-01-01T00:00:00Z',
  permisos: ['GESTIONAR_CASOS', 'PROGRAMAR_SESIONES', 'CONSULTAR_PARTICIPACIONES', 'DESCARGAR_REPORTES', 'REGISTRAR_RETROALIMENTACION', 'AUTORIZAR_REPETICION', 'ELIMINAR_CASOS'] };
const ESTUDIANTE = { token: 't-est', correoInstitucional: 'ana.perez@institucion.edu.co', nombreCompleto: 'Ana Pérez', rol: 'ESTUDIANTE', expiraEn: '2099-01-01T00:00:00Z',
  permisos: ['PARTICIPAR_EN_CASOS', 'CONSULTAR_RESULTADOS_PROPIOS'] };

const CASO = { id: 1, nombre: 'Conformidad en el grupo de trabajo', estado: 'PUBLICADO', tiempoEstimadoMinutos: 40, fechaActualizacion: '2026-11-10T10:00:00', fechaPublicacion: '2026-11-10T10:00:00' };
const ESTUDIANTES = [{ id: 11, nombreCompleto: 'Ana Pérez', correoInstitucional: 'ana.perez@institucion.edu.co' }, { id: 12, nombreCompleto: 'Luis Ríos', correoInstitucional: 'luis.rios@institucion.edu.co' }];

export async function instalarServidorSimulado(page: Page, e: Estado): Promise<void> {
  const json = (r: Route, cuerpo: unknown, status = 200): Promise<void> => r.fulfill({ status, contentType: 'application/json', body: JSON.stringify(cuerpo) });
  const sesion = (): Record<string, unknown> => ({
    idPractica: 1, idCaso: 1, nombreCaso: CASO.nombre, espacioAcademico: 'Psicología Social I', fechaInicio: '2026-11-20T14:00:00', fechaFin: '2026-11-20T16:00:00',
    tiempoMaximoMinutos: 45, lugar: null, mensajePersonalizado: null, urlAcceso: null, estado: e.iniciada ? 'EN_CURSO' : 'SIN_INICIAR',
    fechaInicioReal: e.iniciada ? '2026-11-20T14:01:00' : null, fechaCierreReal: null, tipoCierre: null, accionesPermitidas: e.iniciada ? ['FINALIZAR'] : ['INICIAR'],
  });
  const resultado = (): Record<string, unknown> => ({
    id: 5, participacionId: 9, numeroIntento: 1, estudianteId: 11, estudianteCorreo: ESTUDIANTE.correoInstitucional, estudianteNombre: 'Ana Pérez', practicaId: 1, practicaNombre: CASO.nombre,
    estadoParticipacion: 'FINALIZADA', correctas: 1, incorrectas: 0, noRespondidas: 0, pesoObtenido: 1, pesoTotal: 1, escalaMaxima: 5, notaFinal: 5, oficial: e.oficial, notaOficial: e.oficial ? 5 : null,
    fechaCalculo: '2026-11-20T14:30:00', detallePreguntas: [{ preguntaId: 1, enunciado: '¿Qué hace la persona ante la presión del grupo?', peso: 1, respondida: true, respuestaElegidaId: 101, correcta: true }],
  });

  await page.route('**/api/**', async (r) => {
    const u = new URL(r.request().url()); const m = r.request().method(); const ruta = u.pathname;
    e.llamadas.push(`${m} ${ruta}`);
    if (ruta === '/api/auth/personal/login') return json(r, DOCENTE);
    if (ruta === '/api/auth/estudiantes/login') return json(r, ESTUDIANTE);
    if (ruta === '/api/avisos' && m === 'GET') return json(r, { noLeidos: e.avisosLeidos ? 0 : 1, avisos: [{ id: 1, tipo: 'INGRESO_ESTUDIANTE', titulo: 'Un estudiante ingresó', mensaje: 'Ana Pérez ingresó a la plataforma.', leido: e.avisosLeidos, fecha: '2026-11-19T10:00:00' }] });
    if (ruta === '/api/avisos/leidos' && m === 'POST') { e.avisosLeidos = true; return r.fulfill({ status: 204 }); }
    if (ruta === '/api/estudiantes') return json(r, ESTUDIANTES);
    if (ruta === '/api/casos' && m === 'GET') return json(r, e.casoEliminado ? [] : [CASO]);
    if (ruta === '/api/casos/1' && m === 'DELETE') { e.casoEliminado = true; return r.fulfill({ status: 204 }); }
    if (ruta === '/api/casos/papelera' && m === 'GET') return json(r, e.casoEliminado ? [{ id: 1, nombre: CASO.nombre, estado: 'BORRADOR', fechaEliminacion: '2026-11-19T10:00:00' }] : []);
    if (ruta === '/api/casos/1/restauracion' && m === 'POST') { e.casoEliminado = false; return r.fulfill({ status: 204 }); }
    if (ruta === '/api/sesiones' && m === 'GET') return json(r, e.sesionCreada ? [sesion()] : []);
    if (ruta === '/api/sesiones' && m === 'POST') { e.sesionCreada = true; return json(r, { idPractica: 1, convocados: [] }, 201); }
    if (ruta === '/api/sesiones/1') return json(r, sesion());
    if (ruta === '/api/sesiones/1/estudiantes') return json(r, e.sesionCreada ? [{ idAutorizacion: 21, idEstudiante: 11, nombre: 'Ana Pérez', correo: ESTUDIANTE.correoInstitucional, estado: 'ACTIVA' }] : []);
    if (ruta === '/api/sesiones/1/iniciar') { e.iniciada = true; return json(r, sesion()); }
    if (ruta === '/api/notificaciones') return json(r, []);
    if (ruta === '/api/autorizaciones' ) return json(r, e.iniciada ? [{ idAutorizacion: 21, idPractica: 1, idCaso: 1, nombreCaso: CASO.nombre, espacioAcademico: 'Psicología Social I', fechaInicio: '2026-11-20T14:00:00', fechaFin: '2026-11-20T16:00:00', tiempoMaximoMinutos: 45, lugar: null, mensajePersonalizado: null, urlAcceso: null, estadoSesion: 'EN_CURSO', intentosUsados: e.participacion ? 1 : 0, intentosPermitidos: 1 }] : []);
    if (ruta === '/api/autorizaciones/21/ingreso') return json(r, { permitido: true, motivo: null, limiteIngreso: null, ahora: '2026-11-20T14:05:00', estadoSesion: 'EN_CURSO' });
    if (ruta === '/api/participaciones/iniciar') { e.participacion = { id: 9, estado: 'EN_CURSO', respuesta: null }; return json(r, { id: 9, practicaId: 1, estado: 'EN_CURSO' }, 201); }
    if (ruta === '/api/participaciones/9/contexto') return json(r, { nombreCaso: CASO.nombre, contextoHistoria: 'Un grupo de estudio decide una tarea.', desarrolloSituacional: null,
      escenarios: [{ idEscenario: 1, orden: 1, nombre: 'La reunión', contexto: 'Todos opinan igual menos una persona.', preguntas: [{ idPregunta: 1, orden: 1, enunciado: '¿Qué hace la persona ante la presión del grupo?', idRespuestaSeleccionada: e.participacion?.respuesta ?? null,
        opciones: [{ idRespuesta: 101, codigo: 'A', texto: 'Mantiene su postura con argumentos' }, { idRespuesta: 102, codigo: 'B', texto: 'Cede sin discutir' }] }] }] });
    if (ruta === '/api/participaciones/9/herramientas') return json(r, []);
    if (ruta === '/api/participaciones/9/progreso') return json(r, { idParticipacion: 9, totalPreguntas: 1, preguntasRespondidas: e.participacion?.respuesta ? 1 : 0, porcentajeProgreso: e.participacion?.respuesta ? 100 : 0 });
    if (ruta === '/api/participaciones/9/tiempo') return json(r, { idParticipacion: 9, estado: e.participacion?.estado ?? 'EN_CURSO', horaInicio: '2026-11-20T14:05:00', limite: '2026-11-20T14:50:00', segundosRestantes: 2400, expirada: false });
    if (ruta === '/api/participaciones/9/preguntas/1/respuesta') { e.participacion!.respuesta = 101; return json(r, {}); }
    if (ruta === '/api/participaciones/9/finalizar') { e.participacion!.estado = 'FINALIZADA'; return json(r, { idParticipacion: 9, estado: 'FINALIZADA', horaFin: '2026-11-20T14:30:00' }); }
    if (ruta === '/api/participaciones') return json(r, e.participacion ? [{ id: 9, practicaId: 1, practicaNombre: CASO.nombre, estudianteId: 11, estudianteCorreo: ESTUDIANTE.correoInstitucional, estudianteNombre: 'Ana Pérez', autorizacionId: 21, numeroIntento: 1, inicio: '2026-11-20T14:05:00', fin: null, tiempoUsadoSeg: 0, estado: e.participacion.estado }] : []);
    if (ruta === '/api/resultados' ) return json(r, e.participacion?.estado === 'FINALIZADA' ? [resultado()] : []);
    if (ruta === '/api/resultados/5') return json(r, resultado());
    if (ruta === '/api/resultados/notas-oficiales') return json(r, e.oficial ? [{ id: 1, practicaId: 1, estudianteId: 11, participacionId: 9, numeroIntento: 1, notaOficial: 5, docenteId: 2, fechaSeleccion: '2026-11-20T15:00:00' }] : []);
    if (ruta === '/api/resultados/nota-oficial') { e.oficial = true; return json(r, {}, 201); }
    if (ruta === '/api/resultados/practica/1/publicar-calificaciones') { e.publicada = true; return json(r, { enviadas: 1, omitidasDuplicado: 0, errores: 0 }); }
    if (ruta === '/api/retroalimentaciones') return json(r, []);
    return json(r, { mensaje: `Sin simular: ${m} ${ruta}` }, 404);
  });
}
