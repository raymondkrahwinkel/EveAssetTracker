import {CanActivateFn, Router} from '@angular/router';
import {AuthService} from "./auth.service";
import {inject} from "@angular/core";

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if(!authService.isAuthenticated()) {
    console.debug('canActivate, not authenticated');

    router.navigate(['auth/login']);
    return false;
  }

  return true;
}

export const authGuardAnonymous: CanActivateFn = () => true;
