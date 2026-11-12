import { Routes } from '@angular/router';
import { guardiaDocente, guardiaSesion } from './core/acceso';
import { AccesoDenegado } from './paginas/acceso-denegado/acceso-denegado';
import { ConstructorCaso } from './paginas/constructor-caso/constructor-caso';
import { Inicio } from './paginas/inicio/inicio';
import { ListaCasos } from './paginas/lista-casos/lista-casos';
import { Login } from './paginas/login/login';

export const routes: Routes = [
  { path: 'login', component: Login, title: 'Iniciar sesión · SOMA' },
  { path: 'inicio', component: Inicio, canActivate: [guardiaSesion], title: 'Inicio · SOMA' },
  { path: 'acceso-denegado', component: AccesoDenegado, title: 'Acceso denegado · SOMA' },
  { path: 'docente/casos', component: ListaCasos, canActivate: [guardiaDocente], title: 'Casos de estudio · SOMA' },
  { path: 'docente/casos/:id', component: ConstructorCaso, canActivate: [guardiaDocente], title: 'Constructor de caso · SOMA' },
  { path: '', pathMatch: 'full', redirectTo: 'inicio' },
  { path: '**', redirectTo: 'inicio' },
];
