import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface HealthResponse {
  status: 'UP' | 'DOWN';
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class HealthService {
  private readonly baseUrl = environment.apiBaseUrl;
  private readonly headers = new HttpHeaders({
    'X-Org-Id': environment.identity.orgId,
    'X-Role': environment.identity.role
  });

  constructor(private readonly http: HttpClient) {}

  getHealth(): Observable<HealthResponse> {
    const url = this.baseUrl ? `${this.baseUrl}/api/health` : '/api/health';
    return this.http.get<HealthResponse>(url, {
      headers: this.headers
    });
  }
}
