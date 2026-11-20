/** Lluvia breve de confeti sobre toda la pantalla. No hace nada con «reducir animaciones». Devuelve la duración en ms. */
export function confeti(): number {
  const d = document.documentElement;
  if (d.hasAttribute('data-quieto') || matchMedia('(prefers-reduced-motion: reduce)').matches) return 0;
  const lienzo = document.createElement('canvas');
  lienzo.setAttribute('aria-hidden', 'true');
  Object.assign(lienzo.style, { position: 'fixed', inset: '0', width: '100%', height: '100%', pointerEvents: 'none', zIndex: '500' });
  const ancho = (lienzo.width = innerWidth), alto = (lienzo.height = innerHeight);
  document.body.appendChild(lienzo);
  const g = lienzo.getContext('2d');
  if (!g) { lienzo.remove(); return 0; }
  const css = getComputedStyle(d);
  const colores = ['--lila-300', '--lila-500', '--lila-700', '--lila-200'].map((v) => css.getPropertyValue(v).trim() || '#9b83e6').concat('#f0cd86', '#8fe0bb', '#ffffff');
  const piezas = Array.from({ length: 140 }, () => ({
    x: ancho / 2 + (Math.random() - .5) * 120, y: alto * .35,
    vx: (Math.random() - .5) * 15, vy: -Math.random() * 14 - 4,
    r: Math.random() * Math.PI, vr: (Math.random() - .5) * .4,
    w: 6 + Math.random() * 6, h: 3 + Math.random() * 4, c: colores[Math.floor(Math.random() * colores.length)],
  }));
  const inicio = performance.now(), total = 2200;
  const paso = (t: number): void => {
    const k = (t - inicio) / total;
    g.clearRect(0, 0, ancho, alto);
    g.globalAlpha = Math.max(0, 1 - Math.max(0, k - .6) / .4);
    for (const p of piezas) {
      p.vy += .32; p.vx *= .99; p.x += p.vx; p.y += p.vy; p.r += p.vr;
      g.save(); g.translate(p.x, p.y); g.rotate(p.r); g.fillStyle = p.c; g.fillRect(-p.w / 2, -p.h / 2, p.w, p.h); g.restore();
    }
    if (k < 1) requestAnimationFrame(paso); else lienzo.remove();
  };
  requestAnimationFrame(paso);
  return 1100;
}
