import {
  CAMERA_LERP,
  INTERACT_RADIUS_PX,
  MOVE_SPEED_PX,
  PLAYER_RADIUS,
  WORLD_HEIGHT,
  WORLD_WIDTH,
  obstaclesToPx,
  pctToPxX,
  pctToPxY,
  roomLayout,
} from '../data/room-world-layout';
import { RoomHotspot } from '../data/immersive-room.types';

export type Facing = 'down' | 'up' | 'left' | 'right';

export interface HotspotPx {
  x: number;
  y: number;
  w: number;
  h: number;
  npcId?: string;
  objectId?: string;
  observeId?: string;
  exitTo?: string;
  exitLabel?: string;
}

export class RoomExplorationEngine {
  px = pctToPxX(48);
  py = pctToPxY(72);
  camX = 0;
  camY = 0;
  facing: Facing = 'down';
  walking = false;
  walkPhase = 0;

  private obstacles: { x: number; y: number; w: number; h: number }[] = [];
  private keys = new Set<string>();
  private targetX: number | null = null;
  private targetY: number | null = null;
  viewportW = 800;
  viewportH = 500;

  readonly worldW = WORLD_WIDTH;
  readonly worldH = WORLD_HEIGHT;
  readonly speed = MOVE_SPEED_PX;
  readonly interactRadius = INTERACT_RADIUS_PX;

  loadRoom(institution: 'hospital' | 'comisaria', roomId: string): void {
    const layout = roomLayout(institution, roomId);
    this.obstacles = obstaclesToPx(layout);
    this.px = pctToPxX(layout.spawn.x);
    this.py = pctToPxY(layout.spawn.y);
    this.targetX = null;
    this.targetY = null;
    this.clampCamera();
  }

  setViewport(w: number, h: number): void {
    this.viewportW = Math.max(320, w);
    this.viewportH = Math.max(280, h);
    this.clampCamera();
  }

  setKey(code: string, down: boolean): void {
    if (down) this.keys.add(code);
    else this.keys.delete(code);
    if (this.keys.size > 0) {
      this.targetX = null;
      this.targetY = null;
    }
  }

  clearKeys(): void {
    this.keys.clear();
  }

  setWalkTarget(worldX: number, worldY: number): void {
    this.targetX = worldX;
    this.targetY = worldY;
  }

  hotspotToPx(h: RoomHotspot): HotspotPx {
    return {
      x: pctToPxX(h.x),
      y: pctToPxY(h.y),
      w: pctToPxX(h.w),
      h: pctToPxY(h.h),
      npcId: h.npcId,
      objectId: h.objectId,
      observeId: h.observeId,
      exitTo: h.exitTo,
      exitLabel: h.exitLabel,
    };
  }

  hotspotCenter(h: HotspotPx): { x: number; y: number } {
    return { x: h.x + h.w / 2, y: h.y + h.h / 2 };
  }

  distToHotspot(h: HotspotPx): number {
    const c = this.hotspotCenter(h);
    return Math.hypot(this.px - c.x, this.py - c.y);
  }

  isNear(h: HotspotPx): boolean {
    return this.distToHotspot(h) <= this.interactRadius;
  }

  screenToWorld(clientX: number, clientY: number, rect: DOMRect): { x: number; y: number } {
    const sx = clientX - rect.left;
    const sy = clientY - rect.top;
    return {
      x: sx + this.camX,
      y: sy + this.camY,
    };
  }

  cameraTransform(): string {
    return `translate(${-Math.round(this.camX)}px, ${-Math.round(this.camY)}px)`;
  }

  tick(dtSec: number): void {
    let dx = 0;
    let dy = 0;

    if (this.targetX !== null && this.targetY !== null) {
      const tx = this.targetX - this.px;
      const ty = this.targetY - this.py;
      const d = Math.hypot(tx, ty);
      if (d < 8) {
        this.targetX = null;
        this.targetY = null;
        this.walking = false;
      } else {
        dx = tx / d;
        dy = ty / d;
        this.walking = true;
      }
    } else {
      if (this.keys.has('KeyW') || this.keys.has('ArrowUp')) dy -= 1;
      if (this.keys.has('KeyS') || this.keys.has('ArrowDown')) dy += 1;
      if (this.keys.has('KeyA') || this.keys.has('ArrowLeft')) dx -= 1;
      if (this.keys.has('KeyD') || this.keys.has('ArrowRight')) dx += 1;
      this.walking = dx !== 0 || dy !== 0;
    }

    if (this.walking) {
      this.walkPhase += dtSec * 9;
    }

    if (dx !== 0 || dy !== 0) {
      const len = Math.hypot(dx, dy) || 1;
      dx /= len;
      dy /= len;
      if (Math.abs(dx) > Math.abs(dy)) {
        this.facing = dx < 0 ? 'left' : 'right';
      } else {
        this.facing = dy < 0 ? 'up' : 'down';
      }
      const step = this.speed * dtSec;
      this.tryMove(this.px + dx * step, this.py + dy * step);
    }

    const targetCamX = this.px - this.viewportW / 2;
    const targetCamY = this.py - this.viewportH / 2;
    this.camX += (targetCamX - this.camX) * CAMERA_LERP;
    this.camY += (targetCamY - this.camY) * CAMERA_LERP;
    this.clampCamera();
  }

  private tryMove(nx: number, ny: number): void {
    const minX = PLAYER_RADIUS;
    const maxX = WORLD_WIDTH - PLAYER_RADIUS;
    const minY = PLAYER_RADIUS + 80;
    const maxY = WORLD_HEIGHT - PLAYER_RADIUS;
    nx = Math.max(minX, Math.min(maxX, nx));
    ny = Math.max(minY, Math.min(maxY, ny));
    if (!this.collides(nx, ny)) {
      this.px = nx;
      this.py = ny;
    } else if (!this.collides(nx, this.py)) {
      this.px = nx;
    } else if (!this.collides(this.px, ny)) {
      this.py = ny;
    }
  }

  private collides(x: number, y: number): boolean {
    const r = PLAYER_RADIUS * 0.85;
    for (const o of this.obstacles) {
      if (x + r > o.x && x - r < o.x + o.w && y + r > o.y && y - r < o.y + o.h) {
        return true;
      }
    }
    return false;
  }

  private clampCamera(): void {
    const maxX = Math.max(0, WORLD_WIDTH - this.viewportW);
    const maxY = Math.max(0, WORLD_HEIGHT - this.viewportH);
    this.camX = Math.max(0, Math.min(maxX, this.camX));
    this.camY = Math.max(0, Math.min(maxY, this.camY));
  }
}
