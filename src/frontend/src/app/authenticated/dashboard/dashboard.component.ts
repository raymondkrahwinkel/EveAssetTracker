import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {BackendService} from "../../services/backend.service";
import {environment} from "../../../environments/environment";
import {WalletHistory} from "../../models/wallethistory";
import {TableModule} from "primeng/table";
import {FormattingService} from "../../services/formatting.service";
import {AuthenticatedComponent} from "../authenticated.component";

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, TableModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent {
  history: WalletHistory[] = [];

  constructor(private parent: AuthenticatedComponent, private backend: BackendService, protected formattingService: FormattingService) {
    this.parent.characterChanged.subscribe((character) => {
      this.loadWalletHistory();
    });
    this.parent.walletChanged.subscribe((value) => {
      this.loadWalletHistory();
    });
  }

  ngOnInit() {
    this.loadWalletHistory();
  }

  async loadWalletHistory() {
    this.backend.getWalletHistory().then((data) => {
      this.history = data.data;
    });
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
