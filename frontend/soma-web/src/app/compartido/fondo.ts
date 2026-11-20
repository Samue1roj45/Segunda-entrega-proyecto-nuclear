import { Component } from '@angular/core';

/**
 * Fondo de SOMA: cielo lila con niebla, montañas lejanas, agua y cintas de cristal.
 * Todo es SVG/CSS (sin imágenes), así escala a cualquier pantalla.
 */
@Component({
  selector: 'soma-fondo',
  template: `
    <svg class="escena" viewBox="0 0 1600 900" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
      <defs>
        <linearGradient id="cielo" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="var(--d-d9d0ee)" /><stop offset=".45" stop-color="var(--d-ece7f7)" /><stop offset="1" stop-color="#cfc4e6" />
        </linearGradient>
        <linearGradient id="monte" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="#a99bc9" /><stop offset="1" stop-color="var(--d-e5def3)" />
        </linearGradient>
        <linearGradient id="agua" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="var(--d-e9e3f6)" /><stop offset="1" stop-color="#b7a8d8" />
        </linearGradient>
        <linearGradient id="cinta" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stop-color="#ffffff" stop-opacity=".0" />
          <stop offset=".35" stop-color="#ffffff" stop-opacity=".85" />
          <stop offset=".6" stop-color="#b9a6e0" stop-opacity=".55" />
          <stop offset="1" stop-color="#ffffff" stop-opacity=".0" />
        </linearGradient>
        <radialGradient id="esfera" cx=".35" cy=".3" r=".75">
          <stop offset="0" stop-color="#ffffff" stop-opacity=".95" />
          <stop offset=".45" stop-color="#e2d9f3" stop-opacity=".55" />
          <stop offset="1" stop-color="#8a76b8" stop-opacity=".45" />
        </radialGradient>
        <filter id="niebla"><feGaussianBlur stdDeviation="18" /></filter>
        <filter id="suave"><feGaussianBlur stdDeviation="2.5" /></filter>
      </defs>

      <rect width="1600" height="900" fill="url(#cielo)" />
      <ellipse cx="760" cy="300" rx="520" ry="120" fill="#fff" opacity=".55" filter="url(#niebla)" />

      <!-- montañas lejanas -->
      <g filter="url(#suave)" opacity=".85">
      <path d="M380 640 L470 470 L520 520 L600 380 L650 450 L700 410 L780 560 L860 640 Z" fill="url(#monte)" opacity=".55" />
      <path d="M820 640 L900 330 L940 300 L990 420 L1040 380 L1110 520 L1180 640 Z" fill="url(#monte)" opacity=".7" />
      <path d="M1100 640 L1170 520 L1230 560 L1290 640 Z" fill="url(#monte)" opacity=".45" />
      </g>
      <rect x="0" y="560" width="1600" height="120" fill="var(--d-efeaf8)" opacity=".75" filter="url(#niebla)" />

      <!-- agua -->
      <rect x="0" y="640" width="1600" height="260" fill="url(#agua)" />
      <g stroke="#fff" stroke-opacity=".55" stroke-width="2">
        <line x1="300" y1="690" x2="700" y2="690" /><line x1="820" y1="720" x2="1200" y2="720" />
        <line x1="150" y1="770" x2="520" y2="770" /><line x1="900" y1="810" x2="1400" y2="810" />
      </g>

      <!-- cintas de cristal -->
      <g fill="none" stroke-linecap="round" filter="url(#suave)">
        <path class="cinta c1" d="M-80 760 C 220 600, 360 860, 640 720 S 1100 520, 1700 640" stroke="url(#cinta)" stroke-width="46" />
        <path class="cinta c2" d="M-60 180 C 160 60, 260 420, 120 620 S 260 980, 520 860" stroke="url(#cinta)" stroke-width="30" />
        <path class="cinta c3" d="M1700 120 C 1420 220, 1500 520, 1320 700 S 1100 980, 900 900" stroke="url(#cinta)" stroke-width="34" />
      </g>
      <g fill="none" stroke="#fff" stroke-opacity=".9" stroke-width="2">
        <path d="M-80 760 C 220 600, 360 860, 640 720 S 1100 520, 1700 640" />
        <path d="M-60 180 C 160 60, 260 420, 120 620 S 260 980, 520 860" />
      </g>

      <!-- esferas -->
      <g>
        <circle cx="470" cy="760" r="62" fill="url(#esfera)" />
        <circle cx="830" cy="740" r="44" fill="url(#esfera)" />
        <circle cx="660" cy="820" r="26" fill="url(#esfera)" />
        <ellipse cx="470" cy="826" rx="54" ry="7" fill="#5e4a8a" opacity=".12" />
      </g>
    </svg>
    <div class="vino" aria-hidden="true"></div>
  `,
  styles: `
    :host { position: fixed; inset: 0; z-index: 0; overflow: hidden; }
    .escena { width: 100%; height: 100%; display: block; }
    .cinta { animation: flotar 14s ease-in-out infinite alternate; transform-origin: center; }
    .c2 { animation-duration: 18s; } .c3 { animation-duration: 16s; animation-direction: alternate-reverse; }
    @keyframes flotar { from { transform: translateY(-6px); } to { transform: translateY(8px); } }
    .vino {
      position: absolute; right: -6%; bottom: -8%; width: 32vw; height: 60vh;
      background: radial-gradient(circle at 60% 60%, rgba(var(--d-94_74_138),.55), rgba(var(--d-94_74_138),0) 60%);
      filter: blur(30px);
    }
  `,
})
export class Fondo {}
