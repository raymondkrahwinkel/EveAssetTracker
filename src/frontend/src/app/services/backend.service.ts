import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {environment} from "../../environments/environment";
import {ResponsePing} from "../models/api/response.ping";
import {ResponseBaseWithData} from "../models/api/response.base.data";
import {Character} from "../models/character";

@Injectable({
  providedIn: 'root'
})
export class BackendService {

  constructor(private http: HttpClient) { }

  public async getLoginUrl(): Promise<any> {
    return new Promise<any>(resolve => {
      this.http.get(environment.apiUrl + '/auth/login/url', { responseType: 'text' })
        .subscribe(data => resolve(data));
    });
  }

  public async ping(): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      this.http.post<ResponsePing>(environment.apiUrl + '/auth/ping', '', {
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
      this.http.post(environment.apiUrl + '/auth/logout', '', {
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
      this.http.get<ResponseBaseWithData<Character>>(environment.apiUrl + '/character/' + id, {
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

  private getAuthHeaders(): HttpHeaders {
    let token = localStorage.getItem("token");
    return new HttpHeaders({'Authorization': 'Bearer ' + token });
  }
}
