import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '../../../../environments/environment';
import type { CustomerDebtSummary } from '../models/debt.models';

@Injectable({ providedIn: 'root' })
export class DebtsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.usersApiUrl}/customers/debts`;

  list() {
    return this.http.get<CustomerDebtSummary[]>(this.baseUrl);
  }

  settle(debtId: string, comment: string) {
    return this.http.post<void>(`${this.baseUrl}/${debtId}/settle`, { comment });
  }
}
