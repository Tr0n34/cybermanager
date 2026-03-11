import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '../../../../environments/environment';
import type { Customer, CustomerDetails } from '../models/customer.models';

@Injectable({ providedIn: 'root' })
export class CustomersApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.usersApiUrl}/customers`;

  search(term = '', type = '') {
    let params = new HttpParams();
    if (term) params = params.set('term', term);
    if (type) params = params.set('type', type);
    return this.http.get<Customer[]>(this.baseUrl, { params });
  }

  get(id: string) {
    return this.http.get<CustomerDetails>(`${this.baseUrl}/${id}`);
  }

  create(payload: { name: string; type: 'WALK_IN' | 'SUBSCRIBER'; subscriptionOfferId?: string | null }) {
    return this.http.post<Customer>(this.baseUrl, payload);
  }

  update(id: string, payload: { name: string }) {
    return this.http.put<Customer>(`${this.baseUrl}/${id}`, payload);
  }

  convert(id: string, payload: { subscriptionOfferId: string; deductCurrentSession: boolean }) {
    return this.http.post(`${this.baseUrl}/${id}/convert-to-subscriber`, payload);
  }
}
