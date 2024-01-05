import { Routes} from '@angular/router';
import {LoginComponent} from "./login/login.component";
import {DashboardComponent} from "./authenticated/dashboard/dashboard.component";
import {authGuard} from "./auth/auth.guard";
import {AuthenticatedComponent} from "./authenticated/authenticated.component";

export const routes: Routes = [
  { path: 'auth/callback', component: LoginComponent },
  { path: 'auth/login', component: LoginComponent },
  {
    path: '',
    component: AuthenticatedComponent,
    canActivate: [authGuard],
    loadChildren: () => import('./authenticated/routes')
  },
];
