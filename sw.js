// Gestor de l'hort – service worker
// Bump CACHE_VERSION when you change this file or want to force a full cache refresh.
const CACHE_VERSION = 'v1';
const SHELL_CACHE = 'hort-shell-' + CACHE_VERSION;
const CDN_CACHE = 'hort-cdn-' + CACHE_VERSION;

const SHELL_FILES = [
  './',
  'index.html',
  'manifest.json',
  'favicon.png',
  'icon-512.png',
  'icon-512m.png',
  'icon-512-mono.png'
];

// Cross-origin hosts whose static files are safe to cache.
const CDN_HOSTS = [
  'cdn.tailwindcss.com',
  'fonts.googleapis.com',
  'fonts.gstatic.com',
  'www.gstatic.com' // Firebase SDK modules
];

// How long to wait for the network before falling back to the cached page
// (bad signal at the allotment shouldn't mean a long blank screen).
const NAV_TIMEOUT_MS = 4000;

self.addEventListener('install', (event) => {
  event.waitUntil((async () => {
    const cache = await caches.open(SHELL_CACHE);
    // allSettled: one missing file must not make the whole install fail.
    await Promise.allSettled(
      SHELL_FILES.map((f) => cache.add(new Request(f, { cache: 'reload' })))
    );
    await self.skipWaiting();
  })());
});

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const keep = [SHELL_CACHE, CDN_CACHE];
    const names = await caches.keys();
    await Promise.all(names.filter((n) => !keep.includes(n)).map((n) => caches.delete(n)));
    await self.clients.claim();
  })());
});

self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') self.skipWaiting();
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;

  const url = new URL(req.url);

  // Page loads: network-first (so edits show up), cache as fallback.
  if (req.mode === 'navigate') {
    event.respondWith(networkFirstPage(req));
    return;
  }

  // Own files (manifest, icons…): stale-while-revalidate.
  if (url.origin === self.location.origin) {
    event.respondWith(staleWhileRevalidate(req, SHELL_CACHE));
    return;
  }

  // Static CDN files: stale-while-revalidate.
  if (CDN_HOSTS.includes(url.hostname)) {
    event.respondWith(staleWhileRevalidate(req, CDN_CACHE));
    return;
  }

  // Everything else (Firestore, Firebase Auth, …): don't intercept.
});

async function networkFirstPage(req) {
  const cache = await caches.open(SHELL_CACHE);
  try {
    const res = await Promise.race([
      fetch(req),
      new Promise((_, reject) => setTimeout(() => reject(new Error('timeout')), NAV_TIMEOUT_MS))
    ]);
    if (res && res.ok) cache.put('index.html', res.clone());
    return res;
  } catch (err) {
    const cached = (await cache.match('index.html')) || (await cache.match('./'));
    if (cached) return cached;
    return fetch(req); // nothing cached yet: let the browser show its own error
  }
}

async function staleWhileRevalidate(req, cacheName) {
  const cache = await caches.open(cacheName);
  const cached = await cache.match(req);
  const network = fetch(req)
    .then((res) => {
      // Cache OK responses and opaque ones (no-cors <script>/<link> to CDNs).
      if (res && (res.ok || res.type === 'opaque')) cache.put(req, res.clone());
      return res;
    })
    .catch(() => null);
  if (cached) {
    network.catch(() => {}); // refresh in background
    return cached;
  }
  const res = await network;
  return res || Response.error();
}
