import { Routes } from '@angular/router';
import { guardiaSesion } from './core/acceso';
import { Inicio } from './paginas/inicio/inicio';
import { Login } from './paginas/login/login';
import { Panel } from './paginas/panel/panel';

export const routes: Routes = [
  { path: 'login', component: Login, title: 'Iniciar sesión · SOMA' },
  {
    path: '',
    component: Panel,
    canActivate: [guardiaSesion],
    children: [
      { path: 'inicio', component: Inicio, title: 'Inicio · SOMA' },
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
    ],
  },
  { path: '**', redirectTo: 'inicio' },
];
