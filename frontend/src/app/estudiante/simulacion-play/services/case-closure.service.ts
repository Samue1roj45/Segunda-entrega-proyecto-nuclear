import { Injectable } from '@angular/core';

const STORAGE_PREFIX = 'simulacion-closure:';

@Injectable({ providedIn: 'root' })
export class CaseClosureService {
  save(participacionId: number, snapshot: unknown): void {
    sessionStorage.setItem(`${STORAGE_PREFIX}${participacionId}`, JSON.stringify(snapshot));
  }

  load(participacionId: number): unknown | null {
    const raw = sessionStorage.getItem(`${STORAGE_PREFIX}${participacionId}`);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }

  clear(participacionId: number): void {
    sessionStorage.removeItem(`${STORAGE_PREFIX}${participacionId}`);
  }
}
