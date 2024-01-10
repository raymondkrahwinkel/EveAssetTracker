import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet} from "@angular/router";
import {AuthService} from "../auth/auth.service";
import {BackendService} from "../services/backend.service";
import {TokenInformation} from "../models/tokenInformation";
import {Character} from "../models/character";
import Swal from 'sweetalert2';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  templateUrl: './authenticated.component.html',
  styleUrl: './authenticated.component.css'
})
export class AuthenticatedComponent {
  protected authenticatedInformation: TokenInformation|null = null;
  protected character: Character|null = null;
  private sessionKeepAlive: boolean = true;

  constructor(private backend: BackendService, private router: Router, protected authService: AuthService) {}

  ngOnInit() {
    if(!this.authService.isAuthenticated()) {
      alert('logout now!');
      this.router.navigate(['auth/login']);
      return;
    }

    if(this.router.url.substring(1, 7) == 'logout') {
      this.sessionKeepAlive = false;

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

    // get the authenticated information
    this.authenticatedInformation = this.authService.getAuthenticatedInformation();
    if(this.authenticatedInformation) {
      this.backend.getCharacter(this.authenticatedInformation.id).then((character) => {
        this.character = character;
        console.log("character data", character)
      });
    }
  }

  ngAfterViewInit() {
    this.ping();
  }

  async ping() {
    while(this.sessionKeepAlive) {
      if(this.authService.isAuthenticated()) {
        this.backend.ping().then(
          (data) => {
            localStorage.setItem("token", data.toString());
          },
          (reason) => {
            if(!this.sessionKeepAlive) {
              return;
            }

            if(reason == 403) {
              Swal.fire({
                title: 'Authentication expired',
                showDenyButton: true,
                showCancelButton: false,
                confirmButtonText: 'Reauthenticate',
                denyButtonText: 'Logout',
              }).then((result) => {
                if (result.isConfirmed) {
                  if(this.authenticatedInformation == null) {
                    // try to get the data
                    this.authenticatedInformation = this.authService.getAuthenticatedInformation();

                    // when we have still no data, stop here
                    if(this.authenticatedInformation == null) {
                      Swal.fire({
                        icon: 'error',
                        title: 'Oops...',
                        text: 'No session information available'
                      });

                      return;
                    }
                  }

                  // get the unique login url
                  this.backend.getLoginUrl(this.authenticatedInformation.token, false, true).then((url) => {
                    window.location.href = url;
                  });
                } else if (result.isDenied) {
                  localStorage.removeItem("token");

                  // redirect to login page
                  this.router.navigate(['auth/login']);
                }
              })

              // localStorage.removeItem("token");
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

  onAddCharacterClick() : void {
    if(this.authenticatedInformation == null) {
      Swal.fire({
        icon: 'error',
        title: 'Oops...',
        text: 'No session information available'
      });
      return;
    }

    // get the character login url
    this.backend.getLoginUrl(this.authenticatedInformation.token, true, false).then((data) => {
      window.location.href = data;
    });
  }

  sleep(ms : number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
