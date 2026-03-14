import { Injectable } from '@angular/core';

type LogLevel = 'debug' | 'info' | 'warn' | 'error' | 'off';

type RuntimeConfig = {
  logLevel?: LogLevel;
};

const LEVEL_PRIORITY: Record<Exclude<LogLevel, 'off'>, number> = {
  debug: 10,
  info: 20,
  warn: 30,
  error: 40,
};

@Injectable({ providedIn: 'root' })
export class AppLoggerService {
  private readonly configuredLevel = this.readConfiguredLevel();

  debug(scope: string, message: string, context?: unknown): void {
    this.write('debug', scope, message, context);
  }

  info(scope: string, message: string, context?: unknown): void {
    this.write('info', scope, message, context);
  }

  warn(scope: string, message: string, context?: unknown): void {
    this.write('warn', scope, message, context);
  }

  error(scope: string, message: string, context?: unknown): void {
    this.write('error', scope, message, context);
  }

  private write(level: Exclude<LogLevel, 'off'>, scope: string, message: string, context?: unknown): void {
    if (!this.shouldLog(level)) {
      return;
    }

    const prefix = `[CM][${level.toUpperCase()}][${scope}] ${message}`;
    const sanitizedContext = context === undefined ? undefined : this.sanitizeValue(context);

    if (sanitizedContext === undefined) {
      console[this.consoleMethod(level)](prefix);
      return;
    }

    console[this.consoleMethod(level)](prefix, sanitizedContext);
  }

  private shouldLog(level: Exclude<LogLevel, 'off'>): boolean {
    if (this.configuredLevel === 'off') {
      return false;
    }

    return LEVEL_PRIORITY[level] >= LEVEL_PRIORITY[this.configuredLevel];
  }

  private consoleMethod(level: Exclude<LogLevel, 'off'>): 'debug' | 'info' | 'warn' | 'error' {
    return level;
  }

  private readConfiguredLevel(): LogLevel {
    if (typeof window === 'undefined') {
      return 'debug';
    }

    const runtimeLevel = (window as Window & { __CM_CONFIG__?: RuntimeConfig }).__CM_CONFIG__?.logLevel;
    if (this.isLogLevel(runtimeLevel)) {
      return runtimeLevel;
    }

    return 'debug';
  }

  private isLogLevel(value: string | null | undefined): value is LogLevel {
    return value === 'debug'
      || value === 'info'
      || value === 'warn'
      || value === 'error'
      || value === 'off';
  }

  private sanitizeValue(value: unknown, seen = new WeakSet<object>()): unknown {
    if (value == null || typeof value === 'number' || typeof value === 'boolean') {
      return value;
    }

    if (typeof value === 'string') {
      return this.maskString(value);
    }

    if (Array.isArray(value)) {
      return value.map((entry) => this.sanitizeValue(entry, seen));
    }

    if (value instanceof Error) {
      return {
        name: value.name,
        message: value.message,
        stack: value.stack,
      };
    }

    if (typeof value === 'object') {
      if (seen.has(value)) {
        return '[Circular]';
      }
      seen.add(value);

      return Object.fromEntries(
        Object.entries(value).map(([key, nestedValue]) => [
          key,
          this.isSensitiveKey(key) ? '[REDACTED]' : this.sanitizeValue(nestedValue, seen),
        ]),
      );
    }

    return String(value);
  }

  private isSensitiveKey(key: string): boolean {
    const normalized = key.toLowerCase();
    return normalized.includes('password')
      || normalized.includes('token')
      || normalized.includes('authorization')
      || normalized.includes('cookie')
      || normalized.includes('secret');
  }

  private maskString(value: string): string {
    return value
      .replace(/(Bearer\s+)[A-Za-z0-9\-._~+/]+=*/gi, '$1[REDACTED]')
      .replace(/(\"(?:password|token|authorization|cookie|secret)\"\s*:\s*\")([^\"]+)(\")/gi, '$1[REDACTED]$3');
  }
}
