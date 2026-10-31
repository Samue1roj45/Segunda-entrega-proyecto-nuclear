import { writeFileSync, mkdirSync } from 'fs';
import { dirname, join } from 'path';
import { fileURLToPath } from 'url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', 'public', 'assets', 'simulacion');

function svgWrap(title, body, palette = 'hospital') {
  const sky = palette === 'hospital' ? '#0c1929' : '#1c1917';
  const floor = palette === 'hospital' ? '#94a3b8' : '#a8a29e';
  const accent = palette === 'hospital' ? '#0ea5e9' : '#ca8a04';
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1280 720">
  <defs>
    <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="${sky}"/>
      <stop offset="100%" stop-color="#020617"/>
    </linearGradient>
    <linearGradient id="floor" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="${floor}" stop-opacity="0.5"/>
      <stop offset="100%" stop-color="#475569"/>
    </linearGradient>
    <radialGradient id="glow" cx="50%" cy="15%" r="55%">
      <stop offset="0%" stop-color="${accent}" stop-opacity="0.2"/>
      <stop offset="100%" stop-color="#000" stop-opacity="0"/>
    </radialGradient>
  </defs>
  <rect width="1280" height="720" fill="url(#sky)"/>
  <rect width="1280" height="720" fill="url(#glow)"/>
  ${body}
  <rect x="0" y="520" width="1280" height="200" fill="url(#floor)"/>
  <text x="640" y="48" text-anchor="middle" fill="rgba(248,250,252,0.15)" font-family="Georgia,serif" font-size="18" letter-spacing="8">${title.toUpperCase()}</text>
</svg>`;
}

const rooms = {
  hospital: {
    entrada: `
  <rect x="140" y="100" width="1000" height="420" fill="#1e3a5f"/>
  <rect x="180" y="140" width="920" height="360" fill="#0f172a" opacity="0.4"/>
  <rect x="500" y="180" width="280" height="70" fill="#dc2626"/>
  <text x="640" y="228" text-anchor="middle" fill="#fff" font-family="Arial" font-size="32" font-weight="bold">URGENCIAS</text>
  <rect x="520" y="300" width="240" height="180" fill="#334155" stroke="#64748b" stroke-width="3"/>
  <rect x="540" y="320" width="200" height="140" fill="#38bdf8" opacity="0.12"/>
  <rect x="560" y="400" width="160" height="8" fill="#94a3b8" opacity="0.5"/>
  <rect x="60" y="500" width="180" height="90" fill="#dc2626"/>
  <text x="150" y="545" text-anchor="middle" fill="#fff" font-size="11" font-family="Arial">AMBULANCIA</text>
  <circle cx="120" cy="580" r="22" fill="#1e293b"/><circle cx="180" cy="580" r="22" fill="#1e293b"/>
  <rect x="1000" y="480" width="40" height="50" fill="#fbbf24"/>
  <rect x="80" y="520" width="24" height="32" fill="#f97316"/>
  <rect x="110" y="530" width="24" height="32" fill="#f97316"/>
  <ellipse cx="1020" cy="200" rx="60" ry="30" fill="#fef08a" opacity="0.35"/>`,
    recepcion: `
  <rect x="80" y="160" width="560" height="300" fill="#475569"/>
  <rect x="100" y="200" width="520" height="24" fill="#64748b"/>
  <rect x="120" y="240" width="200" height="140" fill="#1e293b"/>
  <rect x="340" y="250" width="140" height="100" fill="#0ea5e9" opacity="0.2"/>
  <rect x="500" y="280" width="80" height="60" rx="4" fill="#334155"/>
  <rect x="700" y="180" width="480" height="320" fill="#e2e8f0" opacity="0.08"/>
  <rect x="180" y="420" width="48" height="56" rx="4" fill="#64748b"/>
  <rect x="260" y="420" width="48" height="56" rx="4" fill="#64748b"/>
  <rect x="340" y="420" width="48" height="56" rx="4" fill="#64748b"/>
  <rect x="900" y="120" width="280" height="80" fill="#0369a1"/>
  <text x="1040" y="168" text-anchor="middle" fill="#fff" font-size="16" font-family="Arial">RECEPCION</text>`,
    'sala-espera': `
  <rect x="60" y="100" width="1160" height="100" fill="#334155"/>
  <rect x="100" y="400" width="70" height="90" rx="6" fill="#475569"/>
  <rect x="200" y="400" width="70" height="90" rx="6" fill="#475569"/>
  <rect x="300" y="400" width="70" height="90" rx="6" fill="#475569"/>
  <rect x="400" y="400" width="70" height="90" rx="6" fill="#475569"/>
  <rect x="500" y="400" width="70" height="90" rx="6" fill="#475569"/>
  <rect x="900" y="220" width="300" height="200" fill="#0f172a" stroke="#64748b"/>
  <rect x="920" y="240" width="260" height="150" fill="#38bdf8" opacity="0.15"/>
  <rect x="120" y="280" width="100" height="180" fill="#78716c" opacity="0.4"/>
  <rect x="680" y="300" width="80" height="140" fill="#57534e" opacity="0.35"/>
  <rect x="240" y="200" width="200" height="120" fill="#1e40af" opacity="0.25"/>
  <text x="260" y="270" fill="#e2e8f0" font-size="10" font-family="Arial">Derechos del paciente</text>`,
    pasillo: `
  <rect x="0" y="180" width="200" height="400" fill="#334155"/>
  <rect x="1080" y="180" width="200" height="400" fill="#334155"/>
  <rect x="200" y="220" width="880" height="80" fill="#64748b"/>
  <rect x="320" y="380" width="200" height="60" fill="#94a3b8"/>
  <rect x="340" y="360" width="160" height="20" fill="#cbd5e1"/>
  <rect x="700" y="360" width="120" height="100" fill="#475569"/>
  <rect x="720" y="340" width="80" height="40" fill="#dc2626" opacity="0.8"/>
  <text x="760" y="368" text-anchor="middle" fill="#fff" font-size="12" font-family="Arial">RESTRICCION</text>
  <rect x="480" y="300" width="60" height="100" fill="#0f172a" stroke="#22d3ee"/>
  <circle cx="510" cy="330" r="8" fill="#22d3ee" opacity="0.6"/>`,
    'area-medica': `
  <rect x="100" y="140" width="400" height="280" fill="#0f766e" opacity="0.25"/>
  <rect x="520" y="120" width="660" height="320" fill="#1e293b" opacity="0.5"/>
  <rect x="140" y="380" width="280" height="100" fill="#94a3b8"/>
  <rect x="160" y="360" width="240" height="24" fill="#cbd5e1"/>
  <rect x="600" y="200" width="180" height="120" fill="#0f172a" stroke="#475569"/>
  <rect x="620" y="220" width="140" height="80" fill="#22d3ee" opacity="0.2"/>
  <rect x="820" y="380" width="100" height="80" fill="#64748b"/>
  <rect x="400" y="400" width="120" height="70" fill="#475569"/>
  <line x1="620" y1="260" x2="760" y2="260" stroke="#22d3ee" stroke-width="2" opacity="0.5"/>`,
    consultorio: `
  <rect x="200" y="280" width="500" height="180" fill="#57534e"/>
  <rect x="220" y="300" width="200" height="120" fill="#44403c"/>
  <rect x="240" y="320" width="80" height="60" fill="#0ea5e9" opacity="0.2"/>
  <rect x="720" y="200" width="400" height="280" fill="#78716c"/>
  <rect x="740" y="220" width="60" height="200" fill="#a8a29e"/>
  <rect x="810" y="220" width="60" height="200" fill="#a8a29e"/>
  <rect x="880" y="220" width="60" height="200" fill="#a8a29e"/>
  <rect x="400" y="180" width="200" height="100" fill="#1e293b"/>
  <rect x="420" y="200" width="160" height="60" fill="#fff" opacity="0.9"/>
  <rect x="100" y="400" width="80" height="80" rx="40" fill="#64748b" opacity="0.5"/>
  <rect x="1100" y="400" width="80" height="80" rx="40" fill="#64748b" opacity="0.5"/>`,
  },
  comisaria: {
    recepcion: `
  <rect x="80" y="160" width="520" height="300" fill="#334155"/>
  <rect x="100" y="200" width="480" height="28" fill="#475569"/>
  <circle cx="200" cy="120" r="50" fill="#ca8a04"/>
  <text x="200" y="128" text-anchor="middle" fill="#1e3a8a" font-size="22" font-weight="bold" font-family="Arial">CF</text>
  <rect x="700" y="200" width="480" height="260" fill="#1e40af" opacity="0.2"/>
  <rect x="180" y="420" width="56" height="64" rx="4" fill="#57534e"/>
  <rect x="260" y="420" width="56" height="64" rx="4" fill="#57534e"/>`,
    'sala-espera': `
  <rect x="80" y="120" width="1120" height="90" fill="#44403c"/>
  <rect x="100" y="400" width="68" height="88" rx="5" fill="#57534e"/>
  <rect x="200" y="400" width="68" height="88" rx="5" fill="#57534e"/>
  <rect x="300" y="400" width="68" height="88" rx="5" fill="#57534e"/>
  <rect x="400" y="400" width="68" height="88" rx="5" fill="#57534e"/>
  <rect x="880" y="240" width="280" height="160" fill="#292524"/>
  <rect x="120" y="260" width="220" height="100" fill="#ca8a04" opacity="0.2"/>
  <text x="130" y="310" fill="#fef3c7" font-size="11" font-family="Arial">Atencion a victimas</text>`,
    'oficina-atencion': `
  <rect x="180" y="200" width="920" height="280" fill="#57534e"/>
  <rect x="220" y="240" width="360" height="200" fill="#44403c"/>
  <rect x="620" y="260" width="420" height="24" fill="#78716c"/>
  <rect x="620" y="300" width="420" height="24" fill="#78716c"/>
  <rect x="620" y="340" width="420" height="24" fill="#78716c"/>
  <rect x="260" y="280" width="100" height="70" fill="#1e40af" opacity="0.3"/>`,
    'area-psicosocial': `
  <rect x="80" y="140" width="480" height="300" fill="#115e59" opacity="0.35"/>
  <rect x="600" y="140" width="560" height="300" fill="#0f766e" opacity="0.3"/>
  <rect x="120" y="220" width="180" height="100" fill="#fff" opacity="0.06"/>
  <rect x="680" y="220" width="180" height="100" fill="#fff" opacity="0.06"/>
  <rect x="400" y="180" width="480" height="50" fill="#14b8a6" opacity="0.25"/>`,
    despacho: `
  <rect x="320" y="280" width="640" height="200" fill="#44403c"/>
  <rect x="360" y="300" width="220" height="140" fill="#292524"/>
  <rect x="600" y="300" width="320" height="140" fill="#0f172a"/>
  <rect x="100" y="200" width="160" height="280" fill="#57534e"/>
  <rect x="120" y="220" width="120" height="20" fill="#a8a29e"/>
  <rect x="120" y="250" width="120" height="20" fill="#a8a29e"/>
  <rect x="1000" y="200" width="180" height="280" fill="#57534e"/>`,
  },
};

for (const [inst, defs] of Object.entries(rooms)) {
  const dir = join(root, inst, 'rooms');
  mkdirSync(dir, { recursive: true });
  for (const [id, body] of Object.entries(defs)) {
    const path = join(dir, `${id}.svg`);
    writeFileSync(path, svgWrap(id.replace(/-/g, ' '), body, inst === 'hospital' ? 'hospital' : 'comisaria'));
    console.log('wrote', path);
  }
}
