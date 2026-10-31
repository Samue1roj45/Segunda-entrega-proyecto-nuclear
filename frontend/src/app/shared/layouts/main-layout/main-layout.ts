import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { CommonModule } from '@angular/common';
import { Component, ViewChild, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Observable, map, shareReplay } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/auth/auth.service';
import { Rol } from '../../../core/models/usuario.model';

interface NavItem {
  label: string;
  route: string;
  icon: string;
  fragment?: string;
  roles?: Rol[];
}

@Component({
  selector: 'app-main-layout',
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatSidenavModule,
    MatMenuModule,
    MatDividerModule,
  ],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss',
})
export class MainLayout {
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly auth = inject(AuthService);

  @ViewChild(MatSidenav) sidenav?: MatSidenav;

  readonly appName = environment.appName;
  readonly usuario = this.auth.usuario;
  readonly rol = this.auth.rol;

  readonly isHandset$: Observable<boolean> = this.breakpointObserver
    .observe([Breakpoints.Handset, Breakpoints.TabletPortrait])
    .pipe(
      map((result) => result.matches),
      shareReplay({ bufferSize: 1, refCount: true }),
    );

  private readonly allNavItems: NavItem[] = [
    { label: 'Panel administrador', route: '/admin', icon: 'admin_panel_settings', roles: [Rol.Admin] },
    { label: 'Reportes institucionales', route: '/reportes', icon: 'analytics', roles: [Rol.Admin] },
    { label: 'Panel docente', route: '/docente', icon: 'school', roles: [Rol.Docente] },
    { label: 'Dashboard estudiante', route: '/estudiante', icon: 'sports_esports', roles: [Rol.Estudiante] },
    { label: 'Iniciar simulación', route: '/estudiante/simulacion', icon: 'play_circle', roles: [Rol.Estudiante] },
    { label: 'Mis prácticas', route: '/estudiante', fragment: 'practicas', icon: 'assignment', roles: [Rol.Estudiante] },
    { label: 'Historial académico', route: '/estudiante', fragment: 'historial', icon: 'history', roles: [Rol.Estudiante] },
    { label: 'Perfil', route: '/estudiante', fragment: 'perfil', icon: 'person', roles: [Rol.Estudiante] },
    { label: 'Participaciones propias', route: '/participaciones', icon: 'play_circle', roles: [Rol.Estudiante] },
    { label: 'Resultados propios', route: '/resultados', icon: 'assessment', roles: [Rol.Estudiante] },
    { label: 'Estudiantes', route: '/estudiantes', icon: 'group', roles: [Rol.Docente] },
    { label: 'Grupos', route: '/grupos', icon: 'workspaces', roles: [Rol.Docente] },
    { label: 'Casos de estudio', route: '/casos', icon: 'menu_book', roles: [Rol.Docente] },
    { label: 'IA generativa', route: '/ia-generativa', icon: 'auto_awesome', roles: [Rol.Docente] },
    { label: 'Importar documentos', route: '/importacion-documentos', icon: 'upload_file', roles: [Rol.Docente] },
    { label: 'Practicas', route: '/practicas', icon: 'event', roles: [Rol.Docente] },
    { label: 'Participaciones', route: '/participaciones', icon: 'play_circle', roles: [Rol.Docente] },
    { label: 'Resultados', route: '/resultados', icon: 'assessment', roles: [Rol.Docente] },
  ];

  readonly navItems = computed(() => {
    const actual = this.rol();
    if (!actual) return [];
    return this.allNavItems.filter((it) => !it.roles || it.roles.includes(actual));
  });

  readonly iniciales = computed(() => {
    const u = this.usuario();
    if (!u) return '?';
    const first = (u.first_name || '').charAt(0);
    const last = (u.last_name || '').charAt(0);
    const combo = (first + last).toUpperCase();
    return combo || (u.username || '?').charAt(0).toUpperCase();
  });

  closeIfOverMode(): void {
    if (this.sidenav?.mode === 'over') {
      this.sidenav.close();
    }
  }

  logout(): void {
    this.auth.logout().subscribe();
  }
}
