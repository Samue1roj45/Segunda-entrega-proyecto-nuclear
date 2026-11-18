// @ts-nocheck — módulo gráfico portado del prototipo Three.js (sin tipado estricto a propósito).
import * as THREE from 'three';
/**
 * Cinta de cristal: barre una sección rectangular (ancho x grosor) a lo largo de una curva,
 * con giro (twist) variable. Devuelve una BufferGeometry cerrada con normales suaves.
 */
export function cintaGeometria(puntos, { ancho = (t) => 1, grosor = 0.08, giro = (t) => 0, segmentos = 220 } = {}) {
  const curva = new THREE.CatmullRomCurve3(puntos, false, 'catmullrom', 0.5);
  const frames = curva.computeFrenetFrames(segmentos, false);
  const pos = [], idx = [];
  const esquinas = [[-1, 1], [1, 1], [1, -1], [-1, -1]]; // rectángulo (a, g)
  for (let i = 0; i <= segmentos; i++) {
    const t = i / segmentos, p = curva.getPointAt(t);
    const N = frames.normals[i], B = frames.binormals[i], th = giro(t);
    const n = N.clone().multiplyScalar(Math.cos(th)).addScaledVector(B, Math.sin(th));
    const b = B.clone().multiplyScalar(Math.cos(th)).addScaledVector(N, -Math.sin(th));
    const w = ancho(t) / 2, g = grosor / 2;
    for (const [a, c] of esquinas) pos.push(p.x + n.x * w * a + b.x * g * c, p.y + n.y * w * a + b.y * g * c, p.z + n.z * w * a + b.z * g * c);
  }
  for (let i = 0; i < segmentos; i++) for (let k = 0; k < 4; k++) {
    const a = i * 4 + k, b = i * 4 + ((k + 1) % 4), c = (i + 1) * 4 + k, d = (i + 1) * 4 + ((k + 1) % 4);
    idx.push(a, c, b, b, c, d);
  }
  // tapas
  idx.push(0, 1, 2, 0, 2, 3); const f = segmentos * 4; idx.push(f, f + 2, f + 1, f, f + 3, f + 2);
  const geo = new THREE.BufferGeometry();
  geo.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
  geo.setIndex(idx); geo.computeVertexNormals();
  return geo;
}
