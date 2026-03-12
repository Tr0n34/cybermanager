import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '../../../../environments/environment';
import type { DayHistoryResponse, ReportingCustomerDetail } from '../models/reporting.models';

@Injectable({ providedIn: 'root' })
export class ReportingApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.usersApiUrl}/history/days`;

  day(startDate: string, endDate?: string) {
    return this.http.get<DayHistoryResponse>(`${this.baseUrl}?startDate=${startDate}&endDate=${endDate ?? startDate}`);
  }

  customer(startDate: string, endDate: string, customerId: string) {
    return this.http.get<ReportingCustomerDetail>(`${this.baseUrl}/customers/${customerId}?startDate=${startDate}&endDate=${endDate}`);
  }
}
