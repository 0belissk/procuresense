import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface DemoLoadResponse {
  orgId: string;
  importedRows: number;
}

export interface ReorderPredictionDto {
  orgId: string;
  sku: string;
  productName: string;
  lastPurchaseAt: string;
  medianDaysBetween: number;
  predictedReorderAt: string;
  confidence: number;
}

@Injectable({ providedIn: 'root' })
export class ReorderService {
  private readonly baseUrl = environment.apiBaseUrl ?? '';

  constructor(private readonly http: HttpClient) {}

  loadDemoData(orgId: string, role: string): Observable<DemoLoadResponse> {
    const url = `${this.baseUrl}/api/purchases/demo/load`;
    return this.http.post<DemoLoadResponse>(url, {}, { headers: this.identityHeaders(orgId, role) });
  }

  getReorders(orgId: string, role: string, limit = 20): Observable<ReorderPredictionDto[]> {
    const url = `${this.baseUrl}/api/purchases/insights/reorders?limit=${limit}`;
    return this.http.get<ReorderPredictionDto[]>(url, { headers: this.identityHeaders(orgId, role) });
  }

  private identityHeaders(orgId: string, role: string): HttpHeaders {
    return new HttpHeaders({
      'X-Org-Id': orgId,
      'X-Role': role
    });
  }
}
