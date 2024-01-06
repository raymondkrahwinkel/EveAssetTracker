import { Injectable } from '@angular/core';
import {JwtHelperService} from "@auth0/angular-jwt";

const jwtHelper = new JwtHelperService();

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  public isAuthenticated(): boolean {
    const token = localStorage.getItem("token");
    // check if the token is expired
    return !jwtHelper.isTokenExpired(token);
  }
}
