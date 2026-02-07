import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HealthService, HealthResponse } from '../services/health.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  private readonly statusState = signal<'CHECKING' | 'UP' | 'DOWN'>('CHECKING');
  readonly lastChecked = signal<string | null>(null);
  message = signal('Checking backend health...');
  details = signal<string | null>(null);

  readonly statusBadgeClass = computed(() => {
    switch (this.statusState()) {
      case 'UP':
        return 'status up';
      case 'DOWN':
        return 'status down';
      default:
        return 'status checking';
    }
  });

  constructor(private readonly healthService: HealthService) {}

  ngOnInit(): void {
    this.checkHealth();
  }

  checkHealth(): void {
    this.statusState.set('CHECKING');
    this.message.set('Checking backend health...');
    this.details.set(null);
    this.healthService.getHealth().subscribe({
      next: (response: HealthResponse) => {
        this.statusState.set(response.status === 'UP' ? 'UP' : 'DOWN');
        this.message.set(response.status === 'UP'
          ? 'Backend API is reachable'
          : 'Backend responded with an unhealthy status');
        this.details.set(response.timestamp);
        this.lastChecked.set(new Date().toLocaleString());
      },
      error: (err) => {
        this.statusState.set('DOWN');
        this.message.set('Backend API is unreachable');
        this.details.set(err?.message ?? 'Unknown error');
        this.lastChecked.set(new Date().toLocaleString());
      }
    });
  }

  retry(): void {
    this.checkHealth();
  }

  isLoading(): boolean {
    return this.statusState() === 'CHECKING';
  }
}
