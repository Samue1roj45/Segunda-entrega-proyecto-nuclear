import { Routes } from '@angular/router';
import { guardiaDocente, guardiaPermiso, guardiaSesion } from './core/acceso';
import { Permiso } from './core/modelos/permiso';
import { AccesoDenegado } from './paginas/acceso-denegado/acceso-denegado';
import { AdminDocentes } from './paginas/admin-docentes/admin-docentes';
import { ConstructorCaso } from './paginas/constructor-caso/constructor-caso';
import { Inicio } from './paginas/inicio/inicio';
import { ListaCasos } from './paginas/lista-casos/lista-casos';
import { Login } from './paginas/login/login';

export const routes: Routes = [
  { path: 'login', component: Login, title: 'Iniciar sesión · SimulaPsi' },
  { path: 'inicio', component: Inicio, canActivate: [guardiaSesion], title: 'Inicio · SimulaPsi' },
  { path: 'acceso-denegado', component: AccesoDenegado, title: 'Acceso denegado · SimulaPsi' },
  {
    path: 'admin/docentes',
    component: AdminDocentes,
    canActivate: [guardiaPermiso(Permiso.AdministrarDocentes)],
    title: 'Docentes y permisos · SimulaPsi',
  },
  { path: 'docente/casos', component: ListaCasos, canActivate: [guardiaDocente], title: 'Casos de estudio · SimulaPsi' },
  { path: 'docente/casos/:id', component: ConstructorCaso, canActivate: [guardiaDocente], title: 'Constructor de caso · SimulaPsi' },
  { path: '', pathMatch: 'full', redirectTo: 'inicio' },
  { path: '**', redirectTo: 'inicio' },
];
