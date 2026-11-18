// @ts-nocheck — igual que el resto de escena3d.
import * as THREE from 'three';
import { EffectComposer } from 'three/examples/jsm/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/examples/jsm/postprocessing/RenderPass.js';
import { UnrealBloomPass } from 'three/examples/jsm/postprocessing/UnrealBloomPass.js';
import { OutputPass } from 'three/examples/jsm/postprocessing/OutputPass.js';
import { ShaderPass } from 'three/examples/jsm/postprocessing/ShaderPass.js';
import { crearEscena } from './escena';

/** La cámara se corre a la derecha para que el arco quede a la izquierda y el panel de acceso sobre la zona despejada. */
const DESPLAZAMIENTO_X = 3.6;

export interface MotorEscena { detener(): void; }

/** Monta la escena 3D de SOMA en un <canvas>. Carga perezosa: solo se descarga al abrir el login. */
export async function iniciarEscena(canvas: HTMLCanvasElement, opciones: { animar: boolean }): Promise<MotorEscena> {
  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, powerPreference: 'high-performance' });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.5));
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.05;
  const tam = () => ({ w: canvas.clientWidth || innerWidth, h: canvas.clientHeight || innerHeight });
  renderer.setSize(tam().w, tam().h, false);

  const { escena, camara } = await crearEscena(renderer, { urlHdri: 'sky.hdr' });
  const gradacion = new ShaderPass({
    uniforms: { tDiffuse: { value: null } },
    vertexShader: 'varying vec2 vUv; void main(){ vUv=uv; gl_Position=projectionMatrix*modelViewMatrix*vec4(position,1.); }',
    fragmentShader: `uniform sampler2D tDiffuse; varying vec2 vUv;
      void main(){ vec3 c=texture2D(tDiffuse,vUv).rgb; float l=dot(c,vec3(.299,.587,.114));
        c=mix(vec3(l),c,.38); c=mix(c,vec3(.86,.81,.96)*(.5+.7*l),.2); c*=vec3(.98,.95,1.02); c=pow(c,vec3(.92)); c=mix(c,smoothstep(.0,1.,c),.35);
        float v=smoothstep(1.15,.35,length(vUv-.5)*1.4); c*=mix(.9,1.,v); gl_FragColor=vec4(c,1.); }`,
  });
  const comp = new EffectComposer(renderer);
  comp.addPass(new RenderPass(escena, camara));
  comp.addPass(new UnrealBloomPass(new THREE.Vector2(tam().w, tam().h), 0.22, 0.7, 0.88));
  comp.addPass(new OutputPass());
  comp.addPass(gradacion);

  const ajustar = () => { const { w, h } = tam(); renderer.setSize(w, h, false); comp.setSize(w, h); camara.aspect = w / h; camara.updateProjectionMatrix(); };
  ajustar();
  window.addEventListener('resize', ajustar);

  const agua = escena.children.find((o) => o.isWater);
  const t0 = performance.now();
  let activo = true, raf = 0;
  const cuadro = () => {
    if (!activo) return;
    const t = (performance.now() - t0) / 1000;
    if (opciones.animar) {
      if (agua) agua.material.uniforms.time.value = t * 0.25 / 1;
      camara.position.x = DESPLAZAMIENTO_X + Math.sin(t * 0.12) * 0.25;
    }
    camara.lookAt(DESPLAZAMIENTO_X, 4.8, 0);
    comp.render();
    if (opciones.animar && !document.hidden) raf = requestAnimationFrame(cuadro);
    else if (opciones.animar) raf = requestAnimationFrame(cuadro);
  };
  cuadro();
  document.addEventListener('visibilitychange', () => { if (!document.hidden && opciones.animar) { cancelAnimationFrame(raf); cuadro(); } });
  canvas.dataset['listo'] = '1';

  return {
    detener() {
      activo = false; cancelAnimationFrame(raf); window.removeEventListener('resize', ajustar);
      escena.traverse((o) => { o.geometry?.dispose?.(); const m = o.material; (Array.isArray(m) ? m : m ? [m] : []).forEach((x) => x.dispose?.()); });
      comp.dispose?.(); renderer.dispose();
    },
  };
}
