import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import type { CustomerDebtSummary } from '../models/debt.models';
import { DebtsApiService } from '../services/debts-api.service';

@Component({
  selector: 'app-debts-page',
  imports: [CommonModule, FormsModule],
  template: `
    <section class="page">
      <header class="hero">
        <div>
          <p class="eyebrow">Suivi financier</p>
          <h2>Dettes clients</h2>
          <p class="lede">Affiche les clients ayant des dettes ouvertes et le detail de chaque dette non reglee.</p>
          <div class="hero-actions">
            <button type="button" class="ghost filter-toggle" (click)="showFilters.set(!showFilters())" [attr.aria-expanded]="showFilters()">
              <span class="filter-icon" aria-hidden="true"></span>
              <span>Filtres</span>
            </button>
            <div class="pager" *ngIf="debts().length > 0">
              <button type="button" class="ghost" (click)="previousPage()" [disabled]="currentPage() === 1">Precedent</button>
              <p class="meta">Page {{ currentPage() }} / {{ totalPages() }}</p>
              <button type="button" class="ghost" (click)="nextPage()" [disabled]="currentPage() === totalPages()">Suivant</button>
            </div>
          </div>
        </div>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>

      <div class="filters-grid collapsible debt-filters" [class.is-collapsed]="!showFilters()">
        <label class="field">
          <span>Dettes par page</span>
          <select [ngModel]="pageSize()" (ngModelChange)="updatePageSize($event)">
            <option [ngValue]="5">5</option>
            <option [ngValue]="10">10</option>
            <option [ngValue]="15">15</option>
            <option [ngValue]="20">20</option>
          </select>
        </label>
      </div>

      <div class="stack">
        <article class="panel" *ngFor="let customer of pagedDebts()">
          <div class="panel-header">
            <div>
              <h3>{{ customer.customerName }}</h3>
              <p><span [class]="customerTypeChipClass(customer.customerType)">{{ customerTypeLabel(customer.customerType) }}</span></p>
            </div>
            <strong class="debt-total">{{ customer.totalOpenDebt | number:'1.2-2' }} EUR</strong>
          </div>

          <table class="table">
            <thead>
              <tr><th>Dette</th><th>Commentaire</th><th>Montant</th><th>Date</th><th></th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let debt of customer.debts">
                <td class="debt-label-cell">
                  <div class="cell-content debt-label">
                    <span class="money-alert-icon" title="Dette ouverte"><span class="bill back"></span><span class="bill front"></span><span class="slash"></span></span>
                    <span>{{ debt.label }}</span>
                  </div>
                </td>
                <td class="comment-cell">
                  <div class="cell-content">
                    <input
                      [ngModel]="commentValue(debt.debtId, debt.comment)"
                      (ngModelChange)="updateComment(debt.debtId, $event)"
                      placeholder="Commentaire"
                    />
                  </div>
                </td>
                <td class="debt-amount">
                  <div class="cell-content">{{ debt.amount | number:'1.2-2' }} EUR</div>
                </td>
                <td>
                  <div class="cell-content">{{ debt.createdAt | date:'dd/MM/yyyy HH:mm' }}</div>
                </td>
                <td class="settle-cell">
                  <div class="cell-content">
                    <button type="button" (click)="settle(debt.debtId, debt.comment)">Marquer comme reglee</button>
                  </div>
                </td>
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
    .hero-actions{margin-top:.7rem;display:flex;justify-content:flex-start;align-items:center;flex-wrap:wrap}
    .debt-filters{grid-template-columns:max-content}
    .debt-filters select{width:auto;min-width:8.5rem;max-width:11rem;border-radius:999px;background:#fff}
    .eyebrow{margin:0;color:#9a3412;text-transform:uppercase;letter-spacing:.14em;font-size:.72rem}
    .lede{margin:0;color:#475569;max-width:52rem}
    .panel{background:#fff;padding:1.2rem;border-radius:1.2rem;box-shadow:0 18px 40px rgba(15,23,42,.08)}
    .panel-header{display:flex;justify-content:space-between;gap:1rem;align-items:start}
    .panel-header h3,.panel-header p{margin:0}
    .debt-total{color:#b91c1c}
    .table{width:100%;border-collapse:collapse;table-layout:fixed}
    .table tbody tr{height:3rem}
    .table th,.table td{padding:.24rem .52rem;border-bottom:1px solid #e2e8f0;text-align:left;vertical-align:middle;line-height:1.1}
    .cell-content{display:flex;align-items:center;min-height:1.85rem}
    .debt-label{gap:.45rem;color:#b91c1c;font-weight:700}
    .debt-label span:last-child{display:block;line-height:1.2}
    .debt-amount{color:#b91c1c;font-weight:700;white-space:nowrap}
    .comment-cell .cell-content,.settle-cell .cell-content{justify-content:flex-start}
    .comment-cell input{width:100%;max-width:13rem;border:1px solid #cbd5e1;border-radius:999px;padding:.34rem .68rem;font:inherit;background:#fff;box-sizing:border-box;min-height:1.8rem}
    .settle-cell button{padding:.46rem .72rem !important;font-size:.8rem}
    .money-alert-icon{position:relative;display:inline-block;width:1.05rem;height:.95rem;flex:0 0 auto}
    .money-alert-icon .bill{position:absolute;border-radius:.2rem;background:linear-gradient(180deg,#fecaca,#fca5a5);border:1px solid #b91c1c;box-shadow:inset 0 0 0 1px rgba(255,255,255,.24)}
    .money-alert-icon .bill::after{content:'';position:absolute;inset:.18rem .22rem;border:1px solid rgba(127,29,29,.55);border-radius:.16rem}
    .money-alert-icon .bill.back{width:.68rem;height:.48rem;top:.24rem;left:.06rem;opacity:.78}
    .money-alert-icon .bill.front{width:.76rem;height:.52rem;top:.06rem;left:.22rem}
    .money-alert-icon .slash{position:absolute;width:.14rem;height:1.02rem;background:#991b1b;border-radius:999px;transform:rotate(38deg);top:-.04rem;left:.46rem;box-shadow:0 0 0 1px rgba(255,255,255,.2)}
    .empty{margin:0;color:#64748b}
    .error{margin:0;color:#991b1b;font-weight:700}
    @media(max-width:1000px){
      .debt-filters{grid-template-columns:1fr}
      .table thead{display:none}
      .table,.table tbody,.table tr,.table td{display:block;width:100%}
      .table tr{padding:.35rem 0}
      .table td{border-bottom:0;padding:.28rem 0}
      .comment-cell input{max-width:none}
    }
  `,
})
export class DebtsPageComponent {
  private readonly api = inject(DebtsApiService);

