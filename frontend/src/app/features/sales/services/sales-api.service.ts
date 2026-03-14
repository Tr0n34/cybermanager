import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import type { ConnectionPricing, Sale } from '../models/sales.models';

@Injectable({ providedIn: 'root' })
export class SalesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.usersApiUrl}`;
  day() { return this.http.get<Sale[]>(`${this.baseUrl}/sales/day`); }
  productSale(payload: { customerId: string; sessionId?: string | null; lines: { productId: string; quantity: number }[]; createDebt: boolean }) { return this.http.post<Sale>(`${this.baseUrl}/sales/products`, payload); }
  subscriptionSale(payload: { customerId: string; sessionId?: string | null; subscriptionOfferId: string; createDebt: boolean }) { return this.http.post<Sale>(`${this.baseUrl}/sales/subscriptions`, payload); }
  deleteSubscriptionSale(saleId: string) { return this.http.delete<void>(`${this.baseUrl}/sales/${saleId}/subscription-session`); }
  connectionSale(payload: { customerId: string; sessionId?: string | null; minutes: number; createDebt: boolean }) { return this.http.post<Sale>(`${this.baseUrl}/sales/connection-time`, payload); }
  pricing() { return this.http.get<ConnectionPricing>(`${this.baseUrl}/pricing/connection-time`); }
  updatePricing(payload: { tiers: { hours: number; minutes: number; price: number }[] }) { return this.http.put<ConnectionPricing>(`${this.baseUrl}/pricing/connection-time`, payload); }
}
