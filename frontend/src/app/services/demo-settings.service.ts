import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class DemoSettingsService {
  private readonly cachedAi = signal(false);

  readonly useCachedAi = this.cachedAi.asReadonly();

  setUseCachedAi(enabled: boolean): void {
    this.cachedAi.set(enabled);
  }
}
