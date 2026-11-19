import { Component, inject, signal } from '@angular/core';
import { Barras, Histograma } from '../../compartido/graficas';
import { DialogoService } from '../../core/dialogo.service';
import { RegistroErroresService } from '../../core/registro-errores.service';
import { ToastService } from '../../core/toast.service';
import { guardarArchivo } from '../../compartido/formato';

/** Catálogo vivo de los componentes de SOMA (para mantener la interfaz coherente) y visor del registro de errores. */
@Component({
  selector: 'app-componentes',
  imports: [Barras, Histograma],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2 class="sr-only">Componentes</h2></div>
      <section class="cristal bloque"><h3>Botones y etiquetas</h3>
        <div class="fila-demo">
          <button type="button" class="boton-primario">Primario</button><button type="button" class="boton-suave">Suave</button>
          <button type="button" class="boton-peligro">Peligro</button><button type="button" class="boton-suave boton-chico">Chico</button>
          <span class="etiqueta ok">Correcta</span><span class="etiqueta mal">Incorrecta</span><span class="etiqueta">Neutra</span>
        </div></section>
      <section class="cristal bloque"><h3>Avisos y diálogos</h3>
        <div class="fila-demo">
          <button type="button" class="boton-suave" (click)="toasts.ok('Operación completada.')">Aviso de éxito</button>
          <button type="button" class="boton-suave" (click)="toasts.error('Algo salió mal.')">Aviso de error</button>
          <button type="button" class="boton-suave" (click)="toasts.info('Dato informativo.')">Aviso informativo</button>
          <button type="button" class="boton-suave" (click)="dialogo()">Diálogo de confirmación</button>
        </div>
        <p class="alerta-error" role="note">Así se ve un error en pantalla.</p><p class="aviso-ok" role="note">Así se ve una confirmación.</p></section>
      <section class="cristal bloque"><h3>Formulario</h3>
        <div class="fila-demo"><label class="campo-f">Texto<input placeholder="Escribe aquí" /></label><label class="campo-f">Lista<select><option>Opción A</option><option>Opción B</option></select></label></div></section>
      <section class="cristal bloque"><h3>Gráficas</h3>
        <div class="fila-demo">
          <soma-histograma [valores]="[0.2, 0.5, 0.55, 0.7, 0.72, 0.8, 0.9, 1]" />
          <soma-barras [datos]="[{ etiqueta: '1. Pregunta de ejemplo', valor: 0.8 }, { etiqueta: '2. Otra pregunta', valor: 0.45 }]" />
        </div></section>
      <section class="cristal bloque"><h3>Registro de errores de este navegador</h3>
        <p class="suave">{{ total() }} error(es) guardado(s). Sirve para adjuntar al reportar un fallo.</p>
        <div class="fila-demo">
          <button type="button" class="boton-suave" (click)="descargar()">Descargar informe</button>
          <button type="button" class="boton-suave" (click)="borrar()">Vaciar registro</button>
        </div></section>
    </div>
  `,
  styles: `.fila-demo { display: flex; flex-wrap: wrap; gap: .8rem; align-items: flex-end; } soma-histograma, soma-barras { flex: 1 1 280px; }`,
})
export class Componentes {
  protected readonly toasts = inject(ToastService);
  private readonly dialogos = inject(DialogoService);
  private readonly registro = inject(RegistroErroresService);
  protected readonly total = signal(this.registro.leer().length);
  protected dialogo(): void {
    void this.dialogos.confirmar({ titulo: 'Diálogo de ejemplo', mensaje: 'Así se pide una confirmación.', detalle: ['Primera línea de resumen', 'Segunda línea'], confirmar: 'Entendido', peligro: true })
      .then((ok) => this.toasts.info(ok ? 'Confirmaste.' : 'Cancelaste.'));
  }
  protected descargar(): void { guardarArchivo(new Blob([this.registro.informe()], { type: 'text/plain;charset=utf-8' }), 'informe-errores-soma.txt'); }
  protected borrar(): void { this.registro.borrar(); this.total.set(0); }
}
