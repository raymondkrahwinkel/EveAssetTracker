import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from "@angular/common/http";
import {environment} from "../../environments/environment";
import {ResponsePing} from "../models/api/response.ping";
import {ResponseBaseWithData} from "../models/api/response.base.data";
import {Character} from "../models/character";
import {AuthService} from "../auth/auth.service";
import {ResponseCharacterWallet} from "../models/api/response.character.wallet";
import {ConfigService} from "./config.service";

@Injectable({
  providedIn: 'root'
})
export class BackendService {
  constructor(private configService: ConfigService, private http: HttpClient, private authService: AuthService) { }

  public async getLoginUrl(session: string|null, addCharacter: boolean = false, reAuthentication: boolean = false): Promise<any> {
    let params = new HttpParams()
        .set('state', crypto.randomUUID())
        .set('s', session ?? '')
        .set('ra', reAuthentication)
        .set('ac', addCharacter);

    if(addCharacter) {
      let characterId = this.authService.getAuthenticatedInformation()?.id
      console.log('character id', characterId);
      if(characterId != null) {
        params = params.append('pc', characterId);
      }
    }

    console.log(params);

    return new Promise<any>(resolve => {
      this.http.get(this.configService.config().apiUrl + '/auth/login/url?' + params.toString(), { responseType: 'text' })
        .subscribe(data => resolve(data));
    });
  }

  public async ping(): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      this.http.post<ResponsePing>(this.configService.config().apiUrl + '/auth/ping', '', {
        headers: this.getAuthHeaders(),
      }).subscribe({
        next: (data) => {
          console.debug((new Date).toLocaleString(), 'ping response', data);
          if (data == null || !data.success) {
            console.error(data?.message ?? "");
            reject(403); // send 403 back because received token is invalid
          } else {
            resolve(data.data);
          }
        },
        error: (e) => {
          console.log('ping error', e);
          if (e.status == 403) {
            reject(403);
            // localStorage.removeItem("token");
            //
            // // redirect to login page
            // this.router.navigate(['auth/login']);
          } else {
            reject(e);
          }
        }
      });
    });
  }

  public async logout(): Promise<boolean> {
    return new Promise<boolean>(resolve => {
      this.http.post(this.configService.config().apiUrl + '/auth/logout', '', {
        headers: this.getAuthHeaders(),
        responseType: 'text'
      }).subscribe({
        next: value => {
          resolve((/true/).test(value));
        }
      });
    });
  }

  public async getCharacter(id: number): Promise<Character> {
    return new Promise<Character>((resolve, reject) => {
      this.http.get<ResponseBaseWithData<Character>>(this.configService.config().apiUrl + '/character/' + id, {
        headers: this.getAuthHeaders(),
      }).subscribe({
        next: (data) => {
          console.debug((new Date).toLocaleString(), 'character.get response', data);
          if (data == null || !data.success) {
            console.error(data?.message ?? "");
            reject(403); // send 403 back because received token is invalid
          } else {
            resolve(data.data);
          }
        },
        error: (e) => {
          if (e.status == 403) {
            reject(403);
          } else {
            reject(e);
          }
        }
      });
    });
  }

  public async getWallet(id: number): Promise<ResponseCharacterWallet> {
    return new Promise<ResponseCharacterWallet>((resolve, reject) => {
      this.http.get<ResponseCharacterWallet>(this.configService.config().apiUrl + '/wallet/balance/' + id, {
        headers: this.getAuthHeaders(),
      }).subscribe({
        next: (data) => {
          console.debug((new Date).toLocaleString(), 'wallet.balance response', data);
          if (data == null || !data.success) {
            console.error(data?.message ?? "");
            reject(403); // send 403 back because received token is invalid
          } else {
            resolve(data);
          }
        },
        error: (e) => {
          if (e.status == 403) {
            reject(403);
          } else {
            reject(e);
          }
        }
      });
    });
  }

  private getAuthHeaders(): HttpHeaders {
    let token = localStorage.getItem("token");
    return new HttpHeaders({'Authorization': 'Bearer ' + token });
  }
}
