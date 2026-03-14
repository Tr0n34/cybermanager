import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '../../../../environments/environment';
import type { CafeSession } from '../models/session.models';

@Injectable({ providedIn: 'root' })
export class SessionsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.usersApiUrl}/sessions`;

  day() {
    return this.http.get<CafeSession[]>(`${this.baseUrl}/day`);
  }

  current() {
    return this.http.get<{ sessions: CafeSession[] }>(`${this.baseUrl}/current`);
  }

  start(payload: { customerId?: string; customerName?: string }) {
    return this.http.post<CafeSession>(`${this.baseUrl}/start`, payload);
  }

  stop(sessionId: string) {
    return this.http.post<CafeSession>(`${this.baseUrl}/${sessionId}/stop`, {});
  }

  pay(sessionId: string, payload: { amountPaid: number; subscriptionOfferIds: string[]; createSubscriptionDebt: boolean }) {
    return this.http.post<CafeSession>(`${this.baseUrl}/${sessionId}/pay`, payload);
  }

  restartDay() {
    return this.http.post<{ archivedSessions: number }>(`${this.baseUrl}/restart-day`, {});
  }

  pause(sessionId: string) {
    return this.http.post<CafeSession>(`${this.baseUrl}/${sessionId}/pause`, {});
  }

  resume(sessionId: string) {
    return this.http.post<CafeSession>(`${this.baseUrl}/${sessionId}/resume`, {});
  }
}
