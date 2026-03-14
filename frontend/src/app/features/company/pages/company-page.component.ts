import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CompanyApiService } from '../services/company-api.service';

@Component({
  selector: 'app-company-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './company-page.component.html',
  styleUrl: './company-page.component.css',
})
export class CompanyPageComponent {
  private readonly api = inject(CompanyApiService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly hasCompany = signal(false);
  readonly editing = signal(false);

  readonly form = this.fb.nonNullable.group({
    legalName: ['', Validators.required],
    siret: [''],
    phone: [''],
    email: [''],
    addressLine1: [''],
    addressLine2: [''],
    postalCode: [''],
    city: [''],
    country: ['France'],
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.current().subscribe({
      next: (company) => {
        this.hasCompany.set(!!company);
        this.editing.set(!company);
        if (company) {
          this.form.reset({
            legalName: company.legalName ?? '',
            siret: company.siret ?? '',
            phone: company.phone ?? '',
            email: company.email ?? '',
            addressLine1: company.addressLine1 ?? '',
            addressLine2: company.addressLine2 ?? '',
            postalCode: company.postalCode ?? '',
            city: company.city ?? '',
            country: company.country ?? 'France',
          });
        }
        if (company) {
          this.form.disable({ emitEvent: false });
        } else {
          this.form.enable({ emitEvent: false });
        }
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.resolveHttpError(error, 'Chargement impossible.'));
        this.loading.set(false);
      },
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    const raw = this.form.getRawValue();
    this.api.save({
      legalName: raw.legalName.trim(),
      siret: raw.siret.trim(),
      phone: raw.phone.trim(),
      email: raw.email.trim(),
      addressLine1: raw.addressLine1.trim(),
      addressLine2: raw.addressLine2.trim(),
      postalCode: raw.postalCode.trim(),
      city: raw.city.trim(),
      country: raw.country.trim(),
    }).subscribe({
      next: (company) => {
        this.hasCompany.set(true);
        this.editing.set(false);
        this.form.patchValue({
          legalName: company.legalName ?? '',
          siret: company.siret ?? '',
          phone: company.phone ?? '',
          email: company.email ?? '',
          addressLine1: company.addressLine1 ?? '',
          addressLine2: company.addressLine2 ?? '',
          postalCode: company.postalCode ?? '',
          city: company.city ?? '',
          country: company.country ?? 'France',
        });
        this.form.disable({ emitEvent: false });
        this.success.set('Entreprise enregistree.');
        this.saving.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.resolveHttpError(error, 'Enregistrement impossible.'));
        this.saving.set(false);
      },
    });
  }

  startEditing(): void {
    this.editing.set(true);
    this.form.enable({ emitEvent: false });
    this.success.set('');
  }

  cancelEditing(): void {
    if (this.hasCompany()) {
      this.load();
      return;
    }
    this.form.markAsPristine();
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
