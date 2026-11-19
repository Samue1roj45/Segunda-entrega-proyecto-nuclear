import { Routes } from '@angular/router';
import { guardiaPermiso, guardiaSesion } from './core/acceso';
import { Permiso } from './core/modelos/permiso';
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
      {
        path: 'casos', canActivate: [guardiaPermiso(Permiso.GestionarCasos)],
        children: [
          { path: '', loadComponent: () => import('./paginas/casos/casos-lista').then((m) => m.CasosLista), title: 'Casos · SOMA' },
          { path: ':id', loadComponent: () => import('./paginas/casos/caso-editor').then((m) => m.CasoEditor), title: 'Caso · SOMA' },
        ],
      },
      {
        path: 'sesiones', canActivate: [guardiaPermiso(Permiso.ProgramarSesiones)],
        children: [
          { path: '', loadComponent: () => import('./paginas/sesiones/sesiones-lista').then((m) => m.SesionesLista), title: 'Sesiones · SOMA' },
          { path: ':id', loadComponent: () => import('./paginas/sesiones/sesion-detalle').then((m) => m.SesionDetalleVista), title: 'Sesión · SOMA' },
        ],
      },
      {
        path: 'participaciones', canActivate: [guardiaPermiso(Permiso.ConsultarParticipaciones)],
        loadComponent: () => import('./paginas/evaluacion/participaciones-docente').then((m) => m.ParticipacionesDocente), title: 'Participaciones · SOMA',
      },
      {
        path: 'resultados', canActivate: [guardiaPermiso(Permiso.ConsultarParticipaciones)],
        loadComponent: () => import('./paginas/evaluacion/resultados-docente').then((m) => m.ResultadosDocente), title: 'Resultados · SOMA',
      },
      {
        path: 'mis-sesiones', canActivate: [guardiaPermiso(Permiso.ParticiparEnCasos)],
        loadComponent: () => import('./paginas/estudiante/mis-sesiones').then((m) => m.MisSesiones), title: 'Mis casos · SOMA',
      },
      {
        path: 'participar/:id', canActivate: [guardiaPermiso(Permiso.ParticiparEnCasos)],
        loadComponent: () => import('./paginas/estudiante/participar').then((m) => m.Participar), title: 'Participación · SOMA',
      },
      {
        path: 'mis-resultados', canActivate: [guardiaPermiso(Permiso.ConsultarResultadosPropios)],
        loadComponent: () => import('./paginas/estudiante/mis-resultados').then((m) => m.MisResultados), title: 'Mis resultados · SOMA',
      },
      {
        path: 'docentes', canActivate: [guardiaPermiso(Permiso.AdministrarDocentes)],
        loadComponent: () => import('./paginas/admin/docentes').then((m) => m.Docentes), title: 'Docentes · SOMA',
      },
      { path: 'acceso-denegado', loadComponent: () => import('./paginas/admin/acceso-denegado').then((m) => m.AccesoDenegado), title: 'Acceso denegado · SOMA' },
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
    ],
  },
  { path: '**', redirectTo: 'inicio' },
];
