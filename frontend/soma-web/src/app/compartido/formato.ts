const NOMBRES: Record<string, string> = {
  SIN_INICIAR: 'Sin iniciar', EN_CURSO: 'En curso', FINALIZADO: 'Finalizada', FINALIZADA: 'Finalizada',
  BORRADOR: 'Borrador', PUBLICADO: 'Publicado', ARCHIVADO: 'Archivado', NO_INICIADA: 'No iniciada', INCOMPLETA: 'Incompleta',
  ENVIADA: 'Enviada', PENDIENTE: 'Pendiente', ERROR: 'Con error', CONVOCADO: 'Convocado', ACTIVO: 'Activo',
};

/** Texto legible de un estado del backend (si no se conoce, se muestra tal cual en minúsculas). */
export function nombreEstado(estado: string | null | undefined): string {
  if (!estado) { return '—'; }
  return NOMBRES[estado] ?? estado.charAt(0) + estado.slice(1).toLowerCase().replace(/_/g, ' ');
}

/** Clase CSS (etiqueta de color) para un estado. */
export function claseEstado(estado: string | null | undefined): string {
  return (estado ?? '').toLowerCase().replace(/_/g, '-');
}

/** «2026-11-18T14:30» -> valor válido para <input type="datetime-local">. */
export function paraInputFecha(iso: string | null | undefined): string {
  return iso ? iso.slice(0, 16) : '';
}

/** Valor de <input type="datetime-local"> -> formato que espera el backend (LocalDateTime con segundos). */
export function desdeInputFecha(valor: string): string {
  return valor.length === 16 ? `${valor}:00` : valor;
}

export function minutosASegundos(m: number): number { return m * 60; }

/** 3725 -> «1:02:05». */
export function relojDe(segundos: number): string {
  const s = Math.max(0, Math.floor(segundos));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const r = s % 60;
  const dos = (n: number) => String(n).padStart(2, '0');
  return h > 0 ? `${h}:${dos(m)}:${dos(r)}` : `${m}:${dos(r)}`;
}

/** Descarga un Blob como archivo (reportes). */
export function guardarArchivo(blob: Blob, nombre: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = nombre; a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
