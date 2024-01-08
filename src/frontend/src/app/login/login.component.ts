import {Component, Inject} from '@angular/core';
import { CommonModule, DOCUMENT } from '@angular/common';
import {HttpClient} from "@angular/common/http";
import {environment} from "../../environments/environment";
import {ActivatedRoute, Router} from "@angular/router";
import {DeviceDetectorService} from "ngx-device-detector";
import {BackendService} from "../services/backend.service";
import {Title} from "@angular/platform-browser";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
  standalone: true,
  imports: [ CommonModule ]
})
export class LoginComponent {
  loginUrl = '';
  hasUrl : boolean = (this.loginUrl.length > 0);

  // todo: add loading indicator when url is being loaded from the backend

  constructor(
    private route: ActivatedRoute,
    private backend: BackendService,
    private router: Router,
    private http: HttpClient,
    private deviceDetector: DeviceDetectorService,
    private titleService: Title,
    @Inject(DOCUMENT) private document: Document
  ) {
    this.titleService.setTitle("Login to eve online");
    this.document.body.classList.add('text-center')

    const self = this;
    backend.getLoginUrl().then((url) => {
      if(url != null) {
        self.loginUrl = url;
        self.hasUrl = true;
      }
    });
  }

  ngOnInit() {
    const code = this.route.snapshot.queryParamMap.get('code');
    const state = this.route.snapshot.queryParamMap.get('state');

    if(code != null && state != null) {
      console.log(code, state);

      // validate the code via the API
      const self = this;

      let deviceInfo = this.deviceDetector.getDeviceInfo();
      console.log(environment.production, environment.apiUrl);
      this.http.get(environment.apiUrl + '/auth/validate?code=' + code + '&state=' + state + '&browser=' + deviceInfo.browser + '&deviceType=' + deviceInfo.deviceType + '&os=' + deviceInfo.os_version, { responseType: 'text' }).subscribe({
        next: (data) => {
          // Success
          localStorage.setItem("token", data);
          // console.log('authentication call', data);

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
      // alert('NOPE!');
    }
  }
}
