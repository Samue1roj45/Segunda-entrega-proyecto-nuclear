import { Routes } from '@angular/router';
import { guardiaDesarrollo, guardiaPermiso, guardiaSesion } from './core/acceso';
import { Permiso } from './core/modelos/permiso';
import { Inicio } from './paginas/inicio/inicio';
import { Login } from './paginas/login/login';
import { Panel } from './paginas/panel/panel';

export const routes: Routes = [
  { path: 'login', component: Login, title: 'Iniciar sesión · SOMA' },
  // Pública: el estudiante llega desde el correo de invitación y crea su cuenta (sin sesión).
  { path: 'registro/:token', loadComponent: () => import('./paginas/registro/registro').then((m) => m.Registro), title: 'Crea tu cuenta · SOMA' },
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
          { path: 'calendario', loadComponent: () => import('./paginas/sesiones/sesiones-calendario').then((m) => m.SesionesCalendario), title: 'Calendario · SOMA' },
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
        path: 'estadisticas', canActivate: [guardiaPermiso(Permiso.ConsultarParticipaciones)],
        loadComponent: () => import('./paginas/evaluacion/estadisticas').then((m) => m.Estadisticas), title: 'Estadísticas · SOMA',
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
        path: 'ecos', canActivate: [guardiaPermiso(Permiso.ParticiparEnCasos)],
        loadComponent: () => import('./paginas/ecos/ecos-estudiante').then((m) => m.EcosEstudiante), title: 'Ecos de una decisión · SOMA',
      },
      {
        path: 'mis-estudiantes', canActivate: [guardiaPermiso(Permiso.InvitarEstudiantes)],
        loadComponent: () => import('./paginas/ecos/mis-estudiantes').then((m) => m.MisEstudiantes), title: 'Mis estudiantes · SOMA',
      },
      {
        path: 'ecos-catalogo', canActivate: [guardiaPermiso(Permiso.AdministrarDocentes)],
        loadComponent: () => import('./paginas/ecos/ecos-catalogo').then((m) => m.EcosCatalogo), title: 'Ecos · catálogo · SOMA',
      },
      {
        path: 'auditoria', canActivate: [guardiaPermiso(Permiso.AdministrarDocentes)],
        loadComponent: () => import('./paginas/admin/auditoria').then((m) => m.Auditoria), title: 'Auditoría · SOMA',
      },
      {
        path: 'componentes', canActivate: [guardiaDesarrollo],
        loadComponent: () => import('./paginas/admin/componentes').then((m) => m.Componentes), title: 'Componentes · SOMA',
      },
      {
        path: 'usuarios', canActivate: [guardiaPermiso(Permiso.AdministrarDocentes)],
        loadComponent: () => import('./paginas/admin/usuarios').then((m) => m.Usuarios), title: 'Usuarios · SOMA',
      },
      {
        path: 'docentes', canActivate: [guardiaPermiso(Permiso.AdministrarDocentes)],
        loadComponent: () => import('./paginas/admin/docentes').then((m) => m.Docentes), title: 'Docentes · SOMA',
      },
      {
        path: 'estudiantes', canActivate: [guardiaPermiso(Permiso.AdministrarDocentes)],
        loadComponent: () => import('./paginas/admin/estudiantes').then((m) => m.Estudiantes), title: 'Estudiantes · SOMA',
      },
      { path: 'acceso-denegado', loadComponent: () => import('./paginas/admin/acceso-denegado').then((m) => m.AccesoDenegado), title: 'Acceso denegado · SOMA' },
      { path: 'perfil', loadComponent: () => import('./paginas/perfil/perfil').then((m) => m.Perfil), title: 'Mi perfil · SOMA' },
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      { path: '**', loadComponent: () => import('./paginas/admin/no-encontrado').then((m) => m.NoEncontrado), title: 'No encontrada · SOMA' },
    ],
  },
  { path: '**', redirectTo: 'inicio' },
];
