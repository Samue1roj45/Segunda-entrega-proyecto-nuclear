import { Component, input } from '@angular/core';

/** Isotipo de SOMA: perfil humano que nace de dos hojas y un punto (la persona y su contexto). */
@Component({
  selector: 'soma-logo',
  template: `
    <svg [attr.width]="tamano()" [attr.height]="tamano()" viewBox="0 0 120 120" role="img" aria-label="SOMA">
      <defs>
        <linearGradient id="hojaA" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#cbbde8" /><stop offset="1" stop-color="#6c56a0" />
        </linearGradient>
        <linearGradient id="hojaB" x1="1" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="#8f78c2" /><stop offset="1" stop-color="#3f2f66" />
        </linearGradient>
        <linearGradient id="rostro" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#e6ddf5" /><stop offset="1" stop-color="#a893d1" />
        </linearGradient>
      </defs>
      <circle cx="62" cy="12" r="7.5" fill="url(#hojaB)" />
      <path d="M58 104 C 30 86, 26 52, 44 26 C 58 46, 64 74, 58 104 Z" fill="url(#hojaA)" opacity=".9" />
      <path d="M60 104 C 92 88, 104 52, 92 14 C 72 30, 58 62, 60 104 Z" fill="url(#hojaB)" opacity=".9" />
      <path d="M50 46 C 38 46, 30 56, 30 66 L 25 74 L 31 76 L 30 84 C 30 90, 36 92, 44 90 L 46 100 L 58 104
               C 54 86, 56 64, 50 46 Z" fill="url(#rostro)" opacity=".95" />
    </svg>
  `,
  styles: `:host { display: inline-flex; }`,
})
export class Logo {
  readonly tamano = input(120);
}
