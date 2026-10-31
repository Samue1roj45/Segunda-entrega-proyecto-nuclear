import { Injectable, signal } from '@angular/core';

import { Participacion } from '../../../core/models/practicas.model';

/** Puente mínimo hacia participación API (Fase C). */
@Injectable({ providedIn: 'root' })
export class SimulacionPlayBridgeService {
  readonly participacion = signal<Participacion | null>(null);
}
