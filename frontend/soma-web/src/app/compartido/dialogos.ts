import { AfterViewChecked, Component, ElementRef, HostListener, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogoService } from '../core/dialogo.service';

@Component({
  selector: 'soma-dialogos',
  imports: [FormsModule],
  template: `
    @if (dialogos.actual(); as d) {
      <div class="modal-fondo" (click)="cancelar()"></div>
      <div #caja class="modal dialogo" role="alertdialog" aria-modal="true" [attr.aria-label]="d.titulo">
        <div class="cuerpo-d">
          <h3>{{ d.titulo }}</h3>
          @if (d.mensaje) { <p class="suave">{{ d.mensaje }}</p> }
          @if (d.detalle?.length) { <ul class="detalle-d">@for (l of d.detalle; track l) { <li>{{ l }}</li> }</ul> }
          @if (d.campo; as c) {
            <label class="campo-f">{{ c.etiqueta }}
              <textarea #texto rows="3" [placeholder]="c.placeholder ?? ''" [ngModel]="valor()" (ngModelChange)="valor.set($event)" name="valor-dialogo"></textarea>
            </label>
          }
        </div>
        <footer>
          <button type="button" class="boton-suave" (click)="cancelar()">{{ d.cancelar ?? 'Cancelar' }}</button>
          <button type="button" [class]="d.peligro ? 'boton-peligro' : 'boton-primario'" [disabled]="d.campo?.obligatorio && !valor().trim()" (click)="aceptar()">{{ d.confirmar ?? 'Aceptar' }}</button>
        </footer>
      </div>
    }
  `,
  styles: `
    .dialogo { z-index: 400; width: min(460px, 92vw); } .modal-fondo { z-index: 399; }
    .cuerpo-d { padding: 1.4rem 1.5rem .6rem; display: grid; gap: .7rem; } h3 { font-size: 1.5rem; }
    .detalle-d { margin: 0; padding-left: 1.2rem; display: grid; gap: .25rem; color: var(--tinta); }
    footer { display: flex; justify-content: flex-end; gap: .6rem; padding: .8rem 1.5rem 1.3rem; }
    textarea { width: 100%; }
  `,
})
export class Dialogos implements AfterViewChecked {
  protected readonly dialogos = inject(DialogoService);
  protected readonly valor = signal('');
  private readonly caja = viewChild<ElementRef<HTMLElement>>('caja');
  private readonly texto = viewChild<ElementRef<HTMLTextAreaElement>>('texto');
  private abierto = false;
  private anterior: HTMLElement | null = null;

  ngAfterViewChecked(): void {
    const hay = !!this.dialogos.actual();
    if (hay && !this.abierto) {
      this.abierto = true; this.valor.set('');
      this.anterior = document.activeElement as HTMLElement | null;
      setTimeout(() => (this.texto()?.nativeElement ?? this.caja()?.nativeElement.querySelector<HTMLElement>('button:last-of-type'))?.focus());
    } else if (!hay && this.abierto) {
      this.abierto = false; this.anterior?.focus?.();
    }
  }

  protected aceptar(): void { const d = this.dialogos.actual(); this.dialogos.cerrar(d?.campo ? this.valor().trim() : true); }
  protected cancelar(): void { this.dialogos.cerrar(this.dialogos.actual()?.campo ? null : false); }

  /** Escape cancela y Tab queda dentro del diálogo (trampa de foco). */
  @HostListener('document:keydown', ['$event'])
  protected teclas(e: KeyboardEvent): void {
    if (!this.dialogos.actual()) return;
    if (e.key === 'Escape') { e.stopPropagation(); this.cancelar(); return; }
    if (e.key !== 'Tab') return;
    const focos = [...(this.caja()?.nativeElement.querySelectorAll<HTMLElement>('button:not([disabled]), textarea, input') ?? [])];
    if (!focos.length) return;
    const primero = focos[0], ultimo = focos[focos.length - 1];
    if (e.shiftKey && document.activeElement === primero) { e.preventDefault(); ultimo.focus(); }
    else if (!e.shiftKey && document.activeElement === ultimo) { e.preventDefault(); primero.focus(); }
  }
}
