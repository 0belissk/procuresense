import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { DemoSettingsService } from './services/demo-settings.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  readonly useCachedAi = this.demoSettings.useCachedAi;

  constructor(private readonly demoSettings: DemoSettingsService) {}

  toggleCachedMode(enabled: boolean): void {
    this.demoSettings.setUseCachedAi(enabled);
  }
}
