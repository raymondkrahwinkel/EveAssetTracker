import {DashboardComponent} from "./dashboard/dashboard.component";
import {Route} from "@angular/router";

export default[
  {
    path: '', component: DashboardComponent, pathMatch: "full"
  }
] satisfies Route[];
