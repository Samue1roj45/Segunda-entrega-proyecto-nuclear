import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Toasts } from './compartido/toasts';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toasts],
  template: `<router-outlet /><soma-toasts />`,
})
export class App {}
