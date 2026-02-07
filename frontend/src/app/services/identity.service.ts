import { Injectable, signal } from '@angular/core';
import { environment } from '../../environments/environment';

export interface IdentityState {
  orgId: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class IdentityService {
  private readonly state = signal<IdentityState>({
    orgId: environment.identity.orgId,
    role: environment.identity.role
  });

  identity = this.state.asReadonly();

  update(orgId: string, role: string): void {
    this.state.set({ orgId, role });
  }
}
