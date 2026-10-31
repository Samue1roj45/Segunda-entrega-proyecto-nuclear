import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import { presentationFor } from '../data/character-presentation';
import { COMISARIA_OBS_MAP } from '../data/comisaria-room-observations.content';
import { HOSPITAL_OBSERVATIONS } from '../data/hospital-room-observations.content';
import { ImmersiveRoomDef, RoomHotspot } from '../data/immersive-room.types';
import {
  COMISARIA_MAP_GRAPH,
  HOSPITAL_MAP_GRAPH,
  WORLD_HEIGHT,
  WORLD_WIDTH,
} from '../data/room-world-layout';
import { PlayNpcDef, PlayObjectDef } from '../data/scene-types';
import { SceneId } from '../data/scene-types';
import { HotspotPx, RoomExplorationEngine } from '../engine/room-exploration.engine';
import { InteractableTarget } from '../engine/play-types';
import { ExpedienteCasoService } from '../services/expediente-caso.service';
import { LocationMapComponent } from './location-map.component';

const HOSPITAL_OBS_MAP = new Map(HOSPITAL_OBSERVATIONS.map((o) => [o.id, o]));

export interface ProximityTarget {
  kind: 'npc' | 'object' | 'observe' | 'exit';
  label: string;
  hotspot: RoomHotspot;
  hotspotPx: HotspotPx;
  npc?: PlayNpcDef;
  object?: PlayObjectDef;
  observe?: { id: string; label: string; text: string };
  exitTo?: string;
}

@Component({
  selector: 'app-immersive-scene',
  imports: [CommonModule, MatIconModule, LocationMapComponent],
  templateUrl: './immersive-scene.component.html',
  styleUrls: ['./immersive-scene.component.scss', './room-parallax-layers.scss'],
})
export class ImmersiveSceneComponent implements AfterViewInit, OnDestroy {
  @ViewChild('viewport') viewportRef?: ElementRef<HTMLDivElement>;

  private readonly expediente = inject(ExpedienteCasoService);
  readonly explore = new RoomExplorationEngine();

  readonly institution = input.required<SceneId>();
  readonly roomId = input.required<string>();
  readonly rooms = input.required<ImmersiveRoomDef[]>();
  readonly roomMap = input.required<Map<string, ImmersiveRoomDef>>();
  readonly npcs = input.required<PlayNpcDef[]>();
  readonly objects = input.required<PlayObjectDef[]>();
  readonly paused = input(false);
  readonly ubicacionLabel = input('Institución');
  readonly institutionTitle = input('Institución');
  readonly mapLabel = input('Mapa de ubicaciones');

  readonly roomChange = output<string>();
  readonly interact = output<InteractableTarget>();
  readonly observationRecorded = output<void>();

  readonly worldW = WORLD_WIDTH;
  readonly worldH = WORLD_HEIGHT;

  readonly camTransform = signal('translate(0px, 0px)');
  readonly parallaxMid = signal('translate(0px, 0px)');
  readonly parallaxFront = signal('translate(0px, 0px)');
  readonly walkBob = signal(0);
  readonly playerPx = signal({ x: 0, y: 0 });
  readonly facing = signal<'down' | 'up' | 'left' | 'right'>('down');
  readonly isWalking = signal(false);
  readonly visitedRooms = signal(new Set<string>());
  readonly transitioning = signal(false);
  readonly transitionCurtain = signal(false);
  readonly locationCard = signal<{ institution: string; room: string } | null>(null);
  readonly fieldNote = signal<string | null>(null);
  readonly proximity = signal<ProximityTarget | null>(null);
  readonly entityScale = signal(1.15);
  readonly backdropZoom = signal(1.1);

  readonly room = computed(() => this.roomMap().get(this.roomId())!);

  readonly mapGraph = computed(() =>
    this.institution() === 'hospital' ? HOSPITAL_MAP_GRAPH : COMISARIA_MAP_GRAPH,
  );

  readonly showAmbientWalkers = computed(() => {
    const r = this.roomId();
    return r === 'sala-espera' || r === 'recepcion' || r === 'entrada' || r === 'oficina-atencion';
  });

  readonly proximityLabel = computed(() => {
    const p = this.proximity();
    if (!p) return null;
    if (p.kind === 'npc') return `Entrevistar — ${p.label}`;
    if (p.kind === 'object') return `Revisar — ${p.label}`;
    if (p.kind === 'observe') return `Observar — ${p.label}`;
    return `Ir a — ${p.label}`;
  });

  private readonly npcMap = computed(() => new Map(this.npcs().map((n) => [n.id, n])));
  private readonly objectMap = computed(() => new Map(this.objects().map((o) => [o.id, o])));
  private readonly obsMap = computed(() =>
    this.institution() === 'hospital' ? HOSPITAL_OBS_MAP : COMISARIA_OBS_MAP,
  );

