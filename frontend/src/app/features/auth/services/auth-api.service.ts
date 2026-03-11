import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '../../../../environments/environment';
import type { AuthenticationResponse, LoginPayload } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.authApiUrl}/auth`;

  login(payload: LoginPayload) {
    return this.http.post<AuthenticationResponse>(`${this.baseUrl}/login`, payload);
  }

  me() {
    return this.http.get<AuthenticationResponse>(`${this.baseUrl}/me`);
  }
}
