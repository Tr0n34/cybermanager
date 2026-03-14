import { CommonModule } from '@angular/common';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { distinctUntilChanged } from 'rxjs';

import type { ArchiveCandidate, ArchiveRequest, GeneratedArchiveFile } from '../models/archive.models';
import { ArchivingApiService } from '../services/archiving-api.service';

@Component({
  selector: 'app-archiving-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './archiving-page.component.html',
  styleUrl: './archiving-page.component.css',
})
export class ArchivingPageComponent {
  private readonly api = inject(ArchivingApiService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly archiving = signal(false);
  readonly exportingDebtReport = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly candidates = signal<ArchiveCandidate[]>([]);
  readonly generatedFiles = signal<GeneratedArchiveFile[]>([]);
  readonly hasLoaded = signal(false);
  readonly showFilters = signal(false);
  readonly page = signal(1);
  readonly pageSize = signal(10);
  readonly form = this.fb.nonNullable.group({
    startDate: [this.defaultStartDate(), Validators.required],
    endDate: [this.defaultEndDate(), Validators.required],
    type: ['WALK_IN' as '' | 'WALK_IN'],
    pageSize: [10],
  });

  readonly totalSales = computed(() => this.candidates().reduce((total, item) => total + item.salesTotal, 0));
  readonly totalDebt = computed(() => this.candidates().reduce((total, item) => total + item.debtTotal, 0));
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.candidates().length / this.pageSize())));
  readonly currentPage = computed(() => Math.min(this.page(), this.totalPages()));
  readonly pagedCandidates = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize();
    return this.candidates().slice(start, start + this.pageSize());
  });

  constructor() {
    this.loadGeneratedFiles();
    this.form.controls.pageSize.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((value) => {
        this.pageSize.set(Number(value ?? 10) || 10);
        this.page.set(1);
      });
  }

  loadCandidates(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.success.set('');
    this.api.candidates(this.payload()).subscribe({
      next: (items) => {
        this.candidates.set(items);
        this.hasLoaded.set(true);
        this.page.set(1);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.resolveHttpError(error, 'Chargement impossible'));
        this.loading.set(false);
      },
    });
  }

  archive(format: 'csv' | 'xlsx'): void {
    if (this.form.invalid || this.candidates().length === 0) {
      return;
    }
    this.archiving.set(true);
    this.error.set('');
    this.success.set('');
    this.api.archive({ ...this.payload(), format }).subscribe({
      next: (response) => {
        this.download(response);
        const count = Number(response.headers.get('X-Archived-Count') ?? this.candidates().length);
        this.success.set(`${count} client${count > 1 ? 's' : ''} archive${count > 1 ? 's' : ''}.`);
        this.archiving.set(false);
        this.loadCandidates();
        this.loadGeneratedFiles();
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.resolveHttpError(error, 'Archivage impossible'));
        this.archiving.set(false);
      },
    });
  }

  exportDebtReport(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.exportingDebtReport.set(true);
    this.error.set('');
    this.success.set('');
    this.api.debtsReport(this.payload()).subscribe({
      next: (response) => {
        this.download(response);
        this.success.set('Rapport PDF des dettes genere.');
        this.exportingDebtReport.set(false);
        this.loadGeneratedFiles();
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.resolveHttpError(error, 'Generation du rapport des dettes impossible'));
        this.exportingDebtReport.set(false);
      },
    });
  }

  previousPage(): void {
    this.page.update((value) => Math.max(1, value - 1));
  }

  nextPage(): void {
    this.page.update((value) => Math.min(this.totalPages(), value + 1));
  }

  customerTypeLabel(type: ArchiveCandidate['type']): string {
    return type === 'SUBSCRIBER' ? 'Abonne' : 'Client';
  }

  customerTypeChipClass(type: ArchiveCandidate['type']): string {
    return type === 'SUBSCRIBER' ? 'type-chip subscriber-chip' : 'type-chip walk-in-chip';
  }

  private loadGeneratedFiles(): void {
    this.api.files().subscribe({
      next: (files) => this.generatedFiles.set(files),
      error: () => undefined,
    });
  }

  private payload(): ArchiveRequest {
    const raw = this.form.getRawValue();
    return {
      startDate: raw.startDate,
      endDate: raw.endDate,
      type: raw.type,
    };
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

  private resolveHttpError(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }
    if (error.error?.message) {
      return error.error.message;
    }
    return fallback;
  }

  private defaultStartDate(): string {
    const value = new Date();
    value.setMonth(value.getMonth() - 1);
    return value.toISOString().slice(0, 10);
  }

  private defaultEndDate(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
