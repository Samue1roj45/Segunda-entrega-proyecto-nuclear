/*
 * Puente SOMA <-> Pixel Streaming (Ecos de una decisión).
 *
 * Se agrega a la página del reproductor de Pixel Streaming (la que sirve el servidor de señalización de Epic), DESPUÉS
 * de player.js:
 *
 *     <script defer src="player.js"></script>
 *     <script defer src="soma-puente.js" data-soma-origenes="https://front-lbjo.onrender.com,http://localhost:4200"></script>
 *
 * Qué hace:
 *  1. SOMA muestra este reproductor dentro de su página (iframe) y le entrega el código de sesión por postMessage.
 *  2. El puente se lo pasa al juego con emitUIInteraction({ tipo: 'ecos_sesion', sesionId, codigo, api }).
 *     Unreal lo canjea igual que con -EcosSesion/-EcosApi (contrato ecos-soma/1).
 *  3. Lo que el juego envía con «Send Pixel Streaming Response» (JSON) se reenvía a SOMA, que lo muestra en vivo.
 *
 * data-soma-origenes (opcional): orígenes de SOMA permitidos, separados por coma. Vacío = el primero que salude.
 * Requiere el reproductor estándar de Epic (expone window.pixelStreaming).
 */
(function () {
  'use strict';

  var FUENTE = 'ecos-ps';
  var VERSION = 1;
  var script = document.currentScript;
  var permitidos = ((script && script.getAttribute('data-soma-origenes')) || '')
    .split(',').map(function (o) { return o.trim(); }).filter(Boolean);

  var padre = null;          // origen de SOMA, fijado con el primer saludo válido
  var pendiente = null;      // sesión que SOMA pidió entregar al juego
  var entregada = false;
  var estado = 'conectando';
  var conectado = false;

  function ps() { return window.pixelStreaming; }

  function aSoma(mensaje) {
    if (!padre || window.parent === window) { return; }
    mensaje.fuente = FUENTE;
    window.parent.postMessage(mensaje, padre);
  }

  function cambiarEstado(nuevo) {
    estado = nuevo;
    aSoma({ tipo: 'estado', estado: nuevo });
  }

  /** emitUIInteraction devuelve false mientras el video no está listo: se reintenta hasta lograrlo. */
  function entregar() {
    var s = ps();
    if (!pendiente || entregada || !s || typeof s.emitUIInteraction !== 'function') { return; }
    var ok = s.emitUIInteraction({ tipo: 'ecos_sesion', sesionId: pendiente.sesionId, codigo: pendiente.codigo, api: pendiente.api });
    if (ok !== false && conectado) {
      entregada = true;
      aSoma({ tipo: 'entregada' });
    }
  }

  function esTexto(v) { return typeof v === 'string' && v.length > 0 && v.length <= 512; }

  window.addEventListener('message', function (e) {
    if (e.source !== window.parent) { return; }
    var d = e.data;
    if (!d || d.fuente !== 'soma') { return; }
    if (permitidos.length && permitidos.indexOf(e.origin) < 0) { return; }
    if (!padre) { padre = e.origin; }
    if (e.origin !== padre) { return; }

    if (d.tipo === 'hola') {
      aSoma({ tipo: 'listo', version: VERSION });
      aSoma({ tipo: 'estado', estado: estado });
    } else if (d.tipo === 'sesion' && esTexto(d.sesionId) && esTexto(d.codigo) && esTexto(d.api)) {
      pendiente = { sesionId: d.sesionId, codigo: d.codigo, api: d.api };
      entregada = false;
      entregar();
    }
  });

  function conectarAlReproductor(s) {
    s.addEventListener('webRtcConnected', function () { conectado = true; cambiarEstado('conectado'); entregar(); });
    s.addEventListener('videoInitialized', function () { conectado = true; cambiarEstado('video'); entregar(); });
    s.addEventListener('webRtcDisconnected', function () { conectado = false; entregada = false; cambiarEstado('desconectado'); });
    s.addResponseEventListener('soma_puente', function (respuesta) {
      var datos;
      try { datos = JSON.parse(respuesta); } catch (err) { datos = { texto: String(respuesta) }; }
      if (!datos || typeof datos !== 'object' || Array.isArray(datos)) { datos = { texto: String(respuesta) }; }
      aSoma({ tipo: 'juego', datos: datos });
    });
    // Si el video ya estaba corriendo cuando cargó el puente, no se esperan los eventos que ya pasaron.
    var video = document.querySelector('video');
    if (video && video.readyState >= 2) { conectado = true; cambiarEstado('video'); }
    // Si la sesión llegó antes que el canal de datos, se sigue intentando hasta que el juego la reciba.
    setInterval(entregar, 2000);
  }

  // player.js crea window.pixelStreaming al cargar; se espera hasta 60 s.
  var intentos = 0;
  var espera = setInterval(function () {
    var s = ps();
    if (s && typeof s.addResponseEventListener === 'function') {
      clearInterval(espera);
      conectarAlReproductor(s);
    } else if (++intentos > 300) {
      clearInterval(espera);
    }
  }, 200);
})();
