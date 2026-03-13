import { ApplicationConfig, ErrorHandler, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './core/services/auth.interceptor';
import { FrontendErrorHandler } from './core/services/frontend-error-handler';
import { httpLoggingInterceptor } from './core/services/http-logging.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([httpLoggingInterceptor, authInterceptor])),
    { provide: ErrorHandler, useClass: FrontendErrorHandler },
  ],
};
