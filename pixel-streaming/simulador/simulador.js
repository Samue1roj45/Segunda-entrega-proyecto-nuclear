/*
 * Juego simulado detrás de un window.pixelStreaming falso (misma forma que el reproductor de Epic):
 * addEventListener, addResponseEventListener y emitUIInteraction. Al recibir { tipo: 'ecos_sesion' } hace lo mismo que
 * Unreal: canje del código, un evento por acto y el resultado final, todo contra la API real de SOMA.
 */
(function () {
  'use strict';

  // Preguntas del catálogo Caso1 (backend/src/main/resources/ecos/catalogo-caso1.json). El resultado debe traerlas todas.
  var PREGUNTAS = ['N2', 'N1', 'E1_Q1', 'N3', 'N4', 'E1_Q2', 'N5', 'N6', 'E1_Q3', 'E2_Q1', 'N7', 'N8', 'E2_Q2', 'E2_Q3'];
  var ACTOS = [
    'Andrea llega a la oficina y nota la tensión del equipo.',
    'Primera reunión: alguien propone ignorar el problema.',
    'Decides a quién escuchar primero.',
    'El conflicto escala en el chat del grupo.',
    'Hablas con la líder del proyecto.',
    'El equipo vota cómo seguir.',
  ];
  var MS_POR_ACTO = 2500;

  var oyentes = {};
  var respuestas = {};
  var video = false;
  var acto = 0;
  var texto = 'Conectando…';

  function emitir(tipo) { (oyentes[tipo] || []).forEach(function (f) { f({ type: tipo }); }); }
  function responder(obj) { Object.keys(respuestas).forEach(function (k) { respuestas[k].forEach(function (f) { f(JSON.stringify(obj)); }); }); }
  function uuid() { return crypto.randomUUID ? crypto.randomUUID() : '10000000-1000-4000-8000-100000000000'.replace(/[018]/g, function (c) { return (c ^ (crypto.getRandomValues(new Uint8Array(1))[0] & (15 >> (c / 4)))).toString(16); }); }
  function esperar(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }

  async function llamar(url, token, cuerpo) {
    var h = { 'Content-Type': 'application/json' };
    if (token) { h.Authorization = 'Bearer ' + token; }
    var r = await fetch(url, { method: 'POST', headers: h, body: cuerpo ? JSON.stringify(cuerpo) : null });
    var datos = r.status === 202 ? {} : await r.json().catch(function () { return {}; });
    if (!r.ok) { throw new Error((datos && datos.mensaje) || ('HTTP ' + r.status)); }
    return datos;
  }

  async function jugar(s) {
    try {
      texto = 'Canjeando el código ' + s.codigo + '…';
      var canje = await llamar(s.api + '/sesiones/' + encodeURIComponent(s.codigo) + '/canje');
      var inicio = new Date();
      responder({ tipo: 'conectado', acto: 0, texto: 'Juego conectado a SOMA como ' + canje.estudiante.nombreVisible.replace(/\.$/, '') + '.' });
      for (acto = 1; acto <= 6; acto++) {
        var punto = PREGUNTAS[Math.min(PREGUNTAS.length - 1, (acto - 1) * 2)];
        texto = 'Acto ' + acto + ' de 6 · ' + ACTOS[acto - 1];
        await llamar(s.api + '/sesiones/' + canje.sesionId + '/eventos', canje.token, { envioId: uuid(), punto: punto, acto: acto, fecha: new Date().toISOString() });
        responder({ tipo: 'progreso', acto: acto, punto: punto, texto: 'Acto ' + acto + ': ' + ACTOS[acto - 1] });
        await esperar(MS_POR_ACTO);
      }
      texto = 'Enviando el resultado a SOMA…';
      var fin = new Date();
      var resultado = await llamar(s.api + '/sesiones/' + canje.sesionId + '/resultado', canje.token, {
        contrato: 'ecos-soma/1', envioId: uuid(), sesionId: canje.sesionId, intento: canje.intento, caso: canje.caso,
        juego: { build: 'simulador-navegador', motor: 'simulado' }, inicio: inicio.toISOString(), fin: fin.toISOString(),
        segundosParticipacion: Math.max(0, Math.floor((fin - inicio) / 1000) - 1), protagonista: 'Andrea',
        respuestas: PREGUNTAS.map(function (id, i) { return { preguntaId: id, opcionId: i % 3 === 2 ? 'B' : 'A' }; }),
        desenlace: 'FIN_SOSTENIDA', indicadores: {}, calculoJuego: null,
      });
      texto = 'Fin · nota calculada por SOMA: ' + Number(resultado.nota).toFixed(1);
      responder({ tipo: 'fin', acto: 6, texto: 'Partida terminada. SOMA calculó la nota: ' + Number(resultado.nota).toFixed(1) + ' (' + resultado.nivel + ').' });
    } catch (e) {
      texto = 'Error: ' + e.message;
      responder({ tipo: 'error', texto: 'El juego no pudo hablar con SOMA: ' + e.message });
    }
  }

  window.pixelStreaming = {
    addEventListener: function (tipo, f) { (oyentes[tipo] = oyentes[tipo] || []).push(f); },
    addResponseEventListener: function (nombre, f) { (respuestas[nombre] = respuestas[nombre] || []).push(f); },
    emitUIInteraction: function (d) {
      if (!video) { return false; }
      if (d && d.tipo === 'ecos_sesion') { jugar(d); }
      return true;
    },
  };

  // Igual que el reproductor real: primero WebRTC, luego el video.
  setTimeout(function () { emitir('webRtcConnected'); }, 800);
  setTimeout(function () { video = true; texto = 'Esperando el código de sesión de SOMA…'; emitir('videoInitialized'); }, 1600);

  // «Video»: un lienzo animado con el estado de la partida.
  var lienzo = document.getElementById('pantalla');
  var ctx = lienzo.getContext('2d');
  function dibujar(t) {
    var w = lienzo.width = lienzo.clientWidth * devicePixelRatio;
    var h = lienzo.height = lienzo.clientHeight * devicePixelRatio;
    var g = ctx.createLinearGradient(0, 0, w, h);
    g.addColorStop(0, '#2a1d4a'); g.addColorStop(1, '#5b3f8f');
    ctx.fillStyle = g; ctx.fillRect(0, 0, w, h);
    for (var i = 0; i < 24; i++) {
      var x = (Math.sin(t / 1300 + i) * .45 + .5) * w, y = (Math.cos(t / 1700 + i * 1.7) * .4 + .5) * h;
      ctx.fillStyle = 'rgba(220,205,255,' + (.08 + .06 * Math.sin(t / 500 + i)) + ')';
      ctx.beginPath(); ctx.arc(x, y, (6 + i % 5 * 4) * devicePixelRatio, 0, 7); ctx.fill();
    }
    ctx.fillStyle = '#fff'; ctx.textAlign = 'center';
    ctx.font = 600 * 1 + ' ' + Math.round(h / 14) + 'px system-ui'; ctx.fillText('Ecos de una decisión', w / 2, h * .38);
    ctx.font = Math.round(h / 30) + 'px system-ui'; ctx.fillStyle = 'rgba(255,255,255,.85)';
    ctx.fillText('SIMULADOR DE PIXEL STREAMING (sin Unreal)', w / 2, h * .46);
    ctx.font = Math.round(h / 24) + 'px system-ui'; ctx.fillStyle = '#e8dcff'; ctx.fillText(texto, w / 2, h * .6);
    if (acto > 0) {
      ctx.fillStyle = 'rgba(255,255,255,.2)'; ctx.fillRect(w * .2, h * .7, w * .6, h * .02);
      ctx.fillStyle = '#c3a9ff'; ctx.fillRect(w * .2, h * .7, w * .6 * Math.min(acto, 6) / 6, h * .02);
    }
    requestAnimationFrame(dibujar);
  }
  requestAnimationFrame(dibujar);
})();
