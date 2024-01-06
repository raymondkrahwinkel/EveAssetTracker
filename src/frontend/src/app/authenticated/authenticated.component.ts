import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet} from "@angular/router";
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {environment} from "../../environments/environment";
import {AuthService} from "../auth/auth.service";
import {BackendService} from "../services/backend.service";

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  templateUrl: './authenticated.component.html',
  styleUrl: './authenticated.component.css'
})
export class AuthenticatedComponent {
  constructor(private backend: BackendService, private router: Router, private authService: AuthService) {}

  ngAfterViewInit() {
    if(this.router.url.substring(1, 7) == 'logout') {
      let token = localStorage.getItem("token");
      if(this.authService.isAuthenticated() && token != null && token.length > 1) {
        this.backend.logout()
          .then((completed) => {
            localStorage.removeItem("token");
            this.router.navigate(['auth/login'])
          });
      } else {
        localStorage.removeItem("token");
        this.router.navigate(['auth/login'])
      }
      return;
    }

    this.ping();
  }

  async ping() {
    while(true) {
      let token = localStorage.getItem("token");
      if(this.authService.isAuthenticated() && token != null && token.length > 1) {
        this.backend.ping().then(
          (data) => {
            localStorage.setItem("token", data.toString());
          },
          (reason) => {
            if(reason == 403) {
              localStorage.removeItem("token");

              // redirect to login page
              this.router.navigate(['auth/login']);
            } else {
              console.error(reason);
            }
          }
        );
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
