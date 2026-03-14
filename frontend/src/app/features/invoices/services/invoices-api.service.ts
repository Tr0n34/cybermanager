import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '../../../../environments/environment';
import type { CreateInvoicePayload, InvoiceDetail, InvoiceSearchRequest, InvoiceSummary } from '../models/invoice.models';

@Injectable({ providedIn: 'root' })
export class InvoicesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.usersApiUrl}`;

  search(params: InvoiceSearchRequest) {
    return this.http.get<InvoiceSummary[]>(`${this.baseUrl}/invoices`, {
      params: Object.fromEntries(
        Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
      ),
    });
  }

  detail(invoiceId: string) {
    return this.http.get<InvoiceDetail>(`${this.baseUrl}/invoices/${invoiceId}`);
  }

  create(payload: CreateInvoicePayload) {
    return this.http.post(`${this.baseUrl}/invoices`, payload, { observe: 'response', responseType: 'blob' });
  }

  cancel(invoiceId: string) {
    return this.http.post<InvoiceDetail>(`${this.baseUrl}/invoices/${invoiceId}/cancel`, {});
  }

  pdf(invoiceId: string) {
    return this.http.get(`${this.baseUrl}/invoices/${invoiceId}/pdf`, { observe: 'response', responseType: 'blob' });
  }

  fileName(response: HttpResponse<Blob>): string {
    const header = response.headers.get('content-disposition') ?? '';
    const match = /filename="([^"]+)"/i.exec(header);
    return match?.[1] ?? 'facture.pdf';
  }
}
