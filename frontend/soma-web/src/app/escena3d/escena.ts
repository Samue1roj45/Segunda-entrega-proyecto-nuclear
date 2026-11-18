// @ts-nocheck — módulo gráfico portado del prototipo Three.js (sin tipado estricto a propósito).
import * as THREE from 'three';
import { Water } from 'three/examples/jsm/objects/Water.js';
import { HDRLoader } from 'three/examples/jsm/loaders/HDRLoader.js';
import { ridged } from './ruido';
import { cintaGeometria } from './cinta';

const LAVANDA = new THREE.Color('#cfc3e8');

function materialCristal(extra = {}) {
  return new THREE.MeshPhysicalMaterial({
    color: '#ffffff', metalness: 0, roughness: 0.06, transmission: 1, thickness: 0.9, ior: 1.48,
    attenuationColor: new THREE.Color('#a48bd6'), attenuationDistance: 2.2,
    clearcoat: 1, clearcoatRoughness: 0.05, envMapIntensity: 1.6, specularIntensity: 1,
    iridescence: 0.25, iridescenceIOR: 1.3, side: THREE.DoubleSide, ...extra,
  });
}

function normalesAgua() {
  const n = 256, h = new Float32Array(n * n);
  const val = (x, y) => { const s = Math.sin(x * 12.9898 + y * 78.233) * 43758.5453; return s - Math.floor(s); };
  for (let y = 0; y < n; y++) for (let x = 0; x < n; x++) {
    let v = 0, a = 1;
    for (let o = 0; o < 4; o++) { const f = 4 << o; const xf = (x / n) * f, yf = (y / n) * f; const xi = Math.floor(xf), yi = Math.floor(yf);
      const fx = xf - xi, fy = yf - yi, u = fx * fx * (3 - 2 * fx), w = fy * fy * (3 - 2 * fy);
      const m = (i, j) => val((i % f), (j % f));
      v += a * (m(xi, yi) * (1 - u) * (1 - w) + m(xi + 1, yi) * u * (1 - w) + m(xi, yi + 1) * (1 - u) * w + m(xi + 1, yi + 1) * u * w); a *= 0.5; }
    h[y * n + x] = v;
  }
  const data = new Uint8Array(n * n * 4);
  for (let y = 0; y < n; y++) for (let x = 0; x < n; x++) {
    const g = (i, j) => h[((j + n) % n) * n + ((i + n) % n)];
    const dx = (g(x + 1, y) - g(x - 1, y)) * 3.0, dy = (g(x, y + 1) - g(x, y - 1)) * 3.0;
    const l = Math.hypot(dx, dy, 1), k = (y * n + x) * 4;
    data[k] = (-dx / l * 0.5 + 0.5) * 255; data[k + 1] = (-dy / l * 0.5 + 0.5) * 255; data[k + 2] = (1 / l * 0.5 + 0.5) * 255; data[k + 3] = 255;
  }
  const tex = new THREE.DataTexture(data, n, n, THREE.RGBAFormat); tex.wrapS = tex.wrapT = THREE.RepeatWrapping; tex.needsUpdate = true; return tex;
}

