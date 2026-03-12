import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '../../../../environments/environment';
import type { CreateUserPayload, UpdateUserPayload, UserResponse } from '../models/user.models';

@Injectable({ providedIn: 'root' })
export class UsersApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.usersApiUrl}/users`;

  search(term: string, status: string) {
    let params = new HttpParams();
    if (term) {
      params = params.set('term', term);
    }
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<UserResponse[]>(this.baseUrl, { params });
  }

  get(userId: string) {
    return this.http.get<UserResponse>(`${this.baseUrl}/${userId}`);
  }

  create(payload: CreateUserPayload) {
    return this.http.post<UserResponse>(this.baseUrl, payload);
  }

  update(userId: string, payload: UpdateUserPayload) {
    return this.http.put<UserResponse>(`${this.baseUrl}/${userId}`, payload);
  }

  enable(userId: string) {
    return this.http.put<UserResponse>(`${this.baseUrl}/${userId}/enable`, {});
  }

  disable(userId: string) {
    return this.http.put<UserResponse>(`${this.baseUrl}/${userId}/disable`, {});
  }

  delete(userId: string) {
    return this.http.delete<void>(`${this.baseUrl}/${userId}`);
  }
}
