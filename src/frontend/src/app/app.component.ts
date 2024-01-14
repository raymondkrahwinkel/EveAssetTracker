import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {NavigationEnd, Router, RouterOutlet} from '@angular/router';
import {Title} from "@angular/platform-browser";
import {filter} from "rxjs";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  constructor(
    private readonly router: Router,
    private readonly titleService: Title
  ) { }

  ngOnInit() {
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.titleService.setTitle(this.getNestedRouteTitles().join(' | '));
    });
  }

  getNestedRouteTitles(): string[] {
    let currentRoute = this.router.routerState.root.firstChild;
    const titles: string[] = [];

    while (currentRoute) {
      if (currentRoute.snapshot.routeConfig?.data) {
        titles.push(currentRoute.snapshot.routeConfig.data['title']);
      }

      currentRoute = currentRoute.firstChild;
    }

    return titles;
  }

  getLastRouteTitle(): string {
    let currentRoute = this.router.routerState.root.firstChild;

    while (currentRoute?.firstChild) {
      currentRoute = currentRoute.firstChild;
    }

    return currentRoute?.snapshot.data['title'];
  }
}
