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

  update(settings: SessionDisplaySettings): SessionDisplaySettings {
    const normalized = {
      currentPageSize: this.normalize(settings.currentPageSize, DEFAULT_SETTINGS.currentPageSize),
      dayPageSize: this.normalize(settings.dayPageSize, DEFAULT_SETTINGS.dayPageSize),
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized));
    this.settings.set(normalized);
    return normalized;
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

  private normalize(value: number | string | undefined, fallback: number): number {
    const numericValue = typeof value === 'string' ? Number(value) : value;
    if (!numericValue || Number.isNaN(numericValue) || numericValue < 1) {
      return fallback;
    }
    return Math.floor(numericValue);
  }
}
