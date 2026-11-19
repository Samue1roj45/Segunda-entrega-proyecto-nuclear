import { Component, ElementRef, HostListener, computed, input, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';

export interface Destino { readonly titulo: string; readonly ruta: string; readonly grupo: string; readonly icono: string; readonly tecla?: string }

/** Paleta de comandos (Ctrl/⌘ + K): salta a cualquier sección permitida escribiendo unas letras. */
@Component({
  selector: 'soma-paleta',
  template: `
    <button type="button" class="abrir" (click)="abrir()" aria-label="Buscar o ir a… (Ctrl+K)">
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m21 21-4.3-4.3M11 18a7 7 0 1 1 0-14 7 7 0 0 1 0 14Z" /></svg>
      <span>Ir a…</span><kbd>Ctrl K</kbd>
    </button>

    @if (ayuda()) {
      <div class="fondo" (click)="ayuda.set(false)"></div>
      <div class="caja" role="dialog" aria-modal="true" aria-label="Atajos de teclado">
        <h3>Atajos de teclado</h3>
        <ul class="atajos">
          <li><span class="t">Abrir el buscador «Ir a…»</span><span><kbd>Ctrl</kbd><kbd>K</kbd></span></li>
          @for (d of conTecla(); track d.ruta) { <li><span class="t">Ir a {{ d.titulo }}</span><span><kbd>G</kbd><kbd>{{ d.tecla!.toUpperCase() }}</kbd></span></li> }
          <li><span class="t">Mostrar esta ayuda</span><span><kbd>?</kbd></span></li>
          <li><span class="t">Cerrar ventanas</span><span><kbd>Esc</kbd></span></li>
        </ul>
      </div>
    }
    @if (visible()) {
      <div class="fondo" (click)="cerrar()"></div>
      <div class="caja" role="dialog" aria-modal="true" aria-label="Ir a una sección">
        <input #entrada type="text" placeholder="¿A dónde quieres ir?" autocomplete="off" [value]="consulta()"
               (input)="escribir($any($event.target).value)" (keydown)="teclas($event)" aria-label="Buscar sección" />
        <ul role="listbox">
          @for (d of resultados(); track d.ruta; let i = $index) {
            <li role="option" [attr.aria-selected]="i === indice()" [class.sel]="i === indice()" (mouseenter)="indice.set(i)" (click)="ir(d)">
              <span class="ico"><svg viewBox="0 0 24 24" aria-hidden="true"><path [attr.d]="d.icono" /></svg></span>
              <span class="t">{{ d.titulo }}</span><small>{{ d.grupo }}</small>
            </li>
          } @empty { <li class="nada">Sin coincidencias</li> }
        </ul>
        <p class="pie"><kbd>↑</kbd><kbd>↓</kbd> moverse <kbd>Enter</kbd> abrir <kbd>Esc</kbd> cerrar</p>
      </div>
    }
  `,
  styles: `
    :host { display: contents; }
    .abrir { display: inline-flex; align-items: center; gap: .55rem; padding: .5rem .8rem .5rem .9rem; border-radius: 999px; border: 1px solid rgba(255,255,255,.8); color: var(--tinta-suave); font-size: .85rem;
      background: rgba(255,255,255,.55); box-shadow: inset 0 1px 0 #fff, 0 8px 18px -14px rgba(59,45,92,.5); transition: background .2s, transform .2s; }
    .abrir:hover { background: #fff; transform: translateY(-1px); }
    .abrir svg { width: 16px; height: 16px; fill: none; stroke: var(--lila-700); stroke-width: 1.8; stroke-linecap: round; }
    kbd { font: inherit; font-size: .7rem; padding: .1rem .4rem; border-radius: 6px; background: rgba(94,74,138,.1); color: var(--lila-800); margin: 0 .15rem; }
    .fondo { position: fixed; inset: 0; z-index: 90; background: rgba(42,31,66,.28); -webkit-backdrop-filter: blur(6px); backdrop-filter: blur(6px); animation: pal-f .2s ease both; }
    .caja { position: fixed; z-index: 91; left: 50%; top: 16vh; width: min(560px, 92vw); transform: translateX(-50%); padding: .8rem; border-radius: 26px; animation: pal-c .28s cubic-bezier(.2,.8,.2,1) both;
      background: linear-gradient(160deg, rgba(255,255,255,.88), rgba(240,234,252,.8)); border: 1px solid rgba(255,255,255,.9);
      -webkit-backdrop-filter: blur(30px) saturate(1.8); backdrop-filter: blur(30px) saturate(1.8); box-shadow: 0 50px 90px -30px rgba(42,31,66,.6), inset 0 1.5px 0 #fff; }
    input { width: 100%; font: inherit; font-size: 1.1rem; padding: .85rem 1rem; border-radius: 16px; border: 1px solid rgba(94,74,138,.2); background: rgba(255,255,255,.85); color: var(--tinta); }
    input:focus { outline: 2px solid var(--lila-500); outline-offset: 1px; }
    ul { list-style: none; margin: .6rem 0 .2rem; padding: 0; display: grid; gap: .15rem; max-height: 46vh; overflow-y: auto; }
    li { display: flex; align-items: center; gap: .75rem; padding: .55rem .65rem; border-radius: 14px; cursor: pointer; color: var(--tinta); }
    li.sel { background: linear-gradient(100deg, rgba(255,255,255,.95), rgba(221,212,239,.7)); box-shadow: 0 8px 18px -14px rgba(59,45,92,.6); }
    li.nada { color: var(--tinta-suave); cursor: default; justify-content: center; }
    .ico { width: 2rem; height: 2rem; border-radius: 10px; display: grid; place-items: center; background: rgba(255,255,255,.7); color: var(--lila-700); }
    li.sel .ico { background: linear-gradient(145deg, var(--lila-500), var(--lila-900)); color: #fff; }
    .ico svg { width: 18px; height: 18px; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }
    .t { flex: 1; } small { color: var(--tinta-suave); font-size: .72rem; letter-spacing: .1em; text-transform: uppercase; }
    h3 { font-size: 1.4rem; padding: .3rem .5rem .6rem; }
    .atajos { margin: 0; } .atajos li { cursor: default; justify-content: space-between; }
    .pie { font-size: .75rem; color: var(--tinta-suave); padding: .4rem .5rem 0; }
    @keyframes pal-f { from { opacity: 0; } } @keyframes pal-c { from { opacity: 0; transform: translate(-50%, 14px) scale(.98); } }
    @media (max-width: 1000px) { .abrir span, .abrir kbd { display: none; } .abrir { padding: .55rem; } }
    @media (prefers-reduced-motion: reduce) { .fondo, .caja { animation: none; } }
    :host-context([data-tema='noche']) .abrir { background: rgba(255,255,255,.09); border-color: rgba(190,170,240,.22); box-shadow: inset 0 1px 0 rgba(255,255,255,.12); color: #cfc6ee; }
    :host-context([data-tema='noche']) .abrir:hover { background: rgba(255,255,255,.18); }
    :host-context([data-tema='noche']) .abrir svg { stroke: #d6cbf5; }
    :host-context([data-tema='noche']) kbd { background: rgba(190,170,240,.2); color: #e2d9fb; }
    :host-context([data-tema='noche']) .fondo { background: rgba(6,4,18,.55); }
    :host-context([data-tema='noche']) .caja { background: linear-gradient(160deg, rgba(60,48,110,.82), rgba(26,20,54,.86)); border-color: rgba(190,170,240,.25); box-shadow: 0 50px 90px -30px #000, inset 0 1.5px 0 rgba(255,255,255,.14); }
    :host-context([data-tema='noche']) input { background: rgba(255,255,255,.08); border-color: rgba(190,170,240,.3); color: #f1ecff; }
    :host-context([data-tema='noche']) li { color: #e2d9fb; }
    :host-context([data-tema='noche']) li.sel { background: rgba(255,255,255,.12); box-shadow: none; }
    :host-context([data-tema='noche']) .ico { background: rgba(255,255,255,.1); color: #c6b9ee; }
    :host-context([data-tema='noche']) li.sel .ico { background: linear-gradient(145deg, #9b83e6, #5a3fb0); color: #fff; }

  `,
})
export class Paleta {
  readonly destinos = input.required<readonly Destino[]>();
  protected readonly visible = signal(false);
  protected readonly ayuda = signal(false);
  private esperandoG = 0;
  protected readonly conTecla = computed(() => this.destinos().filter((d) => d.tecla));
  protected readonly consulta = signal('');
  protected readonly indice = signal(0);
  private readonly entrada = viewChild<ElementRef<HTMLInputElement>>('entrada');

  protected readonly resultados = computed(() => {
    const q = this.normal(this.consulta());
    return this.destinos().filter((d) => !q || this.normal(`${d.titulo} ${d.grupo}`).includes(q));
  });

  constructor(private readonly router: Router) {}

  @HostListener('document:keydown', ['$event'])
  protected global(e: KeyboardEvent): void {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); this.visible() ? this.cerrar() : this.abrir(); }
    else if (e.key === 'Escape') { this.cerrar(); this.ayuda.set(false); }
    else if (!e.ctrlKey && !e.metaKey && !e.altKey && !this.escribiendo(e)) {
      if (e.key === '?') { this.ayuda.update((v) => !v); return; }
      const k = e.key.toLowerCase();
      if (this.esperandoG && Date.now() - this.esperandoG < 1200) {
        this.esperandoG = 0;
        const d = this.destinos().find((x) => x.tecla === k);
        if (d) { e.preventDefault(); this.ir(d); }
      } else if (k === 'g') { this.esperandoG = Date.now(); }
    }
  }

  protected abrir(): void {
    this.consulta.set(''); this.indice.set(0); this.visible.set(true);
    setTimeout(() => this.entrada()?.nativeElement.focus());
  }
  protected cerrar(): void { this.visible.set(false); }
  protected escribir(v: string): void { this.consulta.set(v); this.indice.set(0); }
  protected teclas(e: KeyboardEvent): void {
    const n = this.resultados().length;
    if (e.key === 'ArrowDown') { e.preventDefault(); this.indice.set((this.indice() + 1) % Math.max(n, 1)); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); this.indice.set((this.indice() - 1 + Math.max(n, 1)) % Math.max(n, 1)); }
    else if (e.key === 'Enter') { const d = this.resultados()[this.indice()]; if (d) this.ir(d); }
  }
  protected ir(d: Destino): void { this.cerrar(); void this.router.navigateByUrl(d.ruta); }
  private escribiendo(e: KeyboardEvent): boolean { const t = e.target as HTMLElement | null; return !!t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA' || t.tagName === 'SELECT' || t.isContentEditable); }
  private normal(s: string): string { return s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, ''); }
}
