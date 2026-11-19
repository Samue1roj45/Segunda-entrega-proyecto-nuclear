import { Injectable, signal } from '@angular/core';

export interface OpcionesDialogo {
  readonly titulo: string;
  readonly mensaje?: string;
  /** Líneas de resumen (por ejemplo, «3 preguntas sin responder»). */
  readonly detalle?: readonly string[];
  readonly confirmar?: string;
  readonly cancelar?: string;
  /** Pinta el botón de confirmar como acción destructiva. */
  readonly peligro?: boolean;
  /** Si se indica, el diálogo pide un texto. */
  readonly campo?: { readonly etiqueta: string; readonly placeholder?: string; readonly obligatorio?: boolean };
}

export interface DialogoActivo extends OpcionesDialogo { resolver: (valor: boolean | string | null) => void }

/** Reemplazo de confirm()/prompt() del navegador por ventanas de vidrio de SOMA. */
@Injectable({ providedIn: 'root' })
export class DialogoService {
  readonly actual = signal<DialogoActivo | null>(null);

  confirmar(opciones: OpcionesDialogo): Promise<boolean> {
    return new Promise((ok) => this.actual.set({ ...opciones, resolver: (v) => ok(v === true) }));
  }

  /** Devuelve el texto escrito, o null si la persona cancela. */
  pedirTexto(opciones: OpcionesDialogo): Promise<string | null> {
    return new Promise((ok) => this.actual.set({ ...opciones, campo: opciones.campo ?? { etiqueta: 'Texto' }, resolver: (v) => ok(typeof v === 'string' ? v : null) }));
  }

  cerrar(valor: boolean | string | null): void {
    const d = this.actual();
    this.actual.set(null);
    d?.resolver(valor);
  }
}
