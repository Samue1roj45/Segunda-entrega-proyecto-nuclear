import { PlaySceneConfig } from '../data/scene-types';
import { AssetLoader } from './asset-loader';
import {
  createNpcActors,
  NpcActorState,
  syncNpcDefsFromActors,
  updateNpcActors,
} from './npc-actor';
import { PlayPlayer } from './play-player';
import { SceneRenderer } from './scene-renderer';
import { InteractableTarget } from './play-types';

export interface PlaySceneCallbacks {
  onInteract: (target: InteractableTarget) => void;
}

const VIEW_SCALE = 1.65;

export class PlaySceneEngine {
  private scene: PlaySceneConfig;
  private readonly player: PlayPlayer;
  private npcActors: NpcActorState[];
  private objects: PlaySceneConfig['objects'];
  private obstacles: PlaySceneConfig['obstacles'];

  private readonly assets = new AssetLoader();
  private renderer?: SceneRenderer;

  private ctx!: CanvasRenderingContext2D;
  private width = 0;
  private height = 0;
  private scale = VIEW_SCALE;
  private camX = 0;
  private camY = 0;
  private lastTs = 0;
  private rafId = 0;
  private paused = false;
  private nearest: InteractableTarget | null = null;
  private assetsReady = false;

  constructor(
    private readonly canvas: HTMLCanvasElement,
    scene: PlaySceneConfig,
    private readonly callbacks: PlaySceneCallbacks,
  ) {
    this.scene = scene;
    this.npcActors = createNpcActors(scene.npcs);
    this.objects = scene.objects;
    this.obstacles = scene.obstacles;
    this.player = new PlayPlayer(scene.playerStart);
    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('Canvas 2D no disponible');
    this.ctx = ctx;
  }

  async prepareAssets(): Promise<void> {
    try {
      await this.assets.loadAll();
    } catch (e) {
      console.warn('[PlaySceneEngine] Carga parcial de assets', e);
    }
    this.renderer = new SceneRenderer(this.assets);
    this.assetsReady = true;
  }

  loadScene(scene: PlaySceneConfig, options?: { preservePlayer?: boolean }): void {
    const px = options?.preservePlayer ? this.player.x : scene.playerStart.x;
    const py = options?.preservePlayer ? this.player.y : scene.playerStart.y;
    this.scene = scene;
    this.npcActors = createNpcActors(scene.npcs);
    this.objects = scene.objects;
    this.obstacles = scene.obstacles;
    this.player.x = px;
    this.player.y = py;
    this.nearest = null;
    this.updateCamera();
    this.resize();
  }

  updateSceneEntities(scene: PlaySceneConfig): void {
    this.scene = scene;
    this.npcActors = createNpcActors(scene.npcs);
    this.objects = scene.objects;
    this.obstacles = scene.obstacles;
  }

  getSceneId(): string {
    return this.scene.id;
  }

  start(): void {
    this.resize();
    this.lastTs = performance.now();
    this.rafId = requestAnimationFrame(this.loop);
  }

  stop(): void {
    cancelAnimationFrame(this.rafId);
  }

  setPaused(paused: boolean): void {
    this.paused = paused;
  }

  resize(): void {
    const parent = this.canvas.parentElement;
    if (!parent) return;
    const dpr = window.devicePixelRatio || 1;
    const w = Math.max(parent.clientWidth, 320);
    const h = Math.max(parent.clientHeight, 480);
    this.canvas.width = w * dpr;
    this.canvas.height = h * dpr;
    this.canvas.style.width = `${w}px`;
    this.canvas.style.height = `${h}px`;
    this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    this.width = w;
    this.height = h;
    this.scale = VIEW_SCALE;
    this.updateCamera();
  }

  handleKeyDown(code: string): boolean {
    if (code === 'KeyE' || code === 'Enter') {
      if (this.nearest) {
        this.callbacks.onInteract(this.nearest);
        return true;
      }
    }
    this.player.setKey(code, true);
    return false;
  }

  handleKeyUp(code: string): void {
    this.player.setKey(code, false);
  }

  handlePointer(clientX: number, clientY: number): void {
    const world = this.screenToWorld(clientX, clientY);
    const hit = this.pickAt(world.x, world.y);
    if (hit && hit.distance <= this.scene.interactDistance) {
      this.callbacks.onInteract(hit);
      return;
    }
    if (this.nearest) {
      this.callbacks.onInteract(this.nearest);
    }
  }

