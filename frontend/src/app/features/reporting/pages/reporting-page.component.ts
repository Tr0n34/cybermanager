import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import type { DayCustomerHistory, ReportingCustomerDetail } from '../models/reporting.models';
import { ReportingApiService } from '../services/reporting-api.service';

@Component({
  selector: 'app-reporting-page',
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="page">
      <header class="hero">
        <div>
          <p class="eyebrow">Historique</p>
          <h2>Historique journalier</h2>
        </div>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>

      <form class="toolbar" [formGroup]="form">
        <input type="date" formControlName="startDate" />
        <input type="date" formControlName="endDate" />
        <input type="search" formControlName="term" placeholder="Filtrer par nom" />
        <button type="button" (click)="load()" [disabled]="loading()">{{ loading() ? 'Chargement...' : 'Charger' }}</button>
      </form>

      <div class="stats">
        <article class="stat-card">
          <span>Argent encaisse</span>
          <strong>{{ totalCollected() | number:'1.2-2' }} EUR</strong>
        </article>
        <article class="stat-card">
          <span>Dettes actuelles</span>
          <strong>{{ totalDebtCreated() | number:'1.2-2' }} EUR</strong>
        </article>
      </div>

      <div class="grid">
        <article class="panel">
          <div class="panel-head">
            <h3>Resultats</h3>
            <p class="muted">{{ filteredCustomers().length }} resultat(s)</p>
          </div>
          <table class="table" *ngIf="filteredCustomers().length > 0; else emptyState">
            <thead>
              <tr><th>Client</th><th>Type</th><th>Temps</th><th>Montant</th><th>Dette</th><th>Etat</th><th></th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of filteredCustomers()">
                <td>{{ item.name }}</td>
                <td><span [class]="customerTypeChipClass(item.type)">{{ customerTypeLabel(item.type) }}</span></td>
                <td>{{ item.totalMinutes }} min</td>
                <td>{{ item.salesTotal | number:'1.2-2' }} EUR</td>
                <td>{{ item.debtTotal | number:'1.2-2' }} EUR</td>
                <td>{{ item.state }}</td>
                <td><button type="button" (click)="detail(item.customerId)">Detail</button></td>
              </tr>
            </tbody>
          </table>
          <ng-template #emptyState>
            <p class="muted">Aucun resultat pour cette periode.</p>
          </ng-template>
        </article>

        <article class="panel stack" *ngIf="detailState() as detail">
          <h3>{{ detail.name }}</h3>
          <div class="detail-stats">
            <p><strong>Argent encaisse :</strong> {{ detail.totalCollected | number:'1.2-2' }} EUR</p>
            <p><strong>Dettes actuelles :</strong> {{ detail.totalDebtCreated | number:'1.2-2' }} EUR</p>
          </div>

          <section class="stack">
            <h4>Vente</h4>
            <table class="table compact" *ngIf="detail.sales.length > 0; else noSales">
              <thead>
                <tr><th>Produit</th><th>Quantite</th><th>Prix</th></tr>
              </thead>
              <tbody>
                <tr *ngFor="let sale of detail.sales">
                  <td>{{ sale.label }}</td>
                  <td>{{ sale.quantity }}</td>
                  <td>{{ sale.totalPrice | number:'1.2-2' }} EUR</td>
                </tr>
              </tbody>
            </table>
            <ng-template #noSales><p class="muted">Aucune vente.</p></ng-template>
          </section>

          <section class="stack">
            <h4>Session</h4>
            <ul class="events" *ngIf="detail.sessions.length > 0; else noSessions">
              <li *ngFor="let item of detail.sessions">{{ item }}</li>
            </ul>
            <ng-template #noSessions><p class="muted">Aucun evenement de session.</p></ng-template>
          </section>
        </article>
      </div>
    </section>
  `,
  styles: `
    .page,.stack{display:grid;gap:1rem}
    .toolbar{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:.75rem}
    .stats{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1rem}
    .stat-card,.panel{background:#fff;padding:1rem;border-radius:1rem}
    .stat-card span{display:block;color:#64748b;font-size:.8rem}
    .stat-card strong{font-size:1.2rem}
    .grid{display:grid;grid-template-columns:1.15fr .85fr;gap:1rem;align-items:start}
    .panel{align-self:start}
    .panel-head{display:flex;align-items:center;justify-content:space-between;gap:1rem;margin-bottom:.5rem}
    .panel-head h3{margin:0}
    .table{width:100%;border-collapse:collapse}
    .table th,.table td{padding:.7rem;border-bottom:1px solid #e2e8f0;text-align:left}
    .compact th,.compact td{padding:.55rem}
    input,button{padding:.75rem;border-radius:.75rem;border:1px solid #cbd5e1}
    button{background:#14213d;color:#fff;border:0}
    .detail-stats p,.muted,.error{margin:0}
    .events{margin:0;padding-left:1rem;display:grid;gap:.35rem}
    .error{color:#b91c1c}
    @media(max-width:1000px){.grid,.toolbar,.stats{grid-template-columns:1fr}}
  `,
})
export class ReportingPageComponent {
  private readonly api = inject(ReportingApiService);
  private readonly fb = inject(FormBuilder);

  readonly customers = signal<DayCustomerHistory[]>([]);
  readonly filteredCustomers = signal<DayCustomerHistory[]>([]);
  readonly detailState = signal<ReportingCustomerDetail | null>(null);
  readonly totalCollected = signal(0);
  readonly totalDebtCreated = signal(0);
  readonly error = signal('');
  readonly loading = signal(false);
  readonly term = signal('');
  readonly form = this.fb.nonNullable.group({
    startDate: [new Date().toISOString().slice(0, 10)],
    endDate: [new Date().toISOString().slice(0, 10)],
    term: [''],
  });

  constructor() {
    this.form.controls.term.valueChanges
      .pipe(debounceTime(120), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((value) => {
        this.term.set(value.trim().toLowerCase());
        this.applyFilter();
      });

    this.form.controls.startDate.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.load());

    this.form.controls.endDate.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.load());

    this.load();
  }

  load(): void {
    const { startDate, endDate } = this.form.getRawValue();
    this.loading.set(true);
    this.api.day(startDate, endDate).subscribe({
      next: (value) => {
        this.customers.set(value.customers);
        this.totalCollected.set(value.totalCollected);
        this.totalDebtCreated.set(value.totalDebtCreated);
        this.detailState.set(null);
        this.error.set('');
        this.applyFilter();
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.resolveHttpError(error, 'Chargement de l historique impossible'));
        this.loading.set(false);
      },
    });
  }

  detail(customerId: string): void {
    const { startDate, endDate } = this.form.getRawValue();
    this.api.customer(startDate, endDate, customerId).subscribe({
      next: (value) => {
        this.detailState.set(value);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, 'Chargement du detail impossible')),
    });
  }

  customerTypeLabel(type: string): string {
    return type === 'SUBSCRIBER' ? 'Abonne' : 'Client';
  }

  customerTypeChipClass(type: string): string {
    return type === 'SUBSCRIBER' ? 'type-chip subscriber-chip' : 'type-chip walk-in-chip';
  }

  private applyFilter(): void {
    const term = this.term();
    this.filteredCustomers.set(
      this.customers().filter((item) => !term || item.name.toLowerCase().includes(term)),
    );
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
