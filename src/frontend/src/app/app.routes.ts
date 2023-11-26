import { Routes } from '@angular/router';
import {AuthenticationComponent} from "./authentication/authentication.component";

export const routes: Routes = [
  { path: 'auth/callback', component: AuthenticationComponent }
];
