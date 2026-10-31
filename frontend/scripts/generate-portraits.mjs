import { writeFileSync, mkdirSync } from 'fs';
import { dirname, join } from 'path';
import { fileURLToPath } from 'url';

const outDir = join(dirname(fileURLToPath(import.meta.url)), '..', 'public', 'assets', 'simulacion', 'portraits');
mkdirSync(outDir, { recursive: true });

/** Busto institucional detallado (solo presentación visual). */
function portrait({ id, skin, hair, shirt, accent, mood = 'neutral' }) {
  const eyeY = mood === 'distress' ? 53 : 51;
  const browY = eyeY - 6;
  const mouth =
    mood === 'distress'
      ? 'M43 70 Q50 65 57 70'
      : mood === 'anger'
        ? 'M44 69 L56 69'
        : mood === 'reserved'
          ? 'M46 69 Q50 67 54 69'
          : 'M45 69 Q50 72 55 69';
  const cheek = mood === 'distress' ? 0.12 : 0.08;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 130" role="img" aria-label="Retrato">
  <defs>
    <linearGradient id="bg-${id}" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="#334155"/>
      <stop offset="55%" stop-color="#1e293b"/>
      <stop offset="100%" stop-color="#0f172a"/>
    </linearGradient>
    <linearGradient id="shirt-${id}" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="${shirt[0]}"/>
      <stop offset="100%" stop-color="${shirt[1]}"/>
    </linearGradient>
    <radialGradient id="face-light-${id}" cx="50%" cy="35%" r="55%">
      <stop offset="0%" stop-color="${skin}" stop-opacity="1"/>
      <stop offset="100%" stop-color="${skin}" stop-opacity="0.88"/>
    </radialGradient>
  </defs>
  <rect width="100" height="130" fill="url(#bg-${id})"/>
  <ellipse cx="50" cy="118" rx="42" ry="20" fill="url(#shirt-${id})"/>
  <path d="M24 98 Q50 90 76 98 L76 130 L24 130 Z" fill="url(#shirt-${id})"/>
  <path d="M38 88 Q50 82 62 88 L62 98 L38 98 Z" fill="${accent}" opacity="0.9"/>
  <ellipse cx="34" cy="58" rx="5" ry="7" fill="${skin}" opacity="0.95"/>
  <ellipse cx="66" cy="58" rx="5" ry="7" fill="${skin}" opacity="0.95"/>
  <ellipse cx="50" cy="54" rx="24" ry="28" fill="url(#face-light-${id})"/>
  <ellipse cx="38" cy="62" rx="6" ry="4" fill="#c97b63" opacity="${cheek}"/>
  <ellipse cx="62" cy="62" rx="6" ry="4" fill="#c97b63" opacity="${cheek}"/>
  <path d="M26 50 Q50 20 74 50 Q70 36 50 30 Q30 36 26 50 Z" fill="${hair}"/>
  <path d="M30 44 Q50 38 70 44" stroke="${hair}" stroke-width="2" fill="none" opacity="0.5"/>
  <ellipse cx="39" cy="${eyeY}" rx="3.2" ry="2.8" fill="#1e293b"/>
  <ellipse cx="61" cy="${eyeY}" rx="3.2" ry="2.8" fill="#1e293b"/>
  <ellipse cx="39.5" cy="${eyeY - 0.5}" rx="1" ry="0.6" fill="#fff" opacity="0.35"/>
  <ellipse cx="61.5" cy="${eyeY - 0.5}" rx="1" ry="0.6" fill="#fff" opacity="0.35"/>
  <path d="M36 ${browY} Q39 ${browY - 3} 42 ${browY}" stroke="#3f2e22" stroke-width="1.3" fill="none"/>
  <path d="M58 ${browY} Q61 ${browY - 3} 64 ${browY}" stroke="#3f2e22" stroke-width="1.3" fill="none"/>
  <path d="M48 58 L52 64" stroke="${skin}" stroke-width="2" opacity="0.35" stroke-linecap="round"/>
  <path d="${mouth}" stroke="#8b5a4a" stroke-width="1.6" fill="none" stroke-linecap="round"/>
  <rect x="42" y="78" width="16" height="12" rx="3" fill="${skin}" opacity="0.92"/>
</svg>`;
}

const chars = [
  { id: 'guardia', skin: '#d4a574', hair: '#2c1810', shirt: ['#1e3a8a', '#172554'], accent: '#fbbf24', mood: 'neutral' },
  { id: 'secretaria', skin: '#c9a07a', hair: '#3d2914', shirt: ['#475569', '#334155'], accent: '#94a3b8', mood: 'neutral' },
  { id: 'enfermeria', skin: '#e8b896', hair: '#4a3728', shirt: ['#5eead4', '#0f766e'], accent: '#fff', mood: 'neutral' },
  { id: 'medico', skin: '#dbb896', hair: '#5c4033', shirt: ['#f8fafc', '#e2e8f0'], accent: '#0ea5e9', mood: 'neutral' },
  { id: 'madre', skin: '#d9a882', hair: '#2d1f14', shirt: ['#6d28d9', '#5b21b6'], accent: '#c4b5fd', mood: 'distress' },
  { id: 'hermano', skin: '#c4956a', hair: '#1a1208', shirt: ['#b45309', '#92400e'], accent: '#78716c', mood: 'anger' },
  { id: 'victima', skin: '#e5c4a8', hair: '#3b2a1a', shirt: ['#64748b', '#475569'], accent: '#cbd5e1', mood: 'distress' },
  { id: 'comisario', skin: '#c9a882', hair: '#252018', shirt: ['#1e40af', '#1e3a8a'], accent: '#ca8a04', mood: 'neutral' },
  { id: 'trabajador-social', skin: '#d4ae84', hair: '#4a3020', shirt: ['#0f766e', '#115e59'], accent: '#99f6e4', mood: 'neutral' },
  { id: 'proteccion', skin: '#bea078', hair: '#2a2018', shirt: ['#57534e', '#44403c'], accent: '#fbbf24', mood: 'reserved' },
];

for (const c of chars) {
  const path = join(outDir, `${c.id}.svg`);
  writeFileSync(path, portrait(c));
  console.log('wrote', path);
}
