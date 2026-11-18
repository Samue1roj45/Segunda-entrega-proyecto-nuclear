// @ts-nocheck — módulo gráfico portado del prototipo Three.js (sin tipado estricto a propósito).
// Ruido de valor 2D con fBm "ridged" para las montañas (sin dependencias).
function hash(x, y) { const s = Math.sin(x * 127.1 + y * 311.7) * 43758.5453; return s - Math.floor(s); }
function suave(t) { return t * t * (3 - 2 * t); }
export function ruido(x, y) {
  const xi = Math.floor(x), yi = Math.floor(y), xf = x - xi, yf = y - yi;
  const a = hash(xi, yi), b = hash(xi + 1, yi), c = hash(xi, yi + 1), d = hash(xi + 1, yi + 1);
  const u = suave(xf), v = suave(yf);
  return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
}
export function ridged(x, y, octavas = 5) {
  let amp = 1, frec = 1, suma = 0, norma = 0;
  for (let i = 0; i < octavas; i++) {
    const n = 1 - Math.abs(ruido(x * frec, y * frec) * 2 - 1);
    suma += n * n * amp; norma += amp; amp *= 0.5; frec *= 2.07;
  }
  return suma / norma;
}
