import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import type { Product } from '../models/product.models';

@Injectable({ providedIn: 'root' })
export class ProductsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.usersApiUrl}/products`;

  search(term = '', status = '', category = '') {
    let params = new HttpParams();
    if (term) params = params.set('term', term);
    if (status) params = params.set('status', status);
    if (category) params = params.set('category', category);
    return this.http.get<Product[]>(this.baseUrl, { params });
  }

  create(payload: Omit<Product, 'productId' | 'status'>) { return this.http.post<Product>(this.baseUrl, payload); }
  update(productId: string, payload: Omit<Product, 'productId' | 'status'>) { return this.http.put<Product>(`${this.baseUrl}/${productId}`, payload); }
  activate(productId: string) { return this.http.put<Product>(`${this.baseUrl}/${productId}/activate`, {}); }
  deactivate(productId: string) { return this.http.put<Product>(`${this.baseUrl}/${productId}/deactivate`, {}); }
  delete(productId: string) { return this.http.delete<void>(`${this.baseUrl}/${productId}`); }
}
