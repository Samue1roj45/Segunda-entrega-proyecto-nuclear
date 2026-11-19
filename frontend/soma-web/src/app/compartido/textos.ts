/**
 * Textos y formatos de uso común en un solo lugar. Es la base para traducir la interfaz: cuando se
 * agregue otro idioma, estas funciones leen el idioma activo y el resto de pantallas no cambian.
 */
export const IDIOMA = 'es-CO';

export const TEXTOS = {
  cancelar: 'Cancelar', aceptar: 'Aceptar', guardar: 'Guardar', cargando: 'Cargando…', sinConexion: 'Sin conexión',
  eliminar: 'Eliminar', volver: 'Volver', buscar: 'Buscar', vacio: 'No hay nada para mostrar todavía.',
  errorInesperado: 'Ocurrió un error inesperado. Intenta de nuevo.',
} as const;

const fmtFecha = new Intl.DateTimeFormat(IDIOMA, { day: 'numeric', month: 'short', year: 'numeric' });
const fmtHora = new Intl.DateTimeFormat(IDIOMA, { hour: '2-digit', minute: '2-digit' });
const fmtNum = new Intl.NumberFormat(IDIOMA, { maximumFractionDigits: 2 });

export const fecha = (v: string | Date): string => fmtFecha.format(new Date(v));
export const hora = (v: string | Date): string => fmtHora.format(new Date(v));
export const fechaHora = (v: string | Date): string => `${fecha(v)}, ${hora(v)}`;
export const numero = (n: number): string => fmtNum.format(n);
/** 0.857 → «86 %». */
export const porcentaje = (p: number): string => `${Math.round(p * 100)} %`;
