import { HttpErrorResponse, HttpEvent, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, tap, throwError } from 'rxjs';

import { AppLoggerService } from './app-logger.service';

export const httpLoggingInterceptor: HttpInterceptorFn = (req, next) => {
  const logger = inject(AppLoggerService);
  const startedAt = performance.now();

  logger.debug('http', 'Outgoing request', {
    method: req.method,
    url: req.urlWithParams,
    responseType: req.responseType,
    body: req.body,
  });

  return next(req).pipe(
    tap((event: HttpEvent<unknown>) => {
      if (!(event instanceof HttpResponse)) {
        return;
      }

      logger.info('http', 'Incoming response', {
        method: req.method,
        url: req.urlWithParams,
        status: event.status,
        durationMs: Math.round(performance.now() - startedAt),
        bodySummary: summarizeBody(event.body),
      });
    }),
    catchError((error: HttpErrorResponse) => {
      logger.error('http', 'HTTP request failed', {
        method: req.method,
        url: req.urlWithParams,
        status: error.status,
        durationMs: Math.round(performance.now() - startedAt),
        message: error.message,
        error: error.error,
      });
      return throwError(() => error);
    }),
  );
};

function summarizeBody(body: unknown): unknown {
  if (Array.isArray(body)) {
    return { kind: 'array', size: body.length };
  }

  if (body && typeof body === 'object') {
    return {
      kind: 'object',
      keys: Object.keys(body).slice(0, 10),
    };
  }

  return body;
}