  readonly debts = signal<CustomerDebtSummary[]>([]);
  readonly comments = signal<Record<string, string>>({});
  readonly error = signal('');
  readonly showFilters = signal(false);
  readonly currentPage = signal(1);
  readonly pageSize = signal(10);
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.debts().length / this.pageSize())));
  readonly pagedDebts = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize();
    return this.debts().slice(start, start + this.pageSize());
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.api.list().subscribe({
      next: (debts) => {
        this.debts.set(debts);
        this.currentPage.set(1);
        this.comments.set(
          debts.flatMap((customer) => customer.debts).reduce<Record<string, string>>((map, debt) => {
            map[debt.debtId] = debt.comment ?? '';
            return map;
          }, {}),
        );
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Chargement des dettes impossible'),
    });
  }

  settle(debtId: string, fallbackComment: string): void {
    this.api.settle(debtId, this.commentValue(debtId, fallbackComment)).subscribe({
      next: () => this.load(),
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Reglement impossible'),
    });
  }

  updateComment(debtId: string, comment: string): void {
    this.comments.update((current) => ({ ...current, [debtId]: comment }));
  }

  commentValue(debtId: string, fallbackComment: string): string {
    return this.comments()[debtId] ?? fallbackComment ?? '';
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

  customerTypeLabel(type: string): string {
    return type === 'SUBSCRIBER' || type.toLowerCase().includes('abonn') ? 'Abonne' : 'Client';
  }

  customerTypeChipClass(type: string): string {
    return this.customerTypeLabel(type) === 'Abonne' ? 'type-chip subscriber-chip' : 'type-chip walk-in-chip';
  }
}
