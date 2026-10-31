import { Rect, Vec2 } from './play-types';

export class PlayPlayer {
  x: number;
  y: number;
  readonly width = 28;
  readonly height = 40;
  readonly speed = 220;
  direction: 'down' | 'up' | 'left' | 'right' = 'down';

  private readonly keys = {
    up: false,
    down: false,
    left: false,
    right: false,
  };

  constructor(start: Vec2) {
    this.x = start.x;
    this.y = start.y;
  }

  setKey(code: string, pressed: boolean): void {
    const map: Record<string, keyof typeof this.keys> = {
      ArrowUp: 'up',
      ArrowDown: 'down',
      ArrowLeft: 'left',
      ArrowRight: 'right',
      KeyW: 'up',
      KeyS: 'down',
      KeyA: 'left',
      KeyD: 'right',
    };
    const k = map[code];
    if (k) this.keys[k] = pressed;
  }

  update(dt: number, bounds: Rect, obstacles: Rect[]): void {
    let dx = 0;
    let dy = 0;
    if (this.keys.up) dy -= 1;
    if (this.keys.down) dy += 1;
    if (this.keys.left) dx -= 1;
    if (this.keys.right) dx += 1;

    if (dx === 0 && dy === 0) return;

    const len = Math.hypot(dx, dy);
    dx = (dx / len) * this.speed * dt;
    dy = (dy / len) * this.speed * dt;

    if (Math.abs(dx) > Math.abs(dy)) {
      this.direction = dx > 0 ? 'right' : 'left';
    } else {
      this.direction = dy > 0 ? 'down' : 'up';
    }

    const nextX = this.clamp(this.x + dx, bounds.x, bounds.x + bounds.w - this.width);
    const nextY = this.clamp(this.y + dy, bounds.y, bounds.y + bounds.h - this.height);

    const boxX = nextX;
    const boxY = nextY;
    const boxW = this.width;
    const boxH = this.height;

    if (!this.hitsObstacle(boxX, boxY, boxW, boxH, obstacles)) {
      this.x = nextX;
      this.y = nextY;
    } else if (!this.hitsObstacle(nextX, this.y, boxW, boxH, obstacles)) {
      this.x = nextX;
    } else if (!this.hitsObstacle(this.x, nextY, boxW, boxH, obstacles)) {
      this.y = nextY;
    }
  }

  center(): Vec2 {
    return { x: this.x + this.width / 2, y: this.y + this.height / 2 };
  }

  private hitsObstacle(x: number, y: number, w: number, h: number, obstacles: Rect[]): boolean {
    return obstacles.some(
      (o) => x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y,
    );
  }

  private clamp(v: number, min: number, max: number): number {
    return Math.max(min, Math.min(max, v));
  }
}
