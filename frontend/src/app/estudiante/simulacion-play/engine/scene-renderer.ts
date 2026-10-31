import { PlaySceneConfig, SceneDoor, SceneFurniture, SceneZone } from '../data/scene-types';
import { AssetLoader } from './asset-loader';
import { Facing, NpcActorState } from './npc-actor';
import { PlayPlayer } from './play-player';

export class SceneRenderer {
  private bobPhase = 0;

  constructor(private readonly assets: AssetLoader) {}

  tick(dt: number): void {
    this.bobPhase += dt * 4;
  }

  drawBackground(ctx: CanvasRenderingContext2D, scene: PlaySceneConfig): void {
    const W = scene.worldWidth;
    const H = scene.worldHeight;
    ctx.fillStyle = scene.id === 'comisaria' ? '#e2e8f0' : '#cbd5e1';
    ctx.fillRect(0, 0, W, H);
    const bg = this.assets.bgFor(scene.id);
    if (bg) {
      ctx.globalAlpha = 0.05;
      ctx.drawImage(bg, 0, 0, W, H);
      ctx.globalAlpha = 1;
    }
  }

  drawZones(ctx: CanvasRenderingContext2D, scene: PlaySceneConfig): void {
    for (const zone of scene.zones ?? []) {
      this.drawZoneTile(ctx, scene.id, zone);
    }
    this.drawDoors(ctx, scene.doors ?? []);
    this.drawWalls(ctx, scene);
  }

  drawFurniture(ctx: CanvasRenderingContext2D, scene: PlaySceneConfig): void {
    for (const f of scene.furniture ?? []) {
      this.drawFurniturePiece(ctx, f);
    }
  }

  drawPlayer(ctx: CanvasRenderingContext2D, player: PlayPlayer): void {
    const img = this.assets.player();
    const bob = Math.sin(this.bobPhase) * 2;
    const x = player.x;
    const y = player.y + bob;

    if (img) {
      ctx.drawImage(img, x - 4, y - 6, player.width + 8, player.height + 10);
    } else {
      ctx.fillStyle = '#0891b2';
      ctx.fillRect(x, y, player.width, player.height);
    }
  }

  drawNpcs(ctx: CanvasRenderingContext2D, actors: NpcActorState[], nearestId: string | null): void {
    for (const a of actors) {
      const isNear = nearestId === a.def.id;
      this.drawCharacter(
        ctx,
        a.x,
        a.y,
        a.def.color,
        a.def.shortLabel,
        a.facing,
        a.idlePhase,
        isNear,
      );
    }
  }

  drawObjects(ctx: CanvasRenderingContext2D, scene: PlaySceneConfig, nearestId: string | null): void {
    const doc = this.assets.objectDoc();
    for (const obj of scene.objects) {
      const isNear = nearestId === obj.id;
      const r = obj.radius;
      const pulse = isNear ? 1 + Math.sin(this.bobPhase * 2) * 0.08 : 1;

      if (doc) {
        const size = r * 2.2 * pulse;
        ctx.drawImage(doc, obj.x - size / 2, obj.y - size / 2, size, size);
      } else {
        ctx.fillStyle = 'rgba(251, 191, 36, 0.5)';
        ctx.beginPath();
        ctx.arc(obj.x, obj.y, r * pulse, 0, Math.PI * 2);
        ctx.fill();
      }

      if (isNear) {
        this.drawMarker(ctx, obj.x, obj.y - r - 22);
      }

      ctx.fillStyle = '#1e293b';
      ctx.font = 'bold 11px system-ui, sans-serif';
      ctx.textAlign = 'center';
      ctx.strokeStyle = '#fff';
      ctx.lineWidth = 3;
      ctx.strokeText(obj.label, obj.x, obj.y + r + 18);
      ctx.fillText(obj.label, obj.x, obj.y + r + 18);
      ctx.textAlign = 'left';
    }
  }

