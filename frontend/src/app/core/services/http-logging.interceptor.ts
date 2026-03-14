import { HttpEvent, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs';

import { AppLoggerService } from './app-logger.service';

export const httpLoggingInterceptor: HttpInterceptorFn = (req, next) => {
  const logger = inject(AppLoggerService);
  const startedAt = Date.now();

  logger.debug('http', 'Request started', {
    method: req.method,
    url: req.urlWithParams,
  });

  return next(req).pipe(
    tap({
      next: (event: HttpEvent<unknown>) => {
        logger.debug('http', 'Request event', {
          method: req.method,
          url: req.urlWithParams,
          eventType: event.type,
          durationMs: Date.now() - startedAt,
        });
      },
      error: (error: unknown) => {
        logger.error('http', 'Request failed', {
          method: req.method,
          url: req.urlWithParams,
          durationMs: Date.now() - startedAt,
          error,
        });
      },
    }),
  );
};
