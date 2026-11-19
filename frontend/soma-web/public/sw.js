/* SOMA · service worker mínimo.
   - Nunca toca /api (los datos siempre vienen del servidor).
   - Archivos estáticos: primero la red y, si no hay conexión, la última copia guardada.
   - Navegación sin conexión: devuelve la última página guardada (la app se abre aunque no haya red). */
const CACHE = 'soma-v1';

self.addEventListener('install', (e) => { self.skipWaiting(); e.waitUntil(caches.open(CACHE).then((c) => c.add('/index.html').catch(() => undefined))); });
self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k !== CACHE).map((k) => caches.delete(k)))).then(() => self.clients.claim()));
});

self.addEventListener('fetch', (e) => {
  const req = e.request;
  const url = new URL(req.url);
  if (req.method !== 'GET' || url.origin !== location.origin || url.pathname.startsWith('/api')) return;
  e.respondWith(
    fetch(req)
      .then((res) => {
        if (res.ok && (res.type === 'basic')) { const copia = res.clone(); caches.open(CACHE).then((c) => c.put(req.mode === 'navigate' ? '/index.html' : req, copia)); }
        return res;
      })
      .catch(() => caches.match(req.mode === 'navigate' ? '/index.html' : req).then((r) => r || caches.match('/index.html'))),
  );
});
