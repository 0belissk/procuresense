import { HttpClient } from '@angular/common/http';
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
  lastQuantity: number;
  explanation?: string;
}

export interface BundleRecommendationDto {
  orgId: string;
  sku: string;
  relatedSku: string;
  relatedName: string;
  coPurchaseCount: number;
  rationale?: string;
}

@Injectable({ providedIn: 'root' })
export class ReorderService {
  private readonly baseUrl = environment.apiBaseUrl ?? '';

  constructor(private readonly http: HttpClient) {}

  loadDemoData(orgId: string, role: string): Observable<DemoLoadResponse> {
    const url = `${this.baseUrl}/api/purchases/demo/load`;
    return this.http.post<DemoLoadResponse>(url, {});
  }

  getReorders(orgId: string, role: string, limit = 20): Observable<ReorderPredictionDto[]> {
    const url = `${this.baseUrl}/api/purchases/insights/reorders?limit=${limit}`;
    return this.http.get<ReorderPredictionDto[]>(url);
  }

  getBundles(orgId: string, role: string, sku: string): Observable<BundleRecommendationDto[]> {
    const url = `${this.baseUrl}/api/purchases/insights/bundles/${sku}`;
    return this.http.get<BundleRecommendationDto[]>(url);
  }
}
