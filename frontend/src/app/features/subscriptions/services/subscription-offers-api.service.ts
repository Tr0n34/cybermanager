import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import type { SubscriptionOffer } from '../models/subscription-offer.models';

@Injectable({ providedIn: 'root' })
export class SubscriptionOffersApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.usersApiUrl}/subscription-offers`;
  search(term = '', status = '') { let params = new HttpParams(); if (term) params = params.set('term', term); if (status) params = params.set('status', status); return this.http.get<SubscriptionOffer[]>(this.baseUrl, { params }); }
  create(payload: Omit<SubscriptionOffer, 'offerId' | 'status'>) { return this.http.post<SubscriptionOffer>(this.baseUrl, payload); }
  update(id: string, payload: Omit<SubscriptionOffer, 'offerId' | 'status'>) { return this.http.put<SubscriptionOffer>(`${this.baseUrl}/${id}`, payload); }
  activate(id: string) { return this.http.put<SubscriptionOffer>(`${this.baseUrl}/${id}/activate`, {}); }
  deactivate(id: string) { return this.http.put<SubscriptionOffer>(`${this.baseUrl}/${id}/deactivate`, {}); }
  delete(id: string) { return this.http.delete<void>(`${this.baseUrl}/${id}`); }
}
