import {APP_INITIALIZER, ApplicationConfig} from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withFetch} from "@angular/common/http";
import {ConfigService} from "./services/config.service";
import {Observable} from "rxjs";

export function initializeApp(configService: ConfigService) {
  return (): Observable<any> => configService.downloadConfig();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withFetch()),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeApp,
      multi: true,
      deps: [ConfigService]
    }
  ],
};
