import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import type { DayCustomer, MonitoringCustomerDetail } from '../models/monitoring.models';
import { MonitoringApiService } from '../services/monitoring-api.service';

@Component({
  selector: 'app-monitoring-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './monitoring-page.component.html',
  styleUrl: './monitoring-page.component.css',
})
export class MonitoringPageComponent {
  private readonly api = inject(MonitoringApiService);
  readonly pageSizeOptions = [5, 10, 15, 20];

  readonly customers = signal<DayCustomer[]>([]);
  readonly detail = signal<MonitoringCustomerDetail | null>(null);
  readonly error = signal('');
  readonly showFilters = signal(false);
  readonly customerFilter = signal('');
  readonly stateFilter = signal('ALL');
  readonly debtFilter = signal('ALL');
  readonly currentPage = signal(1);
  readonly pageSize = signal(10);
  readonly showFullHistory = signal(false);
  readonly currentSalesPage = signal(1);
  readonly currentSessionsPage = signal(1);
  readonly stateOptions = computed(() =>
    [...new Set(this.customers().map((item) => item.state).filter((state) => state.trim().length > 0))].sort((left, right) =>
      left.localeCompare(right, 'fr', { sensitivity: 'base' }),
    ),
  );
  readonly filteredCustomers = computed(() => {
    const term = this.customerFilter().trim().toLowerCase();
    return this.customers().filter((item) => {
      const matchesCustomer = term.length === 0 || item.name.toLowerCase().includes(term);
      const matchesState = this.stateFilter() === 'ALL' || item.state === this.stateFilter();
      const matchesDebt =
        this.debtFilter() === 'ALL'
        || (this.debtFilter() === 'YES' && item.debtTotal > 0)
        || (this.debtFilter() === 'NO' && item.debtTotal <= 0);
      return matchesCustomer && matchesState && matchesDebt;
    });
  });
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.filteredCustomers().length / this.pageSize())));
  readonly pagedCustomers = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize();
    return this.filteredCustomers().slice(start, start + this.pageSize());
  });
  readonly totalCollected = computed(() => this.filteredCustomers().reduce((sum, item) => sum + item.collectedTotal, 0));
  readonly totalDebtCreated = computed(() => this.filteredCustomers().reduce((sum, item) => sum + item.debtTotal, 0));
  readonly salesTotalPages = computed(() => Math.max(1, Math.ceil((this.detail()?.sales.length ?? 0) / 10)));
  readonly sessionsTotalPages = computed(() => Math.max(1, Math.ceil((this.detail()?.sessions.length ?? 0) / 10)));
  readonly visibleSales = computed(() => {
    const sales = this.detail()?.sales ?? [];
    if (!this.showFullHistory()) {
      return sales.slice(0, 10);
    }
    const start = (this.currentSalesPage() - 1) * 10;
    return sales.slice(start, start + 10);
  });
  readonly visibleSessions = computed(() => {
    const sessions = this.detail()?.sessions ?? [];
    if (!this.showFullHistory()) {
      return sessions.slice(0, 10);
    }
    const start = (this.currentSessionsPage() - 1) * 10;
    return sessions.slice(start, start + 10);
  });

  constructor() {
    this.loadCustomers();
  }

  loadDetail(customerId: string): void {
    if (this.detail()?.customerId === customerId) {
      this.closeDetail();
      return;
    }
    this.api.customer(customerId).subscribe({
      next: (value) => {
        this.detail.set(value);
        this.showFullHistory.set(false);
        this.currentSalesPage.set(1);
        this.currentSessionsPage.set(1);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, 'Chargement du detail impossible')),
    });
  }

  closeDetail(): void {
    this.detail.set(null);
    this.showFullHistory.set(false);
  }

  updateCustomerFilter(value: string): void {
    this.customerFilter.set(value);
    this.currentPage.set(1);
  }

  updateStateFilter(value: string): void {
    this.stateFilter.set(value);
    this.currentPage.set(1);
  }

  updateDebtFilter(value: string): void {
    this.debtFilter.set(value);
    this.currentPage.set(1);
  }

  updatePageSize(value: number | string): void {
    const nextSize = Number(value);
    this.pageSize.set(Number.isFinite(nextSize) && nextSize > 0 ? nextSize : 10);
    this.currentPage.set(1);
  }

  previousPage(): void {
    this.currentPage.update((page) => Math.max(1, page - 1));
  }

  nextPage(): void {
    this.currentPage.update((page) => Math.min(this.totalPages(), page + 1));
  }

  toggleFullHistory(): void {
    const nextValue = !this.showFullHistory();
    this.showFullHistory.set(nextValue);
    this.currentSalesPage.set(1);
    this.currentSessionsPage.set(1);
  }

  previousSalesPage(): void {
    this.currentSalesPage.update((page) => Math.max(1, page - 1));
  }

  nextSalesPage(): void {
    this.currentSalesPage.update((page) => Math.min(this.salesTotalPages(), page + 1));
  }

  previousSessionsPage(): void {
    this.currentSessionsPage.update((page) => Math.max(1, page - 1));
  }

  nextSessionsPage(): void {
    this.currentSessionsPage.update((page) => Math.min(this.sessionsTotalPages(), page + 1));
  }

  canShowFullHistoryButton(): boolean {
    return (this.detail()?.sales.length ?? 0) > 10 || (this.detail()?.sessions.length ?? 0) > 10;
  }

  customerTypeLabel(type: string): string {
    return type === 'SUBSCRIBER' || type.toLowerCase().includes('abonn') ? 'Abonne' : 'Client';
  }

  customerTypeChipClass(type: string): string {
    return this.customerTypeLabel(type) === 'Abonne' ? 'type-chip subscriber-chip' : 'type-chip walk-in-chip';
  }

  private loadCustomers(): void {
    this.api.customers().subscribe({
      next: (value) => {
        this.customers.set(value);
        this.currentPage.set(1);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, 'Chargement du monitoring impossible')),
    });
  }

  private resolveHttpError(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }
    if (error.error?.message) {
      return error.error.message;
    }
    return fallback;
  }
}
