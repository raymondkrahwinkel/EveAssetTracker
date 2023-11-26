import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {ActivatedRoute} from "@angular/router";
import {HttpClient, HttpErrorResponse} from "@angular/common/http";
import { environment } from './../../environments/environment';

@Component({
  selector: 'app-authentication',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './authentication.component.html',
  styleUrl: './authentication.component.css'
})
export class AuthenticationComponent {
  constructor(
    private route: ActivatedRoute,
    private http: HttpClient
  ) {}

  ngOnInit() {
    const code = this.route.snapshot.queryParamMap.get('code');
    const state = this.route.snapshot.queryParamMap.get('state');

    if(code != null && state != null) {
      // validate the code via the API
      const self = this;

      console.log(environment.production, environment.apiUrl);
      this.http.get(environment.apiUrl + '/auth/validate?code=' + code + '&state=' + state, { responseType: 'text' }).subscribe({
        next: (data) => {
            // Success
            localStorage.setItem("name", data.split(' ')[0]);
            localStorage.setItem("token", data.split(' ')[1]);

            // loggedin, redirect

            console.log('authentication call', data);
          },
          error: (error) => {
            // Failed
            console.error(error);
          }
        }
      );
    }
    else {
      // todo: navigate to /
      alert('NOPE!');
    }

    console.log(code, state);
  }
}
