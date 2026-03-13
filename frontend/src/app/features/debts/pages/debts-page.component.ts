import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';

import type { CustomerDebtSummary } from '../models/debt.models';
import { DebtsApiService } from '../services/debts-api.service';

@Component({
  selector: 'app-debts-page',
  imports: [CommonModule],
  template: `
    <section class="page">
      <header class="hero">
        <div>
          <p class="eyebrow">Suivi financier</p>
          <h2>Dettes clients</h2>
          <p class="lede">Affiche les clients ayant des dettes ouvertes et le detail de chaque dette non reglee.</p>
        </div>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>

      <div class="stack">
        <article class="panel" *ngFor="let customer of debts()">
          <div class="panel-header">
            <div>
              <h3>{{ customer.customerName }}</h3>
              <p><span [class]="customerTypeChipClass(customer.customerType)">{{ customerTypeLabel(customer.customerType) }}</span></p>
            </div>
            <strong class="debt-total">{{ customer.totalOpenDebt | number:'1.2-2' }} EUR</strong>
          </div>

          <table class="table">
            <thead>
              <tr><th>Dette</th><th>Montant</th><th>Date</th><th></th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let debt of customer.debts">
                <td class="debt-label"><span class="money-alert-icon" title="Dette ouverte"><span class="bill back"></span><span class="bill front"></span><span class="slash"></span></span>{{ debt.label }}</td>
                <td class="debt-amount">{{ debt.amount | number:'1.2-2' }} EUR</td>
                <td>{{ debt.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
                <td><button type="button" (click)="settle(debt.debtId)">Marquer comme reglee</button></td>
              </tr>
            </tbody>
          </table>
        </article>

        <p class="empty" *ngIf="debts().length === 0">Aucune dette ouverte.</p>
      </div>
    </section>
  `,
  styles: `
    .page,.stack{display:grid;gap:1rem}
    .eyebrow{margin:0;color:#9a3412;text-transform:uppercase;letter-spacing:.14em;font-size:.72rem}
    .lede{margin:0;color:#475569;max-width:52rem}
    .panel{background:#fff;padding:1.2rem;border-radius:1.2rem;box-shadow:0 18px 40px rgba(15,23,42,.08)}
    .panel-header{display:flex;justify-content:space-between;gap:1rem;align-items:start}
    .panel-header h3,.panel-header p{margin:0}
    .debt-total{color:#b91c1c}
    .table{width:100%;border-collapse:collapse}
    .table th,.table td{padding:.78rem;border-bottom:1px solid #e2e8f0;text-align:left}
    .debt-label,.debt-amount{color:#b91c1c;font-weight:700}
    .debt-label{display:flex;align-items:center;gap:.55rem}
    .money-alert-icon{position:relative;display:inline-block;width:1.3rem;height:1.1rem;flex:0 0 auto}
    .money-alert-icon .bill{position:absolute;border-radius:.2rem;background:linear-gradient(180deg,#fecaca,#fca5a5);border:1px solid #b91c1c;box-shadow:inset 0 0 0 1px rgba(255,255,255,.24)}
    .money-alert-icon .bill::after{content:'';position:absolute;inset:.22rem .28rem;border:1px solid rgba(127,29,29,.55);border-radius:.16rem}
    .money-alert-icon .bill.back{width:.82rem;height:.56rem;top:.26rem;left:.1rem;opacity:.78}
    .money-alert-icon .bill.front{width:.88rem;height:.6rem;top:.08rem;left:.28rem}
    .money-alert-icon .slash{position:absolute;width:.16rem;height:1.18rem;background:#991b1b;border-radius:999px;transform:rotate(38deg);top:-.04rem;left:.56rem;box-shadow:0 0 0 1px rgba(255,255,255,.2)}
    .empty{margin:0;color:#64748b}
    .error{margin:0;color:#991b1b;font-weight:700}
  `,
})
export class DebtsPageComponent {
  private readonly api = inject(DebtsApiService);

  readonly debts = signal<CustomerDebtSummary[]>([]);
  readonly error = signal('');

  constructor() {
    this.load();
  }

  load(): void {
    this.api.list().subscribe({
      next: (debts) => {
        this.debts.set(debts);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Chargement des dettes impossible'),
    });
  }

  settle(debtId: string): void {
    this.api.settle(debtId).subscribe({
      next: () => this.load(),
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Reglement impossible'),
    });
  }

  customerTypeLabel(type: string): string {
    return type === 'SUBSCRIBER' ? 'Abonne' : 'Client';
  }

  customerTypeChipClass(type: string): string {
    return type === 'SUBSCRIBER' ? 'type-chip subscriber-chip' : 'type-chip walk-in-chip';
  }
}
