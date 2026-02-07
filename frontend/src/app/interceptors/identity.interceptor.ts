import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { IdentityService } from '../services/identity.service';
import { DemoSettingsService } from '../services/demo-settings.service';

export const identityInterceptor: HttpInterceptorFn = (req, next) => {
  const identity = inject(IdentityService).identity();
  const useCachedAi = inject(DemoSettingsService).useCachedAi();
  const cloned = req.clone({
    setHeaders: {
      'X-Org-Id': identity.orgId,
      'X-Role': identity.role,
      'X-Use-Cached-AI': String(useCachedAi)
    }
  });
  return next(cloned);
};
