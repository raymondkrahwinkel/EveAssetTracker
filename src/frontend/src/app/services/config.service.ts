import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {AppConfig} from "../models/appconfig";
import {Observable, tap} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class ConfigService {
  private configuration: AppConfig = new AppConfig();

  constructor(private httpClient: HttpClient) { }

  downloadConfig(): Observable<any> {
    return this.httpClient
            .get<AppConfig>('./app-config.json')
            .pipe(tap(config => {
              this.configuration = config;
            }));
  }

  config(): AppConfig {
    return this.configuration;
  }
}
