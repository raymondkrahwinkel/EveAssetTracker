import {DashboardComponent} from "./dashboard/dashboard.component";
import {Route} from "@angular/router";
import {AuthenticatedComponent} from "./authenticated.component";

export default[
  { path: '', component: DashboardComponent, pathMatch: "full" },
  { path: 'logout', component: AuthenticatedComponent }
] satisfies Route[];
