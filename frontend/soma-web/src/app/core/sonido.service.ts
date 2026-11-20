import { Injectable, inject } from '@angular/core';
import { PreferenciasService } from './preferencias.service';

/** Sonidos suaves generados con WebAudio (sin archivos). Vienen apagados; se activan en Perfil → Apariencia. */
@Injectable({ providedIn: 'root' })
export class SonidoService {
  private readonly prefs = inject(PreferenciasService);
  private ctx: AudioContext | null = null;

  private tono(frecuencias: number[], dur = .09, vol = .05): void {
    if (!this.prefs.sonido()) return;
    try {
      this.ctx ??= new AudioContext();
      const t0 = this.ctx.currentTime;
      frecuencias.forEach((f, i) => {
        const o = this.ctx!.createOscillator(), g = this.ctx!.createGain();
        o.type = 'sine'; o.frequency.value = f;
        g.gain.setValueAtTime(0, t0 + i * dur); g.gain.linearRampToValueAtTime(vol, t0 + i * dur + .01); g.gain.exponentialRampToValueAtTime(.0001, t0 + i * dur + dur + .12);
        o.connect(g).connect(this.ctx!.destination); o.start(t0 + i * dur); o.stop(t0 + i * dur + dur + .15);
      });
    } catch { /* sin audio */ }
  }

  clic(): void { this.tono([660], .04, .03); }
  exito(): void { this.tono([523, 659, 784]); }
  error(): void { this.tono([330, 247], .12); }
  aviso(): void { this.tono([587, 587], .07); }
}
