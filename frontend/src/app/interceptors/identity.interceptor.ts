import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { IdentityService } from '../services/identity.service';

export const identityInterceptor: HttpInterceptorFn = (req, next) => {
  const identity = inject(IdentityService).identity();
  const cloned = req.clone({
    setHeaders: {
      'X-Org-Id': identity.orgId,
      'X-Role': identity.role
    }
  });
  return next(cloned);
};
