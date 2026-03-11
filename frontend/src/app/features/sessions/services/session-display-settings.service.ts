import { Injectable, signal } from '@angular/core';

export interface SessionDisplaySettings {
  currentPageSize: number;
  dayPageSize: number;
}

const STORAGE_KEY = 'cm_session_display_settings';
const DEFAULT_SETTINGS: SessionDisplaySettings = {
  currentPageSize: 5,
  dayPageSize: 8,
};

@Injectable({ providedIn: 'root' })
export class SessionDisplaySettingsService {
  readonly settings = signal<SessionDisplaySettings>(this.read());

  update(settings: SessionDisplaySettings): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
    this.settings.set(settings);
  }

  private read(): SessionDisplaySettings {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return DEFAULT_SETTINGS;
    }
    try {
      const parsed = JSON.parse(raw) as Partial<SessionDisplaySettings>;
      return {
        currentPageSize: this.normalize(parsed.currentPageSize, DEFAULT_SETTINGS.currentPageSize),
        dayPageSize: this.normalize(parsed.dayPageSize, DEFAULT_SETTINGS.dayPageSize),
      };
    } catch {
      return DEFAULT_SETTINGS;
    }
  }

  private normalize(value: number | undefined, fallback: number): number {
    if (!value || Number.isNaN(value) || value < 1) {
      return fallback;
    }
    return Math.floor(value);
  }
}
