import { ErrorHandler, Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

export interface RegistroError { fecha: string; origen: 'interfaz' | 'servidor'; mensaje: string; detalle?: string; ruta: string; rol: string | null }

const CLAVE = 'soma.errores';
const MAX = 50;

/** Guarda los últimos errores inesperados (con pantalla y rol) para poder diagnosticarlos cuando alguien reporte un fallo. */
@Injectable({ providedIn: 'root' })
export class RegistroErroresService {
  registrar(origen: RegistroError['origen'], mensaje: string, detalle?: string): void {
    const lista = this.leer();
    let rol: string | null = null;
    try { rol = JSON.parse(sessionStorage.getItem('soma.sesion') ?? localStorage.getItem('soma.sesion') ?? 'null')?.rol ?? null; } catch { /* sin sesión */ }
    lista.push({ fecha: new Date().toISOString(), origen, mensaje: mensaje.slice(0, 300), detalle: detalle?.slice(0, 800), ruta: location.pathname, rol });
    try { localStorage.setItem(CLAVE, JSON.stringify(lista.slice(-MAX))); } catch { /* sin almacenamiento */ }
  }
  leer(): RegistroError[] { try { return JSON.parse(localStorage.getItem(CLAVE) ?? '[]') as RegistroError[]; } catch { return []; } }
  borrar(): void { try { localStorage.removeItem(CLAVE); } catch { /* nada */ } }
  informe(): string {
    return `SOMA · informe de errores (${new Date().toLocaleString('es-CO')})\nNavegador: ${navigator.userAgent}\n\n` +
      this.leer().map((e) => `[${e.fecha}] ${e.origen} · ${e.ruta} · rol ${e.rol ?? '—'}\n  ${e.mensaje}${e.detalle ? '\n  ' + e.detalle.replace(/\n/g, '\n  ') : ''}`).join('\n') || 'Sin errores registrados.';
  }
}

/** Reemplaza el manejador por defecto: además de la consola, deja constancia del error. */
@Injectable()
export class ManejadorErrores implements ErrorHandler {
  private readonly registro = inject(RegistroErroresService);
  handleError(error: unknown): void {
    const e = error as Error;
    this.registro.registrar('interfaz', e?.message ?? String(error), e?.stack);
    console.error(error);
  }
}
