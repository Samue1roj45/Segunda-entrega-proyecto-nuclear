import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-no-encontrado',
  imports: [RouterLink],
  template: `
    <section class="cristal caja">
      <img src="logo-soma.webp" alt="" />
      <p class="codigo-error">404</p>
      <h2>Esta página se perdió en la niebla</h2>
      <p class="suave">La dirección no existe o fue movida. Vuelve al inicio o usa <kbd>Ctrl</kbd> <kbd>K</kbd> para ir directo a una sección.</p>
      <a routerLink="/inicio" class="boton-primario">Volver al inicio</a>
    </section>
  `,
  styles: `
    .caja { display: grid; justify-items: center; gap: .8rem; padding: 3rem 1.5rem; text-align: center; }
    img { width: 120px; opacity: .85; animation: flota 7s ease-in-out infinite; filter: drop-shadow(0 14px 18px rgba(88,68,140,.28)); }
    .codigo-error { font-family: var(--serif); font-size: 5rem; line-height: 1; color: var(--lila-500); letter-spacing: .1em; }
    .suave { max-width: 30rem; line-height: 1.5; } a { text-decoration: none; margin-top: .6rem; }
    kbd { font: inherit; font-size: .75rem; padding: .1rem .4rem; border-radius: 6px; background: rgba(94,74,138,.12); }
    @keyframes flota { 50% { transform: translateY(-8px); } }
  `,
})
export class NoEncontrado {}
