import { Injectable, signal } from '@angular/core';

import { SceneId } from '../data/scene-types';

/** Ambiente sonoro sintético (sin archivos externos). */
@Injectable({ providedIn: 'root' })
export class AmbientAudioService {
  private ctx: AudioContext | null = null;
  private master: GainNode | null = null;
  private loops: OscillatorNode[] = [];
  private timers: ReturnType<typeof setInterval>[] = [];
  readonly enabled = signal(true);
  readonly active = signal(false);

  async startFor(sceneId: SceneId): Promise<void> {
    if (!this.enabled()) return;
    this.stop();
    try {
      this.ctx = new AudioContext();
      await this.ctx.resume();
      this.master = this.ctx.createGain();
      this.master.gain.value = sceneId === 'hospital' ? 0.026 : 0.022;
      this.master.connect(this.ctx.destination);

      const hum = this.ctx.createOscillator();
      hum.type = 'sine';
      hum.frequency.value = sceneId === 'hospital' ? 72 : 58;
      hum.connect(this.master);
      hum.start();
      this.loops.push(hum);

      if (sceneId === 'hospital') {
        this.scheduleHospital();
      } else {
        this.scheduleComisaria();
      }

      this.active.set(true);
    } catch {
      this.active.set(false);
    }
  }

  private scheduleHospital(): void {
    this.timers.push(
      setInterval(() => this.beep(880, 0.06, 0.018), 2400 + Math.random() * 800),
    );
    this.timers.push(
      setInterval(() => this.beep(1200, 0.04, 0.012), 5200 + Math.random() * 1200),
    );
    this.timers.push(setInterval(() => this.noiseBurst(0.04, 0.006), 9000 + Math.random() * 4000));
    this.timers.push(setInterval(() => this.noiseBurst(0.08, 0.009), 14000 + Math.random() * 5000));
    this.timers.push(
      setInterval(() => {
        this.beep(220, 0.15, 0.01);
        window.setTimeout(() => this.beep(280, 0.12, 0.008), 180);
      }, 22000 + Math.random() * 8000),
    );
    this.timers.push(setInterval(() => this.footstepDistant(), 11000 + Math.random() * 5000));
    this.timers.push(
      setInterval(() => {
        this.beep(180, 0.08, 0.007);
        window.setTimeout(() => this.noiseBurst(0.06, 0.005), 120);
      }, 16000 + Math.random() * 6000),
    );
    this.timers.push(
      setInterval(() => this.beep(640, 0.05, 0.01), 18000 + Math.random() * 7000),
    );
  }

  private scheduleComisaria(): void {
    this.timers.push(setInterval(() => this.clickBurst(3), 1800 + Math.random() * 600));
    this.timers.push(setInterval(() => this.clickBurst(5), 4500 + Math.random() * 1500));
    this.timers.push(setInterval(() => this.beep(440, 0.2, 0.008), 12000 + Math.random() * 4000));
    this.timers.push(setInterval(() => this.noiseBurst(0.03, 0.005), 7000 + Math.random() * 3000));
    this.timers.push(setInterval(() => this.printerWhir(), 14000 + Math.random() * 8000));
    this.timers.push(setInterval(() => this.murmurBurst(), 20000 + Math.random() * 10000));
    this.timers.push(setInterval(() => this.clickBurst(2), 3200 + Math.random() * 900));
  }

  private footstepDistant(): void {
    for (let i = 0; i < 4; i++) {
      window.setTimeout(() => this.beep(90 + Math.random() * 30, 0.04, 0.005), i * 280);
    }
  }

  private printerWhir(): void {
    this.noiseBurst(0.12, 0.007);
    window.setTimeout(() => this.beep(320, 0.06, 0.006), 140);
    window.setTimeout(() => this.noiseBurst(0.05, 0.004), 400);
  }

  private murmurBurst(): void {
    this.noiseBurst(0.18, 0.004);
    window.setTimeout(() => this.noiseBurst(0.1, 0.003), 400);
  }

  private beep(freq: number, duration: number, volume: number): void {
    if (!this.ctx || !this.master) return;
    const t = this.ctx.currentTime;
    const osc = this.ctx.createOscillator();
    const g = this.ctx.createGain();
    g.gain.setValueAtTime(0, t);
    g.gain.linearRampToValueAtTime(volume, t + 0.01);
    g.gain.exponentialRampToValueAtTime(0.001, t + duration);
    osc.frequency.value = freq;
    osc.type = 'sine';
    osc.connect(g);
    g.connect(this.master);
    osc.start(t);
    osc.stop(t + duration + 0.02);
  }

  private noiseBurst(duration: number, volume: number): void {
    if (!this.ctx || !this.master) return;
    const rate = this.ctx.sampleRate;
    const len = Math.floor(rate * duration);
    const buf = this.ctx.createBuffer(1, len, rate);
    const d = buf.getChannelData(0);
    for (let i = 0; i < len; i++) d[i] = (Math.random() * 2 - 1) * 0.4;
    const src = this.ctx.createBufferSource();
    src.buffer = buf;
    const g = this.ctx.createGain();
    g.gain.value = volume;
    src.connect(g);
    g.connect(this.master);
    src.start();
  }

  private clickBurst(count: number): void {
    for (let i = 0; i < count; i++) {
      window.setTimeout(() => this.beep(900 + Math.random() * 400, 0.025, 0.006), i * 55);
    }
  }

  stop(): void {
    for (const id of this.timers) clearInterval(id);
    this.timers = [];
    for (const n of this.loops) {
      try {
        n.stop();
        n.disconnect();
      } catch {
        /* noop */
      }
    }
    this.loops = [];
    if (this.ctx) {
      void this.ctx.close();
      this.ctx = null;
    }
    this.master = null;
    this.active.set(false);
  }

  toggle(): void {
    this.enabled.update((v) => !v);
    if (!this.enabled()) this.stop();
  }
}
