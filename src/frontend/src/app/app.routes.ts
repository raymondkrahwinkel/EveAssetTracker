import { Routes} from '@angular/router';
import {LoginComponent} from "./login/login.component";
import {authGuard} from "./auth/auth.guard";
import {AuthenticatedComponent} from "./authenticated/authenticated.component";
import {ValidateComponent} from "./validate/validate.component";

export const routes: Routes = [
  { path: 'auth/callback', component: ValidateComponent },
  { path: 'auth/login', component: LoginComponent },
  {
    path: '',
    component: AuthenticatedComponent,
    canActivate: [authGuard],
    loadChildren: () => import('./authenticated/routes')
  },
];
