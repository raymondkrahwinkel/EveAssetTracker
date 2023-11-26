import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import {HttpClient} from "@angular/common/http";
import { environment } from './../environments/environment';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'frontend';
  loginUrl = '';

  constructor(private http: HttpClient) {
    const self = this;

    console.log(environment.production, environment.apiUrl);
    http.get(environment.apiUrl + '/auth/login/url', { responseType: 'text' }).subscribe(data => {
      console.log(data);
      self.loginUrl = data;
    });
  }
}
