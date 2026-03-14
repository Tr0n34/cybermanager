import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AppLoggerService } from './app-logger.service';
import { SessionService } from './session.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(SessionService);
  const router = inject(Router);
  const logger = inject(AppLoggerService);
  const token = session.token();
  const request = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (token && error.status === 401) {
        logger.warn('auth-http', 'JWT rejected by API, clearing local session and redirecting to login', {
          url: req.url,
          method: req.method,
        });
        session.clear();
        void router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
