import { HttpErrorResponse } from '@angular/common/http';
import { Directive, signal } from '@angular/core';
import { Observable } from 'rxjs';

/** Mensaje que el servidor envía en su cuerpo de error, o uno comprensible si no hay respuesta. */
export function mensajeDeError(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.';
  }
  return error.error?.mensaje ?? 'Ocurrió un error inesperado. Intente de nuevo.';
}

/** Estados de carga, error y confirmación comunes a los formularios del módulo. */
@Directive()
export abstract class ConOperaciones {
  protected readonly ocupado = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);

  protected operar<T>(
    peticion: Observable<T>,
    alTerminar: (resultado: T) => void,
    avisoExito?: string,
    alFallar?: (error: HttpErrorResponse) => void,
  ): void {
    this.ocupado.set(true);
    this.error.set(null);
    this.aviso.set(null);
    peticion.subscribe({
      next: (resultado) => {
        this.ocupado.set(false);
        this.aviso.set(avisoExito ?? null);
        alTerminar(resultado);
      },
      error: (error: HttpErrorResponse) => {
        this.ocupado.set(false);
        this.error.set(mensajeDeError(error));
        alFallar?.(error);
      },
    });
  }
}
