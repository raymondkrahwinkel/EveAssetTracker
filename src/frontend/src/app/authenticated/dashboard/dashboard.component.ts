import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {BackendService} from "../../services/backend.service";
import {environment} from "../../../environments/environment";

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  constructor(private backend: BackendService) {
  }

  test(): void {
    if(!environment.production) {
      this.backend.getWallet(883434905).then((data) => {
        console.log('test result 883434905', data);
      });
      this.backend.getWallet(96380007).then((data) => {
        console.log('test result 96380007', data);
      });
      this.backend.getWallet(90550707).then((data) => {
        console.log('test result 90550707 (wrong)', data);
      });
    }
  }

  protected readonly environment = environment;
}
