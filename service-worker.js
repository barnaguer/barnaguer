const CACHE_NAME = "barnaguer-pwa-v7";
const SUPABASE_LIBRARY_URL = "https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2";
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

    // Descarga y guarda explícitamente la biblioteca que necesita el inicio.
    // Si el CDN falla, la instalación del resto de la app no debe cancelarse.
    try {
      await cache.add(SUPABASE_LIBRARY_URL);
    } catch (error) {
      console.warn("No se pudo precargar Supabase:", error);
    }

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
        if (response && response.ok) {
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
    // Abre primero la copia local para evitar esperas cuando la conexión es débil.
    const updatePromise = fetch(request).then(async response => {
      if (response && response.ok) {
        const cache = await caches.open(CACHE_NAME);
        await cache.put("./index.html", response.clone());
      }
      return response;
    }).catch(() => null);

    // La actualización de la copia se realiza en segundo plano.
    event.waitUntil(updatePromise.then(() => undefined));

    event.respondWith((async () => {
      const cached = await caches.match("./index.html");
      if (cached) return cached;

      const response = await updatePromise;
      return response || new Response("BARNAGUER no está disponible sin conexión. Conéctate a internet e inténtalo de nuevo.", {
        status: 503,
        headers: { "Content-Type": "text/plain; charset=utf-8" }
      });
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
