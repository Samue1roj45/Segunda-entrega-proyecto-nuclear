import { SceneId } from '../data/scene-types';

const BASE = '/assets/simulacion';

export const ASSET_URLS = {
  player: `${BASE}/player.svg`,
  objectDoc: `${BASE}/object-doc.svg`,
  hospitalBg: `${BASE}/hospital-bg.svg`,
  comisariaBg: `${BASE}/comisaria-bg.svg`,
} as const;

const HOSPITAL_ZONES = [
  'recepcion',
  'sala-espera',
  'pasillo',
  'enfermeria',
  'uci',
  'archivo',
] as const;

const COMISARIA_ZONES = [
  'recepcion',
  'secretaria',
  'trabajo-social',
  'proteccion',
  'archivo',
] as const;

export class AssetLoader {
  private readonly cache = new Map<string, HTMLImageElement>();
  private ready = false;
  async loadAll(): Promise<void> {
    const urls = [
      ...Object.values(ASSET_URLS),
      ...HOSPITAL_ZONES.map((z) => this.zoneUrl('hospital', z)),
      ...COMISARIA_ZONES.map((z) => this.zoneUrl('comisaria', z)),
    ];

    const unique = [...new Set(urls)];
    await Promise.all(
      unique.map((url) =>
        this.loadImage(url).catch((err) => {
          console.warn('[AssetLoader]', err);
          return null;
        }),
      ),
    );
    this.ready = true;
  }

  isReady(): boolean {
    return this.ready;
  }

  get(url: string): HTMLImageElement | null {
    return this.cache.get(url) ?? null;
  }

  zoneUrl(sceneId: SceneId, tileId: string): string {
    return `${BASE}/${sceneId}/zones/${tileId}.svg`;
  }

  zoneFor(sceneId: SceneId, tileId: string): HTMLImageElement | null {
    return this.get(this.zoneUrl(sceneId, tileId));
  }

  bgFor(sceneId: SceneId): HTMLImageElement | null {
    return sceneId === 'comisaria'
      ? this.get(ASSET_URLS.comisariaBg)
      : this.get(ASSET_URLS.hospitalBg);
  }

  player(): HTMLImageElement | null {
    return this.get(ASSET_URLS.player);
  }

  objectDoc(): HTMLImageElement | null {
    return this.get(ASSET_URLS.objectDoc);
  }

  private loadImage(url: string): Promise<HTMLImageElement> {
    const cached = this.cache.get(url);
    if (cached) return Promise.resolve(cached);

    return new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => {
        this.cache.set(url, img);
        resolve(img);
      };
      img.onerror = () => reject(new Error(`No se pudo cargar: ${url}`));
      img.src = url;
    });
  }
}
