import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '../../../../environments/environment';
import type { ArchiveCandidate, ArchiveRequest, GeneratedArchiveFile } from '../models/archive.models';

@Injectable({ providedIn: 'root' })
export class ArchivingApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.usersApiUrl}/customers/archive`;

  candidates(payload: ArchiveRequest) {
    let params = new HttpParams()
      .set('startDate', payload.startDate)
      .set('endDate', payload.endDate);
    if (payload.type) {
      params = params.set('type', payload.type);
    }
    return this.http.get<ArchiveCandidate[]>(`${this.baseUrl}/candidates`, { params });
  }

  files() {
    return this.http.get<GeneratedArchiveFile[]>(`${this.baseUrl}/files`);
  }

  archive(payload: ArchiveRequest) {
    return this.http.post(`${this.baseUrl}/export`, payload, {
      observe: 'response',
      responseType: 'blob',
    });
  }

  debtsReport(payload: ArchiveRequest) {
    let params = new HttpParams()
      .set('startDate', payload.startDate)
      .set('endDate', payload.endDate);
    if (payload.type) {
      params = params.set('type', payload.type);
    }
    return this.http.get(`${this.baseUrl}/debts-report`, {
      observe: 'response',
      responseType: 'blob',
      params,
    });
  }

  fileName(response: HttpResponse<Blob>): string {
    const disposition = response.headers.get('content-disposition') ?? '';
    const match = disposition.match(/filename="([^"]+)"/i);
    return match?.[1] ?? 'customers-archive.csv';
  }
}
