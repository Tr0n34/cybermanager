import { ErrorHandler, Injectable, inject } from '@angular/core';

import { AppLoggerService } from './app-logger.service';

@Injectable()
export class FrontendErrorHandler implements ErrorHandler {
  private readonly logger = inject(AppLoggerService);

  handleError(error: unknown): void {
    this.logger.error('frontend', 'Unhandled application error', error);
  }
}
