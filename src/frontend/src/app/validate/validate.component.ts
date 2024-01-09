import {Component, Inject} from '@angular/core';
import {CommonModule, DOCUMENT} from '@angular/common';
import {ActivatedRoute, Router} from "@angular/router";
import {BackendService} from "../services/backend.service";
import {HttpClient} from "@angular/common/http";
import {DeviceDetectorService} from "ngx-device-detector";
import {Title} from "@angular/platform-browser";
import {environment} from "../../environments/environment";
import {ResponseValidate} from "../models/api/response.validate";

@Component({
  selector: 'app-validate',
  standalone: true,
  imports: [CommonModule],
  template: '<p>Place wait while validating information...</p>'
})
export class ValidateComponent {
  constructor(
    private route: ActivatedRoute,
    private backend: BackendService,
    private router: Router,
    private http: HttpClient,
    private deviceDetector: DeviceDetectorService,
    private titleService: Title,
    @Inject(DOCUMENT) private document: Document
  ) {
    this.titleService.setTitle("Validating eve online login...");
    this.document.body.classList.add('text-center')
  }

  ngOnInit() {
    const code = this.route.snapshot.queryParamMap.get('code');
    const state = this.route.snapshot.queryParamMap.get('state');

    const self = this;
    if(code != null && state != null) {
      // validate the code via the API
      let deviceInfo = this.deviceDetector.getDeviceInfo();
      console.log(environment.production, environment.apiUrl);
      this.http.get<ResponseValidate>(environment.apiUrl + '/auth/validate?code=' + code + '&state=' + state + '&browser=' + deviceInfo.browser + '&deviceType=' + deviceInfo.deviceType + '&os=' + deviceInfo.os_version).subscribe({
        next: (data) => {
          // Success
          if(!data.childCharacterValidation) {
            localStorage.setItem("token", data.data);
          }

          console.log('authentication call', data);

          // loggedin, redirect
          self.router.navigate(['/']);
        },
        error: (error) => {
          // Failed
          console.error(error);
        }
      });
    }
    else
    {
      self.router.navigate(['auth/login/']);
    }
  }
}
