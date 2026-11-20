import { Injectable, effect, inject, signal } from '@angular/core';
import { PreferenciasService } from './preferencias.service';
import { SomaApiService } from './soma-api.service';
import { ModoTema, TemaService } from './tema.service';

/**
 * Lleva las preferencias de interfaz a la cuenta: al entrar al panel se traen las del servidor (si hay)
 * y cada cambio posterior se guarda allí, para que acompañen a la persona en otro navegador o equipo.
 * Si el servidor no responde, todo sigue funcionando con lo guardado en este navegador.
 */
@Injectable({ providedIn: 'root' })
export class PreferenciasRemotasService {
  private readonly api = inject(SomaApiService);
  private readonly prefs = inject(PreferenciasService);
  private readonly temas = inject(TemaService);
  private listo = false;
  private pendiente: ReturnType<typeof setTimeout> | null = null;
  private ultimo = '';
  readonly sincronizado = signal(false);

  constructor() {
    effect(() => {
      // Se vuelve a ejecutar cuando cambia cualquiera de estas señales.
      const datos = { ...this.prefs.exportar(), tema: this.temas.modo() };
      if (!this.listo) { return; }
      const texto = JSON.stringify(datos);
      if (texto === this.ultimo) { return; }
      this.ultimo = texto;
      if (this.pendiente) { clearTimeout(this.pendiente); }
      this.pendiente = setTimeout(() => this.api.guardarPreferencias(datos).subscribe({ error: () => undefined }), 800);
    });
  }

  /** Trae las preferencias de la cuenta; si la cuenta aún no tiene, sube las de este navegador. */
  iniciar(): void {
    if (this.listo) { return; }
    this.api.preferencias().subscribe({
      next: (remotas) => {
        const tema = remotas['tema'];
        if (Object.keys(remotas).length > 0) {
          this.prefs.importar(remotas);
          if (tema === 'auto' || tema === 'dia' || tema === 'noche') { this.temas.fijarModo(tema as ModoTema); }
        }
        this.ultimo = Object.keys(remotas).length > 0 ? JSON.stringify({ ...this.prefs.exportar(), tema: this.temas.modo() }) : '';
        this.listo = true;
        this.sincronizado.set(true);
        if (Object.keys(remotas).length === 0) { this.api.guardarPreferencias({ ...this.prefs.exportar(), tema: this.temas.modo() }).subscribe({ error: () => undefined }); }
      },
      error: () => undefined,
    });
  }
}
