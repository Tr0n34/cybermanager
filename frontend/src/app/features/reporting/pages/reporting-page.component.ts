import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
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
          <div class="hero-actions">
            <button type="button" class="ghost filter-toggle" (click)="showFilters.set(!showFilters())" [attr.aria-expanded]="showFilters()">
              <span class="filter-icon" aria-hidden="true"></span>
              <span>Filtres</span>
            </button>
            <div class="pager" *ngIf="filteredCustomers().length > 0">
              <button type="button" class="ghost" (click)="previousPage()" [disabled]="currentPage() === 1">Precedent</button>
              <p class="meta">Page {{ currentPage() }} / {{ totalPages() }}</p>
              <button type="button" class="ghost" (click)="nextPage()" [disabled]="currentPage() === totalPages()">Suivant</button>
            </div>
          </div>
        </div>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>

      <form class="filters-grid collapsible report-filters" [class.is-collapsed]="!showFilters()" [formGroup]="form">
        <label class="field">
          <span>Du</span>
          <input type="date" formControlName="startDate" />
        </label>
        <label class="field">
          <span>Au</span>
          <input type="date" formControlName="endDate" />
        </label>
        <label class="field">
          <span>Nom</span>
          <input type="search" formControlName="term" placeholder="Filtrer par nom" />
        </label>
        <label class="field">
          <span>Clients par page</span>
          <select formControlName="pageSize">
            <option value="10">10</option>
            <option value="20">20</option>
            <option value="30">30</option>
            <option value="50">50</option>
          </select>
        </label>
        <div class="field action-field">
          <span>&nbsp;</span>
          <button type="button" class="secondary compact-button" (click)="load()" [disabled]="loading()">{{ loading() ? 'Chargement...' : 'Charger' }}</button>
        </div>
      </form>

      <div class="grid">
        <article class="panel">
          <div class="panel-head">
            <h3>Resultats</h3>
            <p class="muted">{{ filteredCustomers().length }} resultat(s)</p>
          </div>
          <table class="table" *ngIf="pagedCustomers().length > 0; else emptyState">
            <thead>
              <tr><th>Client</th><th>Type</th><th>Temps</th><th>Montant</th><th>Dette</th><th>Etat</th><th></th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of pagedCustomers()">
                <td>{{ item.name }}</td>
                <td><span [class]="customerTypeChipClass(item.type)">{{ customerTypeLabel(item.type) }}</span></td>
                <td>{{ item.totalMinutes }} min</td>
                <td>{{ item.salesTotal | number:'1.2-2' }} EUR</td>
                <td>{{ item.debtTotal | number:'1.2-2' }} EUR</td>
                <td>{{ item.state }}</td>
                <td><button type="button" (click)="detail(item.customerId)">{{ detailState()?.customerId === item.customerId ? 'Fermer' : 'Detail' }}</button></td>
              </tr>
            </tbody>
          </table>
          <ng-template #emptyState>
            <p class="muted">Aucun resultat pour cette periode.</p>
          </ng-template>
        </article>

        <div class="side-column stack">
          <aside class="panel side-panel">
            <div class="stack">
              <div class="side-head">
                <h3>Historique</h3>
              </div>

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

              <section class="stack">
                <h4>Clients non termines</h4>
                <div class="status-list" *ngIf="nonTerminatedCustomers().length > 0; else noOpenCustomers">
                  <article class="status-item" *ngFor="let item of nonTerminatedCustomers()">
                    <div class="status-head">
                      <strong>{{ item.name }}</strong>
                      <span [class]="customerTypeChipClass(item.type)">{{ customerTypeLabel(item.type) }}</span>
                    </div>
                    <small>{{ item.state }} - {{ item.totalMinutes }} min</small>
                  </article>
                </div>
                <ng-template #noOpenCustomers><p class="muted">Aucun client en cours sur la periode.</p></ng-template>
              </section>
            </div>
          </aside>
        </div>
      </div>
      <div class="modal-backdrop" *ngIf="detailState() as detail" (click)="closeDetail()">
        <div class="confirm-modal detail-modal" (click)="$event.stopPropagation()">
          <div class="side-head">
            <div>
              <h3>Detail</h3>
              <p class="muted">{{ detail.name }}</p>
            </div>
            <button type="button" class="ghost" (click)="closeDetail()">Fermer</button>
          </div>
          <div class="stack">
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
              <h4>Sessions</h4>
              <ul class="events" *ngIf="detail.sessions.length > 0; else noSessions">
                <li *ngFor="let item of detail.sessions">{{ item }}</li>
              </ul>
              <ng-template #noSessions><p class="muted">Aucune session sur la periode.</p></ng-template>
            </section>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: `
    .page,.stack{display:grid;gap:1rem}
    .hero{display:flex;align-items:flex-start;justify-content:space-between;gap:1rem;flex-wrap:wrap}
    .hero-actions{margin-top:.7rem;display:flex;justify-content:flex-start;align-items:center;flex-wrap:wrap}
    .report-filters{grid-template-columns:repeat(5,max-content);justify-content:start;align-items:end}
    .report-filters .field input,.report-filters .field select{width:auto;min-width:9.5rem;max-width:12rem;border-radius:999px;background:#fff;padding:.6rem .78rem !important}
    .action-field{align-self:end}
    .compact-button{padding:.56rem .82rem !important}
    .grid{display:grid;grid-template-columns:minmax(0,1.25fr) minmax(19rem,.75fr);gap:1rem;align-items:start}
    .panel,.stat-card{background:#fff;padding:1rem;border-radius:1rem}
    .side-column{align-content:start}
    .panel-head,.side-head{display:flex;align-items:center;justify-content:space-between;gap:1rem}
    .panel-head h3,.side-head h3,.hero h2{margin:0}
    .stats{display:grid;grid-template-columns:1fr;gap:.75rem}
    .stat-card span{display:block;color:#64748b;font-size:.8rem}
    .stat-card strong{font-size:1.15rem}
    .status-list{display:grid;gap:.55rem}
    .status-item{display:grid;gap:.2rem;padding:.72rem .8rem;border:1px solid #e2e8f0;border-radius:.9rem;background:#f8fafc}
    .status-head{display:flex;justify-content:space-between;gap:.75rem;align-items:center}
    .status-item strong,.status-item small{margin:0}
    .status-item small{color:#64748b}
    .table{width:100%;border-collapse:collapse}
    .table th,.table td{padding:.5rem .58rem;border-bottom:1px solid #e2e8f0;text-align:left;line-height:1.15}
    .compact th,.compact td{padding:.42rem .5rem}
    .detail-stats p,.muted,.error{margin:0}
    .events{margin:0;padding-left:1rem;display:grid;gap:.35rem}
    .error{color:#b91c1c}
    .modal-backdrop{position:fixed;inset:0;background:rgba(15,23,42,.4);display:grid;place-items:center;padding:1rem;z-index:40}
    .detail-modal{width:min(56rem,100%);max-height:85vh;overflow:auto;background:#fff;border-radius:1rem;padding:1rem;display:grid;gap:1rem}
    @media(max-width:1000px){
      .grid,.report-filters{grid-template-columns:1fr}
      .report-filters .field input{width:100%;min-width:0;max-width:none}
    }
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
  readonly showFilters = signal(false);
  readonly currentPage = signal(1);
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.filteredCustomers().length / this.pageSize())));
  readonly pagedCustomers = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize();
    return this.filteredCustomers().slice(start, start + this.pageSize());
  });
  readonly nonTerminatedCustomers = computed(() =>
    this.filteredCustomers().filter((item) => !['TERMINEE', 'PAYE'].includes(item.state.toUpperCase())),
  );
  readonly form = this.fb.nonNullable.group({
    startDate: [new Date().toISOString().slice(0, 10)],
    endDate: [new Date().toISOString().slice(0, 10)],
    term: [''],
    pageSize: [10],
  });
  readonly pageSize = computed(() => Number(this.form.controls.pageSize.value ?? 10) || 10);

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

    this.form.controls.pageSize.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.currentPage.set(1));

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
        this.currentPage.set(1);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.resolveHttpError(error, 'Chargement de l historique impossible'));
        this.loading.set(false);
      },
    });
  }

  detail(customerId: string): void {
    if (this.detailState()?.customerId === customerId) {
      this.detailState.set(null);
      return;
    }
    const { startDate, endDate } = this.form.getRawValue();
    this.api.customer(startDate, endDate, customerId).subscribe({
      next: (value) => {
        this.detailState.set(value);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, 'Chargement du detail impossible')),
    });
  }

  closeDetail(): void {
    this.detailState.set(null);
  }

  customerTypeLabel(type: string): string {
    return type === 'SUBSCRIBER' || type.toLowerCase().includes('abonn') ? 'Abonne' : 'Client';
  }

  customerTypeChipClass(type: string): string {
    return this.customerTypeLabel(type) === 'Abonne' ? 'type-chip subscriber-chip' : 'type-chip walk-in-chip';
  }

  private applyFilter(): void {
    const term = this.term();
    this.filteredCustomers.set(this.customers().filter((item) => !term || item.name.toLowerCase().includes(term)));
    this.currentPage.set(1);
  }

  previousPage(): void {
    this.currentPage.update((page) => Math.max(1, page - 1));
  }

  nextPage(): void {
    this.currentPage.update((page) => Math.min(this.totalPages(), page + 1));
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
