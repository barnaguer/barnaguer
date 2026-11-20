const CACHE_NAME = "barnaguer-pwa-v4";
const APP_SHELL = [
  "./",
  "./index.html",
  "./manifest.json",
  "./barnaguer-logo.jpg",
  "./barnaguer-banner.png"
];

self.addEventListener("install", event => {
  event.waitUntil((async () => {
    const cache = await caches.open(CACHE_NAME);
    await cache.addAll(APP_SHELL);
    await self.skipWaiting();
  })());
});

self.addEventListener("activate", event => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys.filter(key => key.startsWith("barnaguer-pwa-") && key !== CACHE_NAME).map(key => caches.delete(key)));
    await self.clients.claim();
  })());
});

self.addEventListener("fetch", event => {
  const request = event.request;
  if (request.method !== "GET") return;

  const url = new URL(request.url);
  const isSupabaseLibrary =
    url.hostname === "cdn.jsdelivr.net" &&
    url.pathname.startsWith("/npm/@supabase/supabase-js@2");

  if (isSupabaseLibrary) {
    event.respondWith((async () => {
      const cached = await caches.match(request);
      if (cached) return cached;
      try {
        const response = await fetch(request);
        if (response && (response.ok || response.type === "opaque")) {
          const cache = await caches.open(CACHE_NAME);
          await cache.put(request, response.clone());
        }
        return response;
      } catch (error) {
        return Response.error();
      }
    })());
    return;
  }

  if (url.origin !== self.location.origin) return;

  if (request.mode === "navigate") {
    event.respondWith((async () => {
      try {
        const response = await fetch(request);
        if (response && response.ok) {
          const cache = await caches.open(CACHE_NAME);
          await cache.put("./index.html", response.clone());
        }
        return response;
      } catch (error) {
        return (await caches.match("./index.html")) ||
          new Response("BARNAGUER no está disponible sin conexión. Conéctate a internet e inténtalo de nuevo.", {
            status: 503,
            headers: { "Content-Type": "text/plain; charset=utf-8" }
          });
      }
    })());
    return;
  }

  if (["image", "style", "script", "font"].includes(request.destination) ||
      url.pathname.endsWith("/manifest.json")) {
    event.respondWith((async () => {
      const cached = await caches.match(request);
      if (cached) return cached;
      try {
        const response = await fetch(request);
        if (response && response.ok) {
          const cache = await caches.open(CACHE_NAME);
          await cache.put(request, response.clone());
        }
        return response;
      } catch (error) {
        return Response.error();
      }
    })());
  }
});
