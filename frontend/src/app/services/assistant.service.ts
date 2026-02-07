import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AssistantChatRequestDto {
  message: string;
  context?: {
    selectedSku?: string;
    orgType?: string;
    projectType?: string;
  };
}

export interface ShoppingListItemDto {
  sku: string;
  name: string;
  qty: number;
  reason: string;
}

export interface AssistantChatResponseDto {
  replyText: string;
  shoppingList?: ShoppingListItemDto[];
}

@Injectable({ providedIn: 'root' })
export class AssistantService {
  private readonly baseUrl = environment.apiBaseUrl ?? '';

  constructor(private readonly http: HttpClient) {}

  chat(request: AssistantChatRequestDto): Observable<AssistantChatResponseDto> {
    const url = `${this.baseUrl}/api/assistant/chat`;
    return this.http.post<AssistantChatResponseDto>(url, request);
  }
}
