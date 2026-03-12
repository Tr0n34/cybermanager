import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import type { DayCustomerHistory } from '../models/reporting.models';

@Injectable({ providedIn: 'root' })
export class ReportingApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.usersApiUrl}/history/days`;
  day(date: string) { return this.http.get<{ date: string; customers: DayCustomerHistory[] }>(`${this.baseUrl}/${date}`); }
  customer(date: string, customerId: string) { return this.http.get<{ customerId: string; name: string; sales: string[]; sessions: string[] }>(`${this.baseUrl}/${date}/customers/${customerId}`); }
}
