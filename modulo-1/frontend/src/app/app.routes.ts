import { Routes } from '@angular/router';
import { guardiaPermiso, guardiaSesion } from './core/acceso';
import { Permiso } from './core/modelos/permiso';
import { AccesoDenegado } from './paginas/acceso-denegado/acceso-denegado';
import { AdminDocentes } from './paginas/admin-docentes/admin-docentes';
import { Inicio } from './paginas/inicio/inicio';
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
  { path: '', pathMatch: 'full', redirectTo: 'inicio' },
  { path: '**', redirectTo: 'inicio' },
];