function montanas() {
  const grupo = new THREE.Group();
  const capas = [
    { z: -34, ancho: 140, alto: 12, escala: 0.05, color: '#7d6cab', semilla: 3.1, y: -0.4 },
    { z: -52, ancho: 190, alto: 20, escala: 0.036, color: '#8e7fbb', semilla: 8.7, y: -0.5 },
    { z: -75, ancho: 260, alto: 26, escala: 0.028, color: '#a496c9', semilla: 1.9, y: -0.5 },
  ];
  for (const c of capas) {
    const geo = new THREE.PlaneGeometry(c.ancho, 34, 520, 70); geo.rotateX(-Math.PI / 2);
    const p = geo.attributes.position, col = new Float32Array(p.count * 3), base = new THREE.Color(c.color), cima = new THREE.Color('#c3b8e4');
    for (let i = 0; i < p.count; i++) {
      const x = p.getX(i), zl = p.getZ(i);
      const dist = (zl + 17) / 34; // 0 frente .. 1 fondo
      const borde = Math.max(0, 1 - Math.abs(x) / (c.ancho * 0.5)) ** 0.6;
      let h = ridged(x * c.escala + c.semilla, zl * c.escala * 1.4 + c.semilla, 6) ** 1.8 * c.alto * 1.1 * borde;
      h *= Math.max(0, 1.15 - dist * 0.8);
      // pináculo rocoso tipo "karst" a la derecha de la composición
      if (c.z === -52) { const d = Math.hypot((x - 22) / 4.2, (zl) / 9); h += Math.exp(-(d ** 3)) * 46; const d2 = Math.hypot((x + 47) / 6, zl / 10); h += Math.exp(-(d2 ** 3)) * 26; }
      h += Math.sin(h * 2.6 + ridged(x * 0.2, zl * 0.2, 2) * 6) * 0.12 * Math.min(h, 4); p.setY(i, h + c.y);
      const t = THREE.MathUtils.clamp(h / (c.alto * 0.9 + 8), 0, 1);
      const cc = base.clone().lerp(cima, t * 0.8); col.set([cc.r, cc.g, cc.b], i * 3);
    }
    geo.computeVertexNormals(); const nr = geo.attributes.normal, luz = new THREE.Vector3(-0.5, 0.45, 0.75).normalize();
    for (let i = 0; i < p.count; i++) { const l = 0.42 + 0.7 * Math.max(0, nr.getX(i) * luz.x + nr.getY(i) * luz.y + nr.getZ(i) * luz.z); col[i * 3] *= l; col[i * 3 + 1] *= l * 0.97; col[i * 3 + 2] *= l * 1.04; }
    geo.setAttribute('color', new THREE.BufferAttribute(col, 3));
    const m = new THREE.Mesh(geo, new THREE.MeshBasicMaterial({ vertexColors: true }));
    m.position.z = c.z; grupo.add(m);
  }
  return grupo;
}

function perfilEstriado(ancho, fondo, ondas) {
  const sh = new THREE.Shape(), n = ondas * 8, a = ancho / 2;
  sh.moveTo(-a, 0);
  for (let k = 0; k <= n; k++) { const x = -a + (ancho * k) / n; sh.lineTo(x, fondo * 0.5 + Math.sin((k / n) * ondas * Math.PI * 2) * fondo * 0.18); }
  sh.lineTo(a, 0); sh.closePath(); return sh;
}
function columnas(x0, dir, material, n = 22) {
  const g = new THREE.Group(); const rnd = (i) => { const s = Math.sin(i * 91.7 + x0) * 1e4; return s - Math.floor(s); };
  for (let i = 0; i < n; i++) {
    const ancho = 0.35 + rnd(i) * 0.75, alto = 20 + rnd(i + 20) * 6, ondas = 2 + Math.floor(rnd(i + 5) * 4);
    const geo = new THREE.ExtrudeGeometry(perfilEstriado(ancho, 0.5 + rnd(i + 40) * 0.5, ondas), { depth: alto, bevelEnabled: false, steps: 1 });
    geo.rotateX(-Math.PI / 2); // extruye hacia arriba (+Y)
    const m = new THREE.Mesh(geo, material);
    m.position.set(x0 + dir * (i * 0.34 + rnd(i + 3) * 0.14), -0.5, -3.4 - rnd(i + 9) * 1.4 - (i % 3) * 0.55); g.add(m);
  }
  return g;
}

