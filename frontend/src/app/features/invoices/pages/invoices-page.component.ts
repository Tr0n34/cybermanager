import { CommonModule } from '@angular/common';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { distinctUntilChanged } from 'rxjs';

import type { InvoiceDetail, InvoiceStatus, InvoiceSummary } from '../models/invoice.models';
import { InvoicesApiService } from '../services/invoices-api.service';

@Component({
  selector: 'app-invoices-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './invoices-page.component.html',
  styleUrl: './invoices-page.component.css',
})
export class InvoicesPageComponent {
  private readonly api = inject(InvoicesApiService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly detailLoading = signal(false);
  readonly actionLoading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly invoices = signal<InvoiceSummary[]>([]);
  readonly selectedInvoiceId = signal<string | null>(null);
  readonly selectedInvoice = signal<InvoiceDetail | null>(null);
  readonly showFilters = signal(false);
  readonly panelMode = signal<'detail' | 'create'>('detail');
  readonly currentPage = signal(1);
  readonly pageSize = signal(10);
  readonly pageSizeOptions = [10, 20, 30, 50];
  readonly statusOptions: InvoiceStatus[] = ['DRAFT', 'ISSUED', 'CANCELLED'];

  readonly filters = this.fb.nonNullable.group({
    invoiceNumber: [''],
    customerName: [''],
    status: ['' as '' | InvoiceStatus],
    startDate: [''],
    endDate: [''],
    pageSize: [10],
  });
  readonly createForm = this.fb.group({
    customerId: [''],
    customerName: ['', Validators.required],
    lines: this.fb.array([this.createLineGroup()]),
  });

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.invoices().length / this.pageSize())));
  readonly paginatedInvoices = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize();
    return this.invoices().slice(start, start + this.pageSize());
  });

  constructor() {
    this.filters.controls.pageSize.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((value) => {
        this.pageSize.set(Number(value ?? 10) || 10);
        this.currentPage.set(1);
      });
    this.search();
  }

  get lines(): FormArray {
    return this.createForm.controls.lines as FormArray;
  }

  search(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.search(this.filters.getRawValue()).subscribe({
      next: (items) => {
        this.invoices.set(items);
        this.currentPage.set(1);
        if (items.length === 0) {
          this.selectedInvoiceId.set(null);
          this.selectedInvoice.set(null);
          this.loading.set(false);
          return;
        }
        const preferred = this.selectedInvoiceId() && items.some((item) => item.invoiceId === this.selectedInvoiceId())
          ? this.selectedInvoiceId()
          : items[0]?.invoiceId ?? null;
        this.loading.set(false);
        if (preferred) {
          this.open(preferred);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.resolveHttpError(error, 'Chargement des factures impossible.'));
        this.loading.set(false);
      },
    });
  }

  open(invoiceId: string): void {
    this.panelMode.set('detail');
    this.selectedInvoiceId.set(invoiceId);
    this.detailLoading.set(true);
    this.error.set('');
    this.api.detail(invoiceId).subscribe({
      next: (invoice) => {
        this.selectedInvoice.set(invoice);
        this.detailLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.resolveHttpError(error, 'Chargement du detail facture impossible.'));
        this.detailLoading.set(false);
      },
    });
  }

  openCreatePanel(): void {
    this.panelMode.set('create');
    this.selectedInvoiceId.set(null);
    this.selectedInvoice.set(null);
    this.error.set('');
    this.success.set('');
    this.resetCreateForm();
  }

  previousPage(): void {
    this.currentPage.update((value) => Math.max(1, value - 1));
  }

  nextPage(): void {
    this.currentPage.update((value) => Math.min(this.totalPages(), value + 1));
  }

  statusLabel(status: InvoiceStatus): string {
    switch (status) {
      case 'DRAFT':
        return 'Brouillon';
      case 'ISSUED':
        return 'Emise';
      case 'CANCELLED':
        return 'Annulee';
    }
  }

  statusChipClass(status: InvoiceStatus): string {
    switch (status) {
      case 'DRAFT':
        return 'status-chip draft-chip';
      case 'ISSUED':
        return 'status-chip issued-chip';
      case 'CANCELLED':
        return 'status-chip cancelled-chip';
    }
  }

  trackByInvoice(_index: number, item: InvoiceSummary): string {
    return item.invoiceId;
  }

  trackByLine(index: number): number {
    return index;
  }

  addLine(): void {
    this.lines.push(this.createLineGroup());
  }

  removeLine(index: number): void {
    if (this.lines.length <= 1) {
      return;
    }
    this.lines.removeAt(index);
  }

  createInvoice(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.actionLoading.set(true);
    this.error.set('');
    this.success.set('');
    const raw = this.createForm.getRawValue();
    this.api.create({
      customerId: raw.customerId?.trim() ? raw.customerId.trim() : null,
      customerName: raw.customerName?.trim() ?? '',
      lines: raw.lines.map((line) => ({
        label: line.label.trim(),
        quantity: Number(line.quantity),
        unitPrice: Number(line.unitPrice),
      })),
    }).subscribe({
      next: (response) => {
        this.download(response);
        const invoiceId = response.headers.get('X-Invoice-Id');
        this.success.set('Facture creee et PDF telecharge.');
        this.actionLoading.set(false);
        this.search();
        if (invoiceId) {
          this.open(invoiceId);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.resolveHttpError(error, 'Creation de facture impossible.'));
        this.actionLoading.set(false);
      },
    });
  }

  cancelInvoice(): void {
    const invoice = this.selectedInvoice();
    if (!invoice || invoice.status === 'CANCELLED') {
      return;
    }
    this.actionLoading.set(true);
    this.error.set('');
    this.success.set('');
    this.api.cancel(invoice.invoiceId).subscribe({
      next: (updated) => {
        this.selectedInvoice.set(updated);
        this.success.set('Facture annulee.');
        this.actionLoading.set(false);
        this.search();
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.resolveHttpError(error, 'Annulation de facture impossible.'));
        this.actionLoading.set(false);
      },
    });
  }

  reissuePdf(): void {
    const invoice = this.selectedInvoice();
    if (!invoice) {
      return;
    }
    this.actionLoading.set(true);
    this.error.set('');
    this.success.set('');
    this.api.pdf(invoice.invoiceId).subscribe({
      next: (response) => {
        this.download(response);
        this.success.set('Facture PDF reeditee.');
        this.actionLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.resolveHttpError(error, 'Reedition PDF impossible.'));
        this.actionLoading.set(false);
      },
    });
  }

  invoiceStatusAllowsCancel(status: InvoiceStatus): boolean {
    return status !== 'CANCELLED';
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

  private createLineGroup() {
    return this.fb.nonNullable.group({
      label: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1)]],
      unitPrice: [0, [Validators.required, Validators.min(0)]],
    });
  }

  private resetCreateForm(): void {
    this.createForm.reset({ customerId: '', customerName: '' });
    while (this.lines.length > 1) {
      this.lines.removeAt(this.lines.length - 1);
    }
    this.lines.at(0)?.reset({ label: '', quantity: 1, unitPrice: 0 });
  }

  private download(response: HttpResponse<Blob>): void {
    if (!response.body) {
      return;
    }
    const blobUrl = URL.createObjectURL(response.body);
    const link = document.createElement('a');
    link.href = blobUrl;
    link.download = this.api.fileName(response);
    link.click();
    URL.revokeObjectURL(blobUrl);
  }
}
