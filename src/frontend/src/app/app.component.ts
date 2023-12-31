import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import { environment } from './../environments/environment';
import {DeviceDetectorService} from "ngx-device-detector";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'frontend';
  loginUrl = '';

  constructor(private http: HttpClient) {
    const self = this;
    console.log(environment.production, environment.apiUrl);
    http.get(environment.apiUrl + '/auth/login/url', { responseType: 'text' }).subscribe(data => {
      console.log(data);
      self.loginUrl = data;
    });
  }

  ngAfterViewInit() {
    this.ping();
  }

  async ping() {
    while(true) {
      let token = localStorage.getItem("token");

      if(token != null && token.length > 1) {
        const httpHeaders: HttpHeaders = new HttpHeaders({
          'Authorization': 'Bearer ' + token
        });

        this.http.post(environment.apiUrl + '/auth/ping', '', {
          headers: httpHeaders
        }).subscribe({
          next: (data) => {
            if (data == null || data.toString().length < 16) {
              localStorage.removeItem("token");
            } else {
              localStorage.setItem("token", data.toString());
            }
            console.log('ping response', data);
          },
          error: (e) => {
            if (e.status == 403) {
              localStorage.removeItem("token");
            }
          }
        });
      }

      await this.sleep((30 * 1000));
    }
  }

  sleep(ms : number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
