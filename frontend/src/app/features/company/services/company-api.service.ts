import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '../../../../environments/environment';
import type { CompanyProfile, CompanyProfilePayload } from '../models/company.models';

@Injectable({ providedIn: 'root' })
export class CompanyApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.usersApiUrl}/company`;

  current() {
    return this.http.get<CompanyProfile | null>(this.baseUrl);
  }

  save(payload: CompanyProfilePayload) {
    return this.http.put<CompanyProfile>(this.baseUrl, payload);
  }
}
