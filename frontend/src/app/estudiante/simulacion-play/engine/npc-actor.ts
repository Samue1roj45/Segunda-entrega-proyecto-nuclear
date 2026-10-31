import { NpcWaypoint, PlayNpcDef } from '../data/scene-types';

export type Facing = 'down' | 'up' | 'left' | 'right';

export interface NpcActorState {
  def: PlayNpcDef;
  x: number;
  y: number;
  facing: Facing;
  idlePhase: number;
  waypointIndex: number;
  pauseLeft: number;
}

export function createNpcActors(defs: PlayNpcDef[]): NpcActorState[] {
  return defs.map((def) => ({
    def,
    x: def.x,
    y: def.y,
    facing: def.facing ?? 'down',
    idlePhase: Math.random() * Math.PI * 2,
    waypointIndex: 0,
    pauseLeft: 0,
  }));
}

export function updateNpcActors(actors: NpcActorState[], dt: number): void {
  for (const a of actors) {
    a.idlePhase += dt * 2.2;

    if (a.pauseLeft > 0) {
      a.pauseLeft -= dt;
      continue;
    }

    const patrol = a.def.patrol;
    if (!patrol?.length) {
      a.facing = a.def.facing ?? a.facing;
      continue;
    }

    const target = patrol[a.waypointIndex];
    const dx = target.x - a.x;
    const dy = target.y - a.y;
    const dist = Math.hypot(dx, dy);

    if (dist < 4) {
      a.x = target.x;
      a.y = target.y;
      a.pauseLeft = target.pauseSec ?? 1.2 + Math.random() * 0.8;
      a.waypointIndex = (a.waypointIndex + 1) % patrol.length;
      continue;
    }

    const speed = (a.def.patrolSpeed ?? 38) * dt;
    const step = Math.min(speed, dist);
    a.x += (dx / dist) * step;
    a.y += (dy / dist) * step;

    if (Math.abs(dx) > Math.abs(dy)) {
      a.facing = dx > 0 ? 'right' : 'left';
    } else {
      a.facing = dy > 0 ? 'down' : 'up';
    }
  }
}

export function syncNpcDefsFromActors(actors: NpcActorState[]): PlayNpcDef[] {
  return actors.map((a) => ({ ...a.def, x: a.x, y: a.y }));
}