  drawInteractHint(
    ctx: CanvasRenderingContext2D,
    player: PlayPlayer,
    label: string,
    tx: number,
    ty: number,
  ): void {
    const c = player.center();
    ctx.strokeStyle = '#22d3ee';
    ctx.lineWidth = 2;
    ctx.setLineDash([6, 4]);
    ctx.beginPath();
    ctx.moveTo(c.x, c.y);
    ctx.lineTo(tx, ty);
    ctx.stroke();
    ctx.setLineDash([]);

    const hint = 'E — Interactuar';
    ctx.font = 'bold 12px system-ui, sans-serif';
    const tw = ctx.measureText(hint).width;
    const bx = tx - tw / 2 - 10;
    const by = ty - 58;
    ctx.fillStyle = 'rgba(15, 23, 42, 0.9)';
    ctx.beginPath();
    ctx.roundRect(bx, by, tw + 20, 26, 8);
    ctx.fill();
    ctx.strokeStyle = '#22d3ee';
    ctx.stroke();
    ctx.fillStyle = '#67e8f9';
    ctx.textAlign = 'center';
    ctx.fillText(hint, tx, by + 17);
    ctx.textAlign = 'left';

    this.drawMarker(ctx, tx, ty - 42);
  }

  private drawZoneTile(
    ctx: CanvasRenderingContext2D,
    sceneId: PlaySceneConfig['id'],
    zone: SceneZone,
  ): void {
    const tile = this.assets.zoneFor(sceneId, zone.tileId);
    if (tile) {
      ctx.drawImage(tile, zone.x, zone.y, zone.w, zone.h);
      ctx.fillStyle = 'rgba(15, 23, 42, 0.06)';
      ctx.fillRect(zone.x, zone.y, zone.w, zone.h);
    } else {
      ctx.fillStyle = zone.floor;
      ctx.fillRect(zone.x, zone.y, zone.w, zone.h);
    }

    const lx = zone.x + zone.w / 2;
    const ly = zone.y + zone.h - 10;
    const label = zone.label;
    ctx.font = 'bold 11px system-ui, sans-serif';
    ctx.textAlign = 'center';
    const tw = ctx.measureText(label).width;
    ctx.fillStyle = 'rgba(15, 23, 42, 0.72)';
    ctx.beginPath();
    ctx.roundRect(lx - tw / 2 - 8, ly - 14, tw + 16, 20, 6);
    ctx.fill();
    ctx.fillStyle = '#f8fafc';
    ctx.fillText(label, lx, ly);
    ctx.textAlign = 'left';
  }

  private drawDoors(ctx: CanvasRenderingContext2D, doors: SceneDoor[]): void {
    for (const d of doors) {
      ctx.fillStyle = '#44403c';
      if (d.orientation === 'horizontal') {
        ctx.fillRect(d.x, d.y, d.w, 6);
        ctx.fillStyle = '#fef3c7';
        ctx.fillRect(d.x + 6, d.y + 2, d.w - 12, Math.max(d.h - 4, 2));
        ctx.fillStyle = '#b45309';
        ctx.beginPath();
        ctx.arc(d.x + d.w - 14, d.y + d.h / 2, 3, 0, Math.PI * 2);
        ctx.fill();
      } else {
        ctx.fillRect(d.x, d.y, 6, d.h);
        ctx.fillStyle = '#fef3c7';
        ctx.fillRect(d.x + 2, d.y + 6, Math.max(d.w - 4, 2), d.h - 12);
        ctx.fillStyle = '#b45309';
        ctx.beginPath();
        ctx.arc(d.x + d.w / 2, d.y + d.h - 14, 3, 0, Math.PI * 2);
        ctx.fill();
      }
    }
  }

  private drawWalls(ctx: CanvasRenderingContext2D, scene: PlaySceneConfig): void {
    const grad = ctx.createLinearGradient(0, 0, 10, 0);
    grad.addColorStop(0, '#0f172a');
    grad.addColorStop(0.4, '#334155');
    grad.addColorStop(1, '#64748b');
    ctx.fillStyle = grad;
    for (const o of scene.obstacles) {
      if (o.w <= 14 || o.h <= 14) {
        ctx.fillRect(o.x, o.y, o.w, o.h);
        ctx.fillStyle = '#94a3b8';
        if (o.w > o.h) {
          ctx.fillRect(o.x, o.y + o.h - 3, o.w, 3);
        } else {
          ctx.fillRect(o.x + o.w - 3, o.y, 3, o.h);
        }
        ctx.fillStyle = grad;
        ctx.strokeStyle = '#0f172a';
        ctx.lineWidth = 1;
        ctx.strokeRect(o.x, o.y, o.w, o.h);
      }
    }
  }

