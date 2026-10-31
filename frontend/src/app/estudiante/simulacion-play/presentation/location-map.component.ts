import { CommonModule } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import { InstitutionMapGraph } from '../data/room-world-layout';
import { ImmersiveRoomDef } from '../data/immersive-room.types';

@Component({
  selector: 'app-location-map',
  imports: [CommonModule, MatIconModule],
  template: `
    <button
      type="button"
      class="map-fab"
      (click)="toggle()"
      [attr.aria-expanded]="open()"
      [attr.aria-label]="open() ? 'Cerrar mapa de ubicaciones' : 'Abrir mapa de ubicaciones'">
      <mat-icon>{{ open() ? 'close' : 'map' }}</mat-icon>
      <span class="fab-label">Mapa</span>
    </button>

    @if (open()) {
      <div class="map-overlay" role="presentation" (click)="close()">
        <nav class="loc-map" [attr.aria-label]="mapLabel()" (click)="$event.stopPropagation()">
          <header class="map-head">
            <span class="loc-title"><mat-icon>map</mat-icon> {{ mapLabel() }}</span>
            <button type="button" class="map-close" (click)="close()" aria-label="Cerrar mapa">
              <mat-icon>close</mat-icon>
            </button>
          </header>

          @if (graph(); as g) {
            <svg class="map-graph" viewBox="0 0 100 100" aria-hidden="true">
              @for (edge of g.edges; track edgeKey(edge)) {
                <line
                  [attr.x1]="nodeX(edge[0])"
                  [attr.y1]="nodeY(edge[0])"
                  [attr.x2]="nodeX(edge[1])"
                  [attr.y2]="nodeY(edge[1])"
                  class="edge" />
              }
              @for (n of g.nodes; track n.id) {
                <g
                  class="node"
                  [class.active]="currentRoom() === n.id"
                  [class.visited]="visited().has(n.id)"
                  (click)="pickRoom(n.id)">
                  <circle [attr.cx]="n.x" [attr.cy]="n.y" r="5" />
                  <text [attr.x]="n.x" [attr.y]="n.y - 8" text-anchor="middle">{{ shortLabel(n.label) }}</text>
                </g>
              }
            </svg>
          }

          <ul class="room-list">
            @for (room of rooms(); track room.id) {
              <li>
                <button
                  type="button"
                  [class.active]="currentRoom() === room.id"
                  [class.visited]="visited().has(room.id)"
                  (click)="pickRoom(room.id)">
                  <span class="dot"></span>
                  <span class="label">{{ room.label }}</span>
                </button>
              </li>
            }
          </ul>
        </nav>
      </div>
    }
  `,
  styles: `
    :host {
      position: absolute;
      left: 0;
      bottom: 0;
      z-index: 18;
      pointer-events: none;
    }

    .map-fab,
    .map-overlay,
    .loc-map {
      pointer-events: auto;
    }

    .map-fab {
      position: absolute;
      bottom: 3.25rem;
      left: 1rem;
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      padding: 0.5rem 0.85rem;
      border: 1px solid rgba(56, 189, 248, 0.45);
      border-radius: 999px;
      background: rgba(15, 23, 42, 0.92);
      color: #e0f2fe;
      cursor: pointer;
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      box-shadow: 0 8px 28px rgba(0, 0, 0, 0.45);
      backdrop-filter: blur(8px);
      font-family: system-ui, sans-serif;
      transition: transform 0.2s ease, background 0.2s ease;
    }

    .map-fab:hover {
      background: rgba(30, 58, 95, 0.95);
      transform: translateY(-1px);
    }

    .map-fab mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
      color: #7dd3fc;
    }

    .fab-label {
      display: none;
    }

    @media (min-width: 900px) {
      .fab-label {
        display: inline;
      }
    }

    .map-overlay {
      position: fixed;
      inset: 0;
      z-index: 60;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
      background: rgba(2, 6, 23, 0.72);
      backdrop-filter: blur(6px);
      animation: mapFadeIn 0.25s ease;
    }

    @keyframes mapFadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    .loc-map {
      width: min(420px, 94vw);
      max-height: min(78vh, 640px);
      overflow-y: auto;
      padding: 1rem 1.1rem;
      border-radius: 12px;
      background: rgba(15, 23, 42, 0.98);
      border: 1px solid rgba(148, 163, 184, 0.3);
      box-shadow: 0 24px 64px rgba(0, 0, 0, 0.55);
      animation: mapSlideUp 0.3s cubic-bezier(0.22, 1, 0.36, 1);
    }

    @keyframes mapSlideUp {
      from { opacity: 0; transform: translateY(16px) scale(0.97); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }

    .map-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
      margin-bottom: 0.65rem;
    }

    .loc-title {
      display: flex;
      align-items: center;
      gap: 0.35rem;
      font-size: 0.7rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.1em;
      color: #cbd5e1;
      font-family: system-ui, sans-serif;
    }

    .loc-title mat-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
      color: #7dd3fc;
    }

    .map-close {
      border: none;
      background: rgba(255, 255, 255, 0.06);
      color: #94a3b8;
      border-radius: 8px;
      padding: 0.25rem;
      cursor: pointer;
      display: flex;
    }

    .map-graph {
      width: 100%;
      height: auto;
      min-height: 120px;
      max-height: 180px;
      margin-bottom: 0.65rem;
      display: block;
    }

    .edge {
      stroke: rgba(100, 116, 139, 0.45);
      stroke-width: 1.2;
    }

    .node {
      cursor: pointer;
    }

    .node circle {
      fill: #475569;
      stroke: #1e293b;
      stroke-width: 1;
    }

    .node.visited circle {
      fill: #4ade80;
    }

    .node.active circle {
      fill: #38bdf8;
      stroke: #0ea5e9;
      stroke-width: 2;
    }

    .node text {
      fill: #94a3b8;
      font-size: 5px;
      font-family: system-ui, sans-serif;
      pointer-events: none;
    }

    .node.active text {
      fill: #e0f2fe;
      font-weight: 700;
    }

    .room-list {
      list-style: none;
      margin: 0;
      padding: 0.5rem 0 0;
      display: flex;
      flex-direction: column;
      gap: 0.15rem;
      border-top: 1px solid rgba(100, 116, 139, 0.25);
    }

    .room-list button {
      width: 100%;
      text-align: left;
      display: flex;
      align-items: center;
      gap: 0.45rem;
      padding: 0.4rem 0.5rem;
      border: none;
      border-radius: 6px;
      background: transparent;
      color: #94a3b8;
      font-size: 0.8rem;
      cursor: pointer;
      font-family: system-ui, sans-serif;
    }

    .room-list button:hover {
      background: rgba(255, 255, 255, 0.05);
      color: #e2e8f0;
    }

    .room-list button.active {
      background: rgba(56, 189, 248, 0.14);
      color: #e0f2fe;
      font-weight: 600;
    }

    .room-list button.visited .dot {
      background: #4ade80;
    }

    .dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #475569;
      flex-shrink: 0;
    }
  `,
})
export class LocationMapComponent {
  readonly rooms = input.required<ImmersiveRoomDef[]>();
  readonly currentRoom = input.required<string>();
  readonly visited = input<Set<string>>(new Set());
  readonly mapLabel = input('Mapa de ubicaciones');
  readonly graph = input<InstitutionMapGraph | null>(null);

  readonly navigate = output<string>();

  readonly open = signal(false);

  private readonly nodeMap = computed(() => {
    const g = this.graph();
    if (!g) return new Map<string, { x: number; y: number }>();
    return new Map(g.nodes.map((n) => [n.id, { x: n.x, y: n.y }]));
  });

  toggle(): void {
    this.open.update((v) => !v);
  }

  close(): void {
    this.open.set(false);
  }

  pickRoom(id: string): void {
    this.navigate.emit(id);
    this.close();
  }

  nodeX(id: string): number {
    return this.nodeMap().get(id)?.x ?? 50;
  }

  nodeY(id: string): number {
    return this.nodeMap().get(id)?.y ?? 50;
  }

  edgeKey(edge: [string, string]): string {
    return `${edge[0]}-${edge[1]}`;
  }

  shortLabel(label: string): string {
    return label.length > 12 ? `${label.slice(0, 11)}…` : label;
  }
}
