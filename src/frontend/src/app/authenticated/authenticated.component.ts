import {Component, EventEmitter, Output} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet} from "@angular/router";
import {AuthService} from "../auth/auth.service";
import {BackendService} from "../services/backend.service";
import {TokenInformation} from "../models/tokenInformation";
import {Character} from "../models/character";
import Swal from 'sweetalert2';
import {FormattingService} from "../services/formatting.service";

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
  protected children: Character[] = [];
  protected walletValue: number = 0;
  protected walletValueDifference: number = 0;
  private sessionKeepAlive: boolean = true;

  // events
  @Output() characterChanged: EventEmitter<Character> = new EventEmitter();
  @Output() walletChanged: EventEmitter<number> = new EventEmitter();

  constructor(private backend: BackendService, private router: Router, protected authService: AuthService, protected formattingService: FormattingService) {}

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
          .then(() => {
            localStorage.removeItem("token");
            this.router.navigate(['auth/login'])
          });
      } else {
        localStorage.removeItem("token");
        this.router.navigate(['auth/login'])
      }
      return;
    }

    this.updateCharacterData();
  }

  ngAfterViewInit() {
    this.ping();
  }

  async updateCharacterData() {
    // get the authenticated information
    this.authenticatedInformation = this.authService.getAuthenticatedInformation();
    if(this.authenticatedInformation && this.character?.id != this.authenticatedInformation.id) {
      this.backend.getCharacter().then((character) => {
        this.character = character;
        this.characterChanged.emit(character);
        this.updateWallet();
        this.updateChildren();
      });
    } else if(this.authenticatedInformation && this.character) {
      this.updateWallet();
    }
  }

  async updateWallet() {
    if(this.character != null) {
      this.backend.getWallet(this.character?.id).then((data) => {
        if(this.walletValue != data.data) {
          this.walletChanged.emit(data.data);
          this.walletValue = data.data;
          this.walletValueDifference = data.difference ?? 0;
        }
      });
    }
  }

  async updateChildren() {
    if(this.character != null) {
      this.backend.getCharacterChildren().then((data) => {
        this.children = data;
      })
    }
  }

  async ping() {
    while(this.sessionKeepAlive) {
      if(this.authService.isAuthenticated()) {
        this.backend.ping().then(
          (response) => {
            localStorage.setItem("token", response.data.toString());
            this.updateCharacterData();
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

  onSwitchCharacterClick(characterId: number) : void {
    this.backend.switch(characterId).then(
      (response) => {
        if(response.success) {
          localStorage.setItem("token", response.data.toString());
          this.updateCharacterData();
        } else {
          Swal.fire({
            icon: 'error',
            title: 'Oops...',
            text: response.message
          });
          return;
        }
      },
      (reason) => {
        if(!this.sessionKeepAlive) {
          return;
        }

        Swal.fire({
          icon: 'error',
          title: 'Failed to switch character',
          text: 'Reason: ' + reason
        });
        return;
      }
    );
  }

  sleep(ms : number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
