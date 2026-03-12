import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import type { DayCustomer } from '../models/monitoring.models';

@Injectable({ providedIn: 'root' })
export class MonitoringApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.usersApiUrl}/day-monitoring/customers`;
  customers() { return this.http.get<DayCustomer[]>(this.baseUrl); }
  customer(customerId: string) { return this.http.get<{ customerId: string; name: string; sales: string[]; sessions: string[] }>(`${this.baseUrl}/${customerId}`); }
}
