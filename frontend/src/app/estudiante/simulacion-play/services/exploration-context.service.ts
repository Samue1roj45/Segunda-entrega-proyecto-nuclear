import { Injectable, signal } from '@angular/core';

import { ComisariaRoomId, COMISARIA_ROOM_MAP } from '../data/comisaria-rooms.content';
import { HospitalRoomId, HOSPITAL_ROOM_MAP } from '../data/hospital-rooms.content';
import { SceneId } from '../data/scene-types';

@Injectable({ providedIn: 'root' })
export class ExplorationContextService {
  readonly institution = signal<SceneId | null>(null);
  readonly roomId = signal<string | null>(null);
  readonly roomLabel = signal('Institución');

  readonly hospitalRoomLabel = this.roomLabel;

  setHospitalRoom(roomId: HospitalRoomId): void {
    this.institution.set('hospital');
    this.roomId.set(roomId);
    this.roomLabel.set(HOSPITAL_ROOM_MAP.get(roomId)?.label ?? 'Hospital');
  }

  setComisariaRoom(roomId: ComisariaRoomId): void {
    this.institution.set('comisaria');
    this.roomId.set(roomId);
    this.roomLabel.set(COMISARIA_ROOM_MAP.get(roomId)?.label ?? 'Comisaría');
  }

  clear(): void {
    this.institution.set(null);
    this.roomId.set(null);
    this.roomLabel.set('Institución');
  }
}
