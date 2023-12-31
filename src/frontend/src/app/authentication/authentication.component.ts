import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {ActivatedRoute, Router} from "@angular/router";
import {HttpClient, HttpErrorResponse} from "@angular/common/http";
import { DeviceDetectorService } from 'ngx-device-detector';
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
    private router: Router,
    private http: HttpClient,
    private deviceDetector: DeviceDetectorService
  ) {}

  ngOnInit() {
    const code = this.route.snapshot.queryParamMap.get('code');
    const state = this.route.snapshot.queryParamMap.get('state');

    if(code != null && state != null) {
      // validate the code via the API
      const self = this;

      let deviceInfo = this.deviceDetector.getDeviceInfo();
      console.log(environment.production, environment.apiUrl);
      this.http.get(environment.apiUrl + '/auth/validate?code=' + code + '&state=' + state + '&browser=' + deviceInfo.browser + '&deviceType=' + deviceInfo.deviceType + '&os=' + deviceInfo.os_version, { responseType: 'text' }).subscribe({
        next: (data) => {
        // Success
        localStorage.setItem("token", data);

        console.log('authentication call', data);

        // loggedin, redirect
        self.router.navigate([ '/' ]);
        },
        error: (error) => {
          // Failed
          console.error(error);
        }
      });
    }
    else {
      // todo: navigate to /
      alert('NOPE!');
    }

    console.log(code, state);
  }
}
