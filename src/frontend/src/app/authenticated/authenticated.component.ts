import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {Router, RouterOutlet} from "@angular/router";
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {environment} from "../../environments/environment";
import {AuthService} from "../auth/auth.service";

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './authenticated.component.html',
  styleUrl: './authenticated.component.css'
})
export class AuthenticatedComponent {
  constructor(private http: HttpClient, private router: Router, private authService: AuthService) {}

  ngAfterViewInit() {
    this.ping();
  }

  async ping() {
    console.log('start ping!');

    while(true) {
      let token = localStorage.getItem("token");
      if(this.authService.isAuthenticated() && token != null && token.length > 1) {
        const httpHeaders: HttpHeaders = new HttpHeaders({
          'Authorization': 'Bearer ' + token
        });

        this.http.post(environment.apiUrl + '/auth/ping', '', {
          headers: httpHeaders,
          responseType: 'text'
        }).subscribe({
          next: (data) => {
            if (data == null || data.toString().length < 16) {
              localStorage.removeItem("token");

              // redirect to login page
              this.router.navigate(['auth/login']);
            } else {
              localStorage.setItem("token", data.toString());
            }

            console.debug(Date.now(), 'ping response', data);
          },
          error: (e) => {
            console.log('ping error', e);
            if (e.status == 403) {
              localStorage.removeItem("token");

              // redirect to login page
              this.router.navigate(['auth/login']);
            }
          }
        });
      } else {
        // redirect to login page
        await this.router.navigate(['auth/login']);
        break;
      }

      await this.sleep((30 * 1000));
    }
  }

  sleep(ms : number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
