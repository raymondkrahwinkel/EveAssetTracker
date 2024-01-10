import {Component, Inject} from '@angular/core';
import { CommonModule, DOCUMENT } from '@angular/common';
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
    private backend: BackendService,
    private titleService: Title,
    @Inject(DOCUMENT) private document: Document
  ) {
    this.titleService.setTitle("Login to eve online");
    this.document.body.classList.add('text-center')

    const self = this;
    backend.getLoginUrl(null).then((url) => {
      if(url != null) {
        self.loginUrl = url;
        self.hasUrl = true;
      }
    });
  }
}
