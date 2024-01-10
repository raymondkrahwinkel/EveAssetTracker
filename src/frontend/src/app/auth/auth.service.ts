import { Injectable } from '@angular/core';
import {JwtHelperService} from "@auth0/angular-jwt";
import {TokenInformation} from "../models/tokenInformation";

const jwtHelper = new JwtHelperService();

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  public isAuthenticated(): boolean {
    const token = localStorage.getItem("token");
    if(token == null || token?.length < 8) {
      console.log('isAuthenticated', 'invalid token!');
      return false;
    }

    // check if the token is expired
    try {
      return !jwtHelper.isTokenExpired(token);
    } catch(e) {
      console.error(e);
      return false;
    }
  }

  public getAuthenticatedInformation(): TokenInformation | null {
    const token = localStorage.getItem("token");
    if(token == null) {
      return null;
    }

    return jwtHelper.decodeToken<TokenInformation>(token);
  }
}
