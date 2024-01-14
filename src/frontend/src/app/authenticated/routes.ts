import {DashboardComponent} from "./dashboard/dashboard.component";
import {Route} from "@angular/router";
import {AuthenticatedComponent} from "./authenticated.component";

export default[
  { path: '', component: DashboardComponent, pathMatch: "full", data: { title: "Dashboard" } },
  { path: 'logout', component: AuthenticatedComponent }
] satisfies Route[];
