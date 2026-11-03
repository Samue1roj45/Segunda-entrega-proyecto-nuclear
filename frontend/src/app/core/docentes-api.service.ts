import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CrearDocente, Docente } from './modelos/docente';
import { Permiso } from './modelos/permiso';

/** Cliente del API de administración de docentes (RF-03). El servidor exige ADMINISTRAR_DOCENTES. */
@Injectable({ providedIn: 'root' })
export class DocentesApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/admin/docentes';

  listar(): Observable<Docente[]> {
    return this.http.get<Docente[]>(this.base);
  }

  crear(datos: CrearDocente): Observable<Docente> {
    return this.http.post<Docente>(this.base, datos);
  }

  asignarPermisos(id: number, permisos: readonly Permiso[]): Observable<Docente> {
    return this.http.put<Docente>(`${this.base}/${id}/permisos`, { permisos });
  }
}
