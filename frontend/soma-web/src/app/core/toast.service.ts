import { Injectable, signal } from '@angular/core';

export interface Aviso { readonly id: number; readonly tipo: 'ok' | 'error' | 'info'; readonly texto: string }

/** Avisos flotantes de vidrio («guardado», «error», …). Se cierran solos. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly avisos = signal<Aviso[]>([]);
  private siguiente = 1;

  ok(texto: string): void { this.mostrar('ok', texto); }
  error(texto: string): void { this.mostrar('error', texto, 6500); }
  info(texto: string): void { this.mostrar('info', texto); }

  cerrar(id: number): void { this.avisos.update((a) => a.filter((x) => x.id !== id)); }

  private mostrar(tipo: Aviso['tipo'], texto: string, ms = 3800): void {
    const id = this.siguiente++;
    this.avisos.update((a) => [...a.slice(-3), { id, tipo, texto }]);
    setTimeout(() => this.cerrar(id), ms);
  }
}
