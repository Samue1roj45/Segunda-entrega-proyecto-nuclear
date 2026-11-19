import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, ErrorHandler, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es-CO';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling, withViewTransitions } from '@angular/router';
import { routes } from './app.routes';
import { autenticacionInterceptor } from './core/acceso';
import { cacheInterceptor, registroHttpInterceptor, reintentoInterceptor } from './core/red';
import { ManejadorErrores } from './core/registro-errores.service';

registerLocaleData(localeEs);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withViewTransitions({ skipInitialTransition: true }),
      withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled' }),
    ),
    provideHttpClient(withInterceptors([autenticacionInterceptor, cacheInterceptor, reintentoInterceptor, registroHttpInterceptor])),
    { provide: LOCALE_ID, useValue: 'es-CO' },
    { provide: ErrorHandler, useClass: ManejadorErrores },
  ],
};