  private rafId = 0;
  private lastFrame = 0;
  private resizeObserver?: ResizeObserver;
  private toastTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    effect(() => {
      const inst = this.institution();
      const rid = this.roomId();
      this.explore.loadRoom(inst, rid);
      this.syncFromEngine();
      this.visitedRooms.update((s) => {
        const n = new Set(s);
        n.add(rid);
        return n;
      });
    });
    effect(() => {
      if (this.paused()) this.explore.clearKeys();
    });
  }

  ngAfterViewInit(): void {
    const el = this.viewportRef?.nativeElement;
    if (!el) return;
    const applySize = (): void => {
      const w = el.clientWidth;
      const h = el.clientHeight;
      this.explore.setViewport(w, h);
      this.updateVisualScale(w, h);
    };
    applySize();
    this.resizeObserver = new ResizeObserver(applySize);
    this.resizeObserver.observe(el);
    el.focus();
    this.lastFrame = performance.now();
    this.loop();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.rafId);
    this.resizeObserver?.disconnect();
    this.explore.clearKeys();
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }

  private loop = (): void => {
    const now = performance.now();
    const dt = Math.min(0.05, (now - this.lastFrame) / 1000);
    this.lastFrame = now;
    if (!this.paused() && !this.transitioning()) {
      this.explore.tick(dt);
      this.syncFromEngine();
      this.updateProximity();
    }
    this.rafId = requestAnimationFrame(this.loop);
  };

  private updateVisualScale(viewportW: number, viewportH: number): void {
    const wRef = 1280;
    const hRef = 720;
    const wRatio = viewportW / wRef;
    const hRatio = viewportH / hRef;
    const blend = Math.min(wRatio, hRatio, 1.65);
    this.entityScale.set(Math.min(Math.max(1.05 + blend * 0.22, 1.05), 1.72));
    this.backdropZoom.set(Math.min(Math.max(1.06 + blend * 0.12, 1.06), 1.35));
  }

  private syncFromEngine(): void {
    this.camTransform.set(this.explore.cameraTransform());
    const cx = this.explore.camX;
    const cy = this.explore.camY;
    this.parallaxMid.set(`translate(${Math.round(-cx * 0.06)}px, ${Math.round(-cy * 0.04)}px)`);
    this.parallaxFront.set(`translate(${Math.round(-cx * 0.12)}px, ${Math.round(-cy * 0.08)}px)`);
    this.walkBob.set(this.explore.walking ? Math.sin(this.explore.walkPhase) * 3 : 0);
    this.playerPx.set({ x: this.explore.px, y: this.explore.py });
    this.facing.set(this.explore.facing);
    this.isWalking.set(this.explore.walking);
  }

  private updateProximity(): void {
    let best: ProximityTarget | null = null;
    let bestD = Infinity;
    for (const h of this.room().hotspots) {
      const px = this.explore.hotspotToPx(h);
      const d = this.explore.distToHotspot(px);
      if (d > this.explore.interactRadius) continue;
      if (d >= bestD) continue;
      bestD = d;
      best = this.hotspotToTarget(h, px);
    }
    this.proximity.set(best);
  }

  private hotspotToTarget(h: RoomHotspot, px: HotspotPx): ProximityTarget | null {
    if (h.npcId) {
      const npc = this.npcMap().get(h.npcId);
      if (!npc) return null;
      const pres = presentationFor(npc.id, npc.label, npc.role);
      return { kind: 'npc', label: pres.nombreCompleto, hotspot: h, hotspotPx: px, npc };
    }
    if (h.objectId) {
      const obj = this.objectMap().get(h.objectId);
      if (!obj) return null;
      return { kind: 'object', label: obj.label, hotspot: h, hotspotPx: px, object: obj };
    }
    if (h.observeId) {
      const obs = this.obsMap().get(h.observeId);
      if (!obs) return null;
      return { kind: 'observe', label: obs.label, hotspot: h, hotspotPx: px, observe: obs };
    }
    if (h.exitTo) {
      return {
        kind: 'exit',
        label: h.exitLabel ?? 'Otra sala',
        hotspot: h,
        hotspotPx: px,
        exitTo: h.exitTo,
      };
    }
    return null;
  }

  focusViewport(): void {
    this.viewportRef?.nativeElement?.focus();
  }

  onKeyDown(event: KeyboardEvent): void {
    if (this.paused()) return;
    this.explore.setKey(event.code, true);
    if (event.code === 'KeyE') {
      event.preventDefault();
      this.tryInteract();
    }
  }

  onKeyUp(event: KeyboardEvent): void {
    this.explore.setKey(event.code, false);
  }

  onViewportClick(event: MouseEvent): void {
    if (this.paused() || this.transitioning()) return;
    const el = this.viewportRef?.nativeElement;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const w = this.explore.screenToWorld(event.clientX, event.clientY, rect);
    this.explore.setWalkTarget(w.x, w.y);
  }

  tryInteract(): void {
    const p = this.proximity();
    if (!p) return;
    if (p.kind === 'npc' && p.npc) {
      this.interact.emit({ kind: 'npc', id: p.npc.id, label: p.npc.label, distance: 0 });
    } else if (p.kind === 'object' && p.object) {
      this.interact.emit({ kind: 'object', id: p.object.id, label: p.object.label, distance: 0 });
    } else if (p.kind === 'observe' && p.observe) {
      this.registerObservation(p.observe);
    } else if (p.kind === 'exit' && p.exitTo) {
      this.goToRoom(p.exitTo);
    }
  }

  onEntityClick(event: MouseEvent, h: RoomHotspot): void {
    event.stopPropagation();
    if (!this.explore.isNear(this.explore.hotspotToPx(h))) return;
    const target = this.hotspotToTarget(h, this.explore.hotspotToPx(h));
    if (!target) return;
    this.proximity.set(target);
    this.tryInteract();
  }

  charPres(npc: PlayNpcDef) {
    return presentationFor(npc.id, npc.label, npc.role);
  }

  npcById(id: string) {
    return this.npcMap().get(id);
  }

  objectById(id: string) {
    return this.objectMap().get(id);
  }

  observationById(id: string) {
    return this.obsMap().get(id);
  }

  hasObserved(id: string) {
    return this.expediente.hasObserved(id);
  }

  isNearHotspot(h: RoomHotspot): boolean {
    return this.explore.isNear(this.explore.hotspotToPx(h));
  }

  hotspotStyle(h: RoomHotspot): Record<string, string> {
    const px = this.explore.hotspotToPx(h);
    return {
      left: `${px.x}px`,
      top: `${px.y}px`,
      width: `${px.w}px`,
      height: `${px.h}px`,
    };
  }

  playerStyle(): Record<string, string> {
    const p = this.playerPx();
    return {
      left: `${p.x}px`,
      top: `${p.y}px`,
    };
  }

  obsVisualKind(id: string): string {
    const k = id.toLowerCase();
    if (k.includes('monitor') || k.includes('tv')) return 'prop-monitor';
    if (k.includes('ambulancia') || k.includes('camilla') || k.includes('medicacion')) return 'prop-equipment';
    if (k.includes('cartel') || k.includes('senal') || k.includes('avisos') || k.includes('urgencias') || k.includes('escudo')) return 'prop-sign';
    if (k.includes('archivo') || k.includes('expediente') || k.includes('radicados') || k.includes('tramites') || k.includes('ruta')) return 'prop-folder';
    if (k.includes('mostrador') || k.includes('escritorio')) return 'prop-desk';
    if (k.includes('revistas') || k.includes('historia')) return 'prop-paper';
    return 'prop-glint';
  }

  objectVisualKind(id: string): string {
    const k = id.toLowerCase();
    if (k.includes('historia')) return 'prop-chart';
    if (k.includes('protocolo')) return 'prop-binder';
    if (k.includes('expediente') || k.includes('denuncia') || k.includes('reporte') || k.includes('ruta')) return 'prop-folder-stack';
    return 'prop-document';
  }

  hotspotKey(h: RoomHotspot): string {
    return h.npcId ?? h.objectId ?? h.observeId ?? h.exitTo ?? 'x';
  }

  isProximityTarget(h: RoomHotspot): boolean {
    const p = this.proximity();
    if (!p) return false;
    return this.hotspotKey(p.hotspot) === this.hotspotKey(h);
  }

  goToRoom(id: string): void {
    if (id === this.roomId()) return;
    const next = this.roomMap().get(id);
    this.transitioning.set(true);
    this.transitionCurtain.set(true);
    this.explore.clearKeys();
    window.setTimeout(() => {
      this.roomChange.emit(id);
      this.locationCard.set({
        institution: this.institutionTitle(),
        room: next?.label ?? id,
      });
      window.setTimeout(() => this.locationCard.set(null), 3200);
      window.setTimeout(() => {
        this.transitioning.set(false);
        this.transitionCurtain.set(false);
        this.viewportRef?.nativeElement?.focus();
      }, 880);
    }, 720);
  }

  private registerObservation(obs: { id: string; label: string; text: string }): void {
    if (!this.expediente.registerObservacion(obs.id, obs.label, obs.text, this.ubicacionLabel())) return;
    this.fieldNote.set(`Anotación: ${obs.label} — registrada en bitácora.`);
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => this.fieldNote.set(null), 3200);
    this.observationRecorded.emit();
  }
}
