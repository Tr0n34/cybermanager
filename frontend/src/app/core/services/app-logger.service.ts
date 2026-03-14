import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AppLoggerService {
  debug(scope: string, message: string, context?: unknown): void {
    console.debug(`[${scope}] ${message}`, context ?? '');
  }

  info(scope: string, message: string, context?: unknown): void {
    console.info(`[${scope}] ${message}`, context ?? '');
  }

  warn(scope: string, message: string, context?: unknown): void {
    console.warn(`[${scope}] ${message}`, context ?? '');
  }

  error(scope: string, message: string, context?: unknown): void {
    console.error(`[${scope}] ${message}`, context ?? '');
  }
}