function cintas(material) {
  const g = new THREE.Group(); const P = (a) => a.map((v) => new THREE.Vector3(...v));
  const defs = [
    { pts: [[-13.5, -0.3, -2], [-12.5, 5, -3.5], [-8.5, 10.5, -5.5], [-1, 13, -6.5], [7, 11.5, -5.5], [12.5, 7.5, -4], [15, 2, -3]],
      ancho: (t) => 1.6 + Math.sin(t * Math.PI) * 2.2, giro: (t) => Math.sin(t * 5.2) * 1.0 + t * 1.6, grosor: 0.32 },
    { pts: [[-17, 9, 0], [-11, 3.4, 2.5], [-4.5, 0.8, 3.2], [3, 1.4, 2.4], [9.5, 4.6, 0], [14.5, 10, -3], [17, 15, -5]],
      ancho: (t) => 1.5 + Math.sin(t * Math.PI) * 2.2, giro: (t) => Math.sin(t * 3.6 + 0.5) * 1.25 - 0.4, grosor: 0.36 },
    { pts: [[-17, 2.4, 1], [-10, 0.6, 3], [-1.5, 1.0, 3.5], [7, 2.8, 0], [15, 6, -5]],
      ancho: (t) => 1.2 + Math.sin(t * Math.PI) * 2.0, giro: (t) => Math.sin(t * 4.4) * 0.9 + 0.6, grosor: 0.28 },
    { pts: [[-15, 14, -3], [-12.2, 8.5, -1], [-10.6, 3.5, 1], [-9.2, 0, 3]],
      ancho: (t) => 1.9 - t * 0.6, giro: (t) => t * 3.6, grosor: 0.26 },
    { pts: [[4, 14, -8], [9, 9.5, -5], [12.5, 5, -2], [14.5, 0.2, 1]],
      ancho: (t) => 1.4 + Math.sin(t * Math.PI) * 1.0, giro: (t) => t * 2.4 + Math.sin(t * 6) * 0.4, grosor: 0.26 },
  ];
  for (const d of defs) g.add(new THREE.Mesh(cintaGeometria(P(d.pts), { ancho: d.ancho, grosor: d.grosor, giro: d.giro, segmentos: 260 }), material));
  return g;
}

function esferas(material) {
  const g = new THREE.Group();
  for (const [x, r, z] of [[-6.4, 1.45, 7], [0.2, 0.9, 6.5], [-3.2, 0.42, 8.2], [7.5, 0.7, 3]]) {
    const m = new THREE.Mesh(new THREE.SphereGeometry(r, 96, 64), material.clone()); m.material.thickness = r * 1.1; m.material.ior = 1.45; m.material.envMapIntensity = 1.1; m.material.iridescence = 0.5;
    m.material.attenuationColor = new THREE.Color('#c4b0ef'); m.material.attenuationDistance = r * 9;
    m.position.set(x, r - 0.02, z); g.add(m);
  }
  return g;
}

export async function crearEscena(renderer, { urlHdri = 'sky.hdr' } = {}) {
  const escena = new THREE.Scene();
  escena.fog = new THREE.FogExp2(LAVANDA, 0.0072);
  const camara = new THREE.PerspectiveCamera(34, renderer.domElement.clientWidth / Math.max(1, renderer.domElement.clientHeight), 0.1, 700);
  camara.position.set(0, 2.5, 26); camara.lookAt(0, 4.8, 0);

  const hdr = await new HDRLoader().loadAsync(urlHdri); hdr.mapping = THREE.EquirectangularReflectionMapping;
  const pmrem = new THREE.PMREMGenerator(renderer); const env = pmrem.fromEquirectangular(hdr).texture;
  escena.environment = env; escena.environmentIntensity = 1.0;
  escena.background = hdr; escena.backgroundIntensity = 1.35; escena.backgroundBlurriness = 0.02;
  escena.backgroundRotation = new THREE.Euler(0, -0.6, 0);

  // Luces: sol lila-blanco y relleno violeta
  const sol = new THREE.DirectionalLight('#fff3ff', 1.6); sol.position.set(-8, 14, 10); escena.add(sol);
  const relleno = new THREE.DirectionalLight('#9b86e6', 1.1); relleno.position.set(10, 5, 6); escena.add(relleno);
  escena.add(new THREE.HemisphereLight('#f4eeff', '#9d8ccc', 0.35));

  const agua = new Water(new THREE.PlaneGeometry(400, 260), {
    textureWidth: 1024, textureHeight: 1024, waterNormals: normalesAgua(), sunDirection: sol.position.clone().normalize(),
    sunColor: 0xffffff, waterColor: 0xb7a6dc, distortionScale: 0.8, fog: true, alpha: 0.96,
  });
  agua.rotation.x = -Math.PI / 2; agua.position.set(0, 0, -50); agua.material.uniforms.size.value = 14; escena.add(agua);

  escena.add(montanas());
  const cristal = materialCristal({ thickness: 1.0, attenuationDistance: 7, attenuationColor: new THREE.Color('#a98ee8'), iridescence: 0.55 });
  escena.add(columnas(-15.5, 1, cristal), columnas(15.5, -1, cristal), columnas(8.2, 1, cristal, 7), cintas(cristal), esferas(cristal));

  return { escena, camara, agua };
}