  private drawFurniturePiece(ctx: CanvasRenderingContext2D, f: SceneFurniture): void {
    const color = f.color ?? '#94a3b8';
    ctx.fillStyle = color;
    ctx.strokeStyle = '#334155';
    ctx.lineWidth = 1.5;

    switch (f.kind) {
      case 'chair':
        ctx.fillRect(f.x + 4, f.y + 10, f.w - 8, f.h - 14);
        ctx.fillRect(f.x, f.y, f.w, 12);
        break;
      case 'sign':
        ctx.fillStyle = '#1e3a8a';
        ctx.fillRect(f.x, f.y, f.w, f.h);
        ctx.fillStyle = '#fbbf24';
        ctx.fillRect(f.x + 3, f.y + 3, f.w - 6, 4);
        if (f.label) {
          ctx.fillStyle = '#fff';
          ctx.font = 'bold 9px system-ui, sans-serif';
          ctx.textAlign = 'center';
          ctx.fillText(f.label, f.x + f.w / 2, f.y + f.h / 2 + 4);
          ctx.textAlign = 'left';
        }
        break;
      case 'bed':
        ctx.fillRect(f.x, f.y + 8, f.w, f.h - 8);
        ctx.fillStyle = '#fff';
        ctx.fillRect(f.x + 6, f.y, f.w - 12, 14);
        ctx.strokeRect(f.x, f.y, f.w, f.h);
        break;
      case 'cabinet':
        ctx.fillRect(f.x, f.y, f.w, f.h);
        for (let i = 0; i < 4; i++) {
          ctx.strokeRect(f.x + 4, f.y + 8 + i * 22, f.w - 8, 18);
        }
        break;
      default:
        ctx.fillRect(f.x, f.y, f.w, f.h);
        ctx.strokeRect(f.x, f.y, f.w, f.h);
    }
  }

  private drawCharacter(
    ctx: CanvasRenderingContext2D,
    x: number,
    y: number,
    color: string,
    label: string,
    facing: Facing,
    idlePhase: number,
    isNear: boolean,
  ): void {
    const bob = Math.sin(idlePhase) * (isNear ? 3 : 2);
    const sway = Math.sin(idlePhase * 0.7) * 1.5;
    const cy = y + bob;

    ctx.fillStyle = 'rgba(0,0,0,0.14)';
    ctx.beginPath();
    ctx.ellipse(x + sway, cy + 28, 22, 7, 0, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.roundRect(x - 18 + sway, cy - 4, 36, 38, 8);
    ctx.fill();

    ctx.fillStyle = '#fde4c4';
    ctx.beginPath();
    ctx.arc(x + sway, cy - 14, 14, 0, Math.PI * 2);
    ctx.fill();

    this.drawFacingEyes(ctx, x + sway, cy - 14, facing);

    if (isNear) {
      ctx.strokeStyle = '#22d3ee';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(x + sway, cy + 8, 28, 0, Math.PI * 2);
      ctx.stroke();
      this.drawMarker(ctx, x + sway, cy - 38);
    }

    ctx.fillStyle = '#0f172a';
    ctx.font = 'bold 11px system-ui, sans-serif';
    ctx.textAlign = 'center';
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 3;
    ctx.strokeText(label, x + sway, cy + 48);
    ctx.fillText(label, x + sway, cy + 48);
    ctx.textAlign = 'left';
  }

  private drawFacingEyes(
    ctx: CanvasRenderingContext2D,
    x: number,
    y: number,
    facing: Facing,
  ): void {
    ctx.fillStyle = '#1e293b';
    const o = 5;
    switch (facing) {
      case 'left':
        ctx.beginPath();
        ctx.arc(x - o, y, 2.5, 0, Math.PI * 2);
        ctx.fill();
        break;
      case 'right':
        ctx.beginPath();
        ctx.arc(x + o, y, 2.5, 0, Math.PI * 2);
        ctx.fill();
        break;
      case 'up':
        ctx.fillRect(x - 4, y - o, 3, 2);
        ctx.fillRect(x + 1, y - o, 3, 2);
        break;
      default:
        ctx.fillRect(x - 4, y + o - 2, 3, 2);
        ctx.fillRect(x + 1, y + o - 2, 3, 2);
    }
  }

  private drawMarker(ctx: CanvasRenderingContext2D, x: number, y: number): void {
    ctx.fillStyle = '#fbbf24';
    ctx.beginPath();
    ctx.arc(x, y, 12, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#b45309';
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.fillStyle = '#78350f';
    ctx.font = 'bold 14px system-ui, sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('!', x, y + 5);
    ctx.textAlign = 'left';
  }
}
