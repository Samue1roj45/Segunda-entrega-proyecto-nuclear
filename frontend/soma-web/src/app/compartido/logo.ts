import { Component, input } from '@angular/core';

/** Isotipo de SOMA: cerebro de cristal con hojas (fondo transparente, en la paleta lila de la página). */
@Component({
  selector: 'soma-logo',
  template: `<img src="logo-soma.webp" [attr.width]="tamano()" [attr.height]="tamano()" alt="SOMA" decoding="async" />`,
  styles: `:host { display: inline-flex; } img { object-fit: contain; display: block; }`,
})
export class Logo {
  readonly tamano = input(120);
}
