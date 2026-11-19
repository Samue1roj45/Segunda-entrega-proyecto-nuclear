import { HttpErrorResponse } from '@angular/common/http';
import { Directive, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { ToastService } from './toast.service';

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
  private readonly toasts = inject(ToastService);
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
        if (avisoExito) this.toasts.ok(avisoExito);
        alTerminar(resultado);
      },
      error: (error: HttpErrorResponse) => {
        this.ocupado.set(false);
        this.error.set(mensajeDeError(error));
        this.toasts.error(mensajeDeError(error));
        alFallar?.(error);
      },
    });
  }
}
