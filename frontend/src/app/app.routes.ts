import { Routes } from '@angular/router';
import { guardiaDocente } from './core/acceso';
import { ConstructorCaso } from './paginas/constructor-caso/constructor-caso';
import { ListaCasos } from './paginas/lista-casos/lista-casos';
import { Login } from './paginas/login/login';

export const routes: Routes = [
  { path: 'login', component: Login, title: 'Iniciar sesión · SimulaPsi' },
  { path: 'docente/casos', component: ListaCasos, canActivate: [guardiaDocente], title: 'Casos de estudio · SimulaPsi' },
  { path: 'docente/casos/:id', component: ConstructorCaso, canActivate: [guardiaDocente], title: 'Constructor de caso · SimulaPsi' },
  { path: '', pathMatch: 'full', redirectTo: 'docente/casos' },
  { path: '**', redirectTo: 'docente/casos' },
];
