import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';

import type { DayCustomer, MonitoringCustomerDetail } from '../models/monitoring.models';
import { MonitoringApiService } from '../services/monitoring-api.service';

@Component({
  selector: 'app-monitoring-page',
  imports: [CommonModule],
  template: `
    <section class="page">
      <header class="hero">
        <div>
          <p class="eyebrow">Vue operationnelle</p>
          <h2>Monitoring</h2>
        </div>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>

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
          <table class="table" *ngIf="customers().length > 0; else emptyState">
            <thead>
              <tr><th>Client</th><th>Type</th><th>Temps</th><th>Achats</th><th>Dette</th><th>Etat</th><th></th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of customers()">
                <td>{{ item.name }}</td>
                <td>{{ item.type }}</td>
                <td>{{ item.consumedMinutes }} min</td>
                <td>{{ item.purchasesTotal | number:'1.2-2' }} EUR</td>
                <td>{{ item.debtTotal | number:'1.2-2' }} EUR</td>
                <td>{{ item.state }}</td>
                <td><button type="button" (click)="loadDetail(item.customerId)">Detail</button></td>
              </tr>
            </tbody>
          </table>
          <ng-template #emptyState>
            <p class="muted">Aucune activite a afficher.</p>
          </ng-template>
        </article>

        <article class="panel stack" *ngIf="detail() as detail">
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
    .stats{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1rem}
    .stat-card,.panel{background:#fff;padding:1rem;border-radius:1rem}
    .stat-card span{display:block;color:#64748b;font-size:.8rem}
    .stat-card strong{font-size:1.2rem}
    .grid{display:grid;grid-template-columns:1.15fr .85fr;gap:1rem;align-items:start}
    .panel{align-self:start}
    .table{width:100%;border-collapse:collapse}
    .table th,.table td{padding:.7rem;border-bottom:1px solid #e2e8f0;text-align:left}
    .compact th,.compact td{padding:.55rem}
    .detail-stats p,.muted{margin:0}
    .events{margin:0;padding-left:1rem;display:grid;gap:.35rem}
    .error{margin:0;color:#b91c1c}
    button{padding:.6rem .8rem;border-radius:.75rem;border:0;background:#14213d;color:#fff}
    @media(max-width:1000px){.grid,.stats{grid-template-columns:1fr}}
  `,
})
export class MonitoringPageComponent {
  private readonly api = inject(MonitoringApiService);

  readonly customers = signal<DayCustomer[]>([]);
  readonly detail = signal<MonitoringCustomerDetail | null>(null);
  readonly error = signal('');
  readonly totalCollected = computed(() => this.customers().reduce((sum, item) => sum + item.collectedTotal, 0));
  readonly totalDebtCreated = computed(() => this.customers().reduce((sum, item) => sum + item.debtTotal, 0));

  constructor() {
    this.api.customers().subscribe({
      next: (value) => {
        this.customers.set(value);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, 'Chargement du monitoring impossible')),
    });
  }

  loadDetail(customerId: string): void {
    this.api.customer(customerId).subscribe({
      next: (value) => {
        this.detail.set(value);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, 'Chargement du detail impossible')),
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