  private loop = (ts: number): void => {
    const dt = Math.min(0.05, (ts - this.lastTs) / 1000);
    this.lastTs = ts;
    if (!this.paused) {
      this.player.update(dt, this.scene.bounds, this.obstacles);
      updateNpcActors(this.npcActors, dt);
      this.nearest = this.findNearest();
      this.updateCamera();
      this.renderer?.tick(dt);
    }
    this.draw();
    this.rafId = requestAnimationFrame(this.loop);
  };

  private updateCamera(): void {
    const viewW = this.width / this.scale;
    const viewH = this.height / this.scale;
    const c = this.player.center();
    this.camX = Math.max(0, Math.min(this.scene.worldWidth - viewW, c.x - viewW / 2));
    this.camY = Math.max(0, Math.min(this.scene.worldHeight - viewH, c.y - viewH / 2));
  }

  private get npcs() {
    return syncNpcDefsFromActors(this.npcActors);
  }

  private findNearest(): InteractableTarget | null {
    const c = this.player.center();
    let best: InteractableTarget | null = null;
    let min = this.scene.interactDistance;

    for (const npc of this.npcs) {
      const d = Math.hypot(c.x - npc.x, c.y - npc.y);
      if (d < min) {
        min = d;
        best = { kind: 'npc', id: npc.id, label: npc.label, distance: d };
      }
    }
    for (const obj of this.objects) {
      const d = Math.hypot(c.x - obj.x, c.y - obj.y);
      if (d < min) {
        min = d;
        best = { kind: 'object', id: obj.id, label: obj.label, distance: d };
      }
    }
    return best;
  }

  private pickAt(wx: number, wy: number): InteractableTarget | null {
    const c = this.player.center();
    for (const npc of this.npcs) {
      if (Math.hypot(wx - npc.x, wy - npc.y) < npc.radius + 14) {
        return {
          kind: 'npc',
          id: npc.id,
          label: npc.label,
          distance: Math.hypot(c.x - npc.x, c.y - npc.y),
        };
      }
    }
    for (const obj of this.objects) {
      if (Math.hypot(wx - obj.x, wy - obj.y) < obj.radius + 12) {
        return {
          kind: 'object',
          id: obj.id,
          label: obj.label,
          distance: Math.hypot(c.x - obj.x, c.y - obj.y),
        };
      }
    }
    return null;
  }

  private screenToWorld(clientX: number, clientY: number): { x: number; y: number } {
    const rect = this.canvas.getBoundingClientRect();
    const sx = clientX - rect.left;
    const sy = clientY - rect.top;
    return { x: sx / this.scale + this.camX, y: sy / this.scale + this.camY };
  }

  private draw(): void {
    const ctx = this.ctx;
    ctx.fillStyle = '#475569';
    ctx.fillRect(0, 0, this.width, this.height);

    if (!this.assetsReady || !this.renderer) {
      ctx.fillStyle = '#f1f5f9';
      ctx.font = '16px system-ui, sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('Cargando escenario…', this.width / 2, this.height / 2);
      ctx.textAlign = 'left';
      return;
    }

    ctx.save();
    ctx.scale(this.scale, this.scale);
    ctx.translate(-this.camX, -this.camY);

    this.renderer.drawBackground(ctx, this.scene);
    this.renderer.drawZones(ctx, this.scene);
    this.renderer.drawFurniture(ctx, this.scene);
    this.renderer.drawObjects(ctx, this.scene, this.nearest?.kind === 'object' ? this.nearest.id : null);
    this.renderer.drawNpcs(
      ctx,
      this.npcActors,
      this.nearest?.kind === 'npc' ? this.nearest.id : null,
    );
    this.renderer.drawPlayer(ctx, this.player);

    if (this.nearest) {
      const target =
        this.nearest.kind === 'npc'
          ? this.npcs.find((n) => n.id === this.nearest!.id)
          : this.objects.find((o) => o.id === this.nearest!.id);
      if (target) {
        this.renderer.drawInteractHint(ctx, this.player, this.nearest.label, target.x, target.y);
      }
    }

    ctx.restore();
  }
}
