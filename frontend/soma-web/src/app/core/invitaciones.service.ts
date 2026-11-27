import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface ResultadoInvitacion {
  correo: string;
  /** ENVIADA, REENVIADA, VINCULADO (ya tenía cuenta), CORREO_INVALIDO, DOMINIO_INVALIDO o NO_ES_ESTUDIANTE. */
  resultado: string;
  mensaje: string;
  /** Solo para el docente que invita: sirve para compartir el enlace a mano si el correo no llegó. */
  enlace: string | null;
  correoEnviado: boolean;
}
export interface InvitacionDocente {
  id: number; correo: string; estado: 'PENDIENTE' | 'ACEPTADA' | 'CANCELADA' | 'VENCIDA'; creada: string; vence: string;
  aceptada: string | null; estudiante: string | null;
}
export interface InvitacionPublica { correo: string; docente: string; vence: string; }
export interface DatosRegistro {
  nombreCompleto: string; tipoDocumento: string; numeroDocumento: string; fechaNacimiento: string; telefono: string | null;
  contrasena: string; aceptaTratamientoDatos: boolean;
}

/** Invitaciones de estudiantes por correo (docente) y creación de la cuenta desde el enlace (público). */
@Injectable({ providedIn: 'root' })
export class InvitacionesService {
  private readonly http = inject(HttpClient);

  invitar(correos: string[]): Observable<ResultadoInvitacion[]> { return this.http.post<ResultadoInvitacion[]>('/api/invitaciones', { correos }); }
  listar(): Observable<InvitacionDocente[]> { return this.http.get<InvitacionDocente[]>('/api/invitaciones'); }
  reenviar(id: number): Observable<ResultadoInvitacion> { return this.http.post<ResultadoInvitacion>(`/api/invitaciones/${id}/reenviar`, null); }
  cancelar(id: number): Observable<void> { return this.http.delete<void>(`/api/invitaciones/${id}`); }
  consultar(token: string): Observable<InvitacionPublica> { return this.http.get<InvitacionPublica>(`/api/invitaciones/publica/${encodeURIComponent(token)}`); }
  aceptar(token: string, datos: DatosRegistro): Observable<{ correo: string; mensaje: string }> {
    return this.http.post<{ correo: string; mensaje: string }>(`/api/invitaciones/publica/${encodeURIComponent(token)}/aceptar`, datos);
  }
}
