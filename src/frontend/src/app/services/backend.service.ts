import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from "@angular/common/http";
import {environment} from "../../environments/environment";
import {ResponsePing} from "../models/api/response.ping";
import {ResponseBaseWithData} from "../models/api/response.base.data";
import {Character} from "../models/character";
import {AuthService} from "../auth/auth.service";
import {ResponseCharacterWallet} from "../models/api/response.character.wallet";
import {ConfigService} from "./config.service";
import {ResponseWalletHistory} from "../models/api/response.character.wallethistory";
import {ResponseSwitch} from "../models/api/response.switch";
import {RequestSwitch} from "../models/api/request.switch";

@Injectable({
  providedIn: 'root'
})
export class BackendService {
  constructor(private configService: ConfigService, private http: HttpClient, private authService: AuthService) { }

  public async isAlive(): Promise<boolean> {
    return new Promise<boolean>((resolve, reject) => {
      this.http.get(this.configService.config().apiUrl + '/health/live', { responseType: 'text' })
        .subscribe({
          next: (data) => resolve(data.toLowerCase() == 'ok'),
          error: err => reject(err)
        });
    });
  }

  public async getLoginUrl(session: string|null, addCharacter: boolean = false, reAuthentication: boolean = false): Promise<any> {
    let params = new HttpParams()
        .set('state', crypto.randomUUID())
        .set('s', session ?? '')
        .set('ra', reAuthentication)
        .set('ac', addCharacter);

    if(addCharacter) {
      let characterId = this.authService.getAuthenticatedInformation()?.id
      if(characterId != null) {
        params = params.append('pc', characterId);
      }
    }

    return new Promise<any>(resolve => {
      this.http.get(this.configService.config().apiUrl + '/auth/login/url?' + params.toString(), { responseType: 'text' })
        .subscribe(data => resolve(data));
    });
  }

  public async ping(): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      this.createPostRequest<ResponsePing>('/auth/ping', null).subscribe({
        next: (data) => {
          console.debug((new Date).toLocaleString(), 'ping response', data);
          if (data == null || !data.success) {
            console.error(data?.message ?? "");
            reject(403); // send 403 back because received token is invalid
          } else {
            resolve(data);
          }
        },
        error: (e) => {
          console.error('ping error', e);
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

  public async switch(characterId: number): Promise<ResponseSwitch> {
    return new Promise<ResponseSwitch>((resolve, reject) => {
      this.createPostRequest<ResponseSwitch>('/auth/switch', JSON.stringify(new RequestSwitch(characterId))).subscribe({
        next: (data) => {
          console.debug((new Date).toLocaleString(), 'switch response', data);
          if (data == null || !data.success) {
            console.error(data?.message ?? "");
            reject(403); // send 403 back because received token is invalid
          } else {
            resolve(data);
          }
        },
        error: (e) => {
          console.error('switch error', e);
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

  public async getCharacter(): Promise<Character> {
    return new Promise<Character>((resolve, reject) => {
      this.createGetRequest<ResponseBaseWithData<Character>>('/character').subscribe({
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

  public async getCharacterChildren(): Promise<Character[]> {
    return new Promise<Character[]>((resolve, reject) => {
      this.createGetRequest<ResponseBaseWithData<Character[]>>('/character/children').subscribe({
        next: (data) => {
          console.debug((new Date).toLocaleString(), 'character.children response', data);
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
      this.createGetRequest<ResponseCharacterWallet>('/wallet/balance/' + id).subscribe({
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

  public async getWalletHistory(): Promise<ResponseWalletHistory> {
    return new Promise<ResponseWalletHistory>((resolve, reject) => {
      this.createGetRequest<ResponseWalletHistory>('/wallet/history').subscribe({
        next: (data) => {
          console.debug((new Date).toLocaleString(), 'wallet.history response', data);
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

  createGetRequest<T>(url: string) {
    return this.http.get<T>(this.configService.config().apiUrl + (url[0] != '/' ? '/' : '') + url, {
      headers: this.getAuthHeaders(),
    });
  }

  createPostRequest<T>(url: string, body: string|null) {
    return this.http.post<T>(this.configService.config().apiUrl + (url[0] != '/' ? '/' : '') + url, body ?? '', {
      headers: this.getAuthHeaders({ 'Content-Type': 'application/json' }),
    });
  }

  createPutRequest<T>(url: string, body: string|null) {
    return this.http.put<T>(this.configService.config().apiUrl + (url[0] != '/' ? '/' : '') + url, body ?? '', {
      headers: this.getAuthHeaders({ 'Content-Type': 'application/json' }),
    });
  }

  createDeleteRequest<T>(url: string) {
    return this.http.delete<T>(this.configService.config().apiUrl + (url[0] != '/' ? '/' : '') + url, {
      headers: this.getAuthHeaders(),
    });
  }

  private getAuthHeaders(headers: any = {}): HttpHeaders {
    let token = localStorage.getItem("token");
    headers['Authorization'] = 'Bearer ' + token;

    // return new HttpHeaders({ 'Authorization': 'Bearer ' + token });
    return new HttpHeaders(headers);
  }
}
