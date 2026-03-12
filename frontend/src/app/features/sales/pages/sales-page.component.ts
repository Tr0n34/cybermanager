import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, map } from 'rxjs';

import type { Customer } from '../../customers/models/customer.models';
import { CustomersApiService } from '../../customers/services/customers-api.service';
import type { Product } from '../../products/models/product.models';
import { ProductsApiService } from '../../products/services/products-api.service';
import type { SubscriptionOffer } from '../../subscriptions/models/subscription-offer.models';
import { SubscriptionOffersApiService } from '../../subscriptions/services/subscription-offers-api.service';
import type { ConnectionPricingTier, Sale } from '../models/sales.models';
import { SalesApiService } from '../services/sales-api.service';

type SalesPanelMode = 'product' | 'subscription' | 'connection';

@Component({
  selector: 'app-sales-page',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="page">
      <header class="hero">
        <div>
          <p class="eyebrow">Flux ventes</p>
          <h2>Ventes et tarification</h2>
          <p class="lede">Selectionne un client ou un abonne par autocompletion, puis enregistre une vente produit, abonnement ou temps.</p>
        </div>

        <div class="hero-actions">
          <a routerLink="/sales/pricing" class="link-button secondary">Configurer les tarifs</a>
          <button *ngIf="!isPanelOpen()" type="button" class="secondary" (click)="openPanel('product')">Nouvelle vente</button>
        </div>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>

      <div class="grid" [class.panel-open]="isPanelOpen()">
        <article class="panel list-panel">
          <div class="panel-header">
            <div>
              <h3>Ventes du jour</h3>
              <p>{{ sales().length }} vente(s)</p>
            </div>
          </div>

          <table class="table">
            <thead>
              <tr>
                <th>Heure</th>
                <th>Type</th>
                <th>Articles</th>
                <th>Montant</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let sale of sales()">
                <td>{{ sale.soldAt | date:'dd/MM/yyyy HH:mm' }}</td>
                <td>{{ sale.type }}</td>
                <td class="muted">{{ sale.lines[0]?.label ?? 'Vente' }}</td>
                <td>{{ sale.totalAmount | number:'1.2-2' }} EUR</td>
              </tr>
              <tr *ngIf="sales().length === 0">
                <td colspan="4" class="empty">Aucune vente enregistree aujourd'hui.</td>
              </tr>
            </tbody>
          </table>

          <section class="pricing-summary">
            <div class="pricing-header">
              <div>
                <h3>Tarifs de connexion</h3>
                <p>La grille active s'applique aux ventes de temps et aux sessions journalieres.</p>
              </div>
            </div>

            <ul class="pricing-list">
              <li *ngFor="let tier of pricingTiers()">
                <strong>{{ formatDuration(tier) }}</strong>
                <span>{{ tier.price | number:'1.2-2' }} EUR</span>
              </li>
            </ul>
          </section>
        </article>

        <article class="panel side-panel stack" [class.open]="isPanelOpen()" [attr.aria-hidden]="!isPanelOpen()">
          <div class="side-panel-header">
            <div>
              <h3>{{ panelTitle() }}</h3>
              <p>Recherche du client ou de l'abonne par nom, sans liste exhaustive.</p>
            </div>
            <button type="button" class="icon-button" (click)="closePanel()">Fermer</button>
          </div>

          <div class="mode-switches">
            <button type="button" [class.secondary]="panelMode() !== 'product'" (click)="openPanel('product')">Produit</button>
            <button type="button" [class.secondary]="panelMode() !== 'subscription'" (click)="openPanel('subscription')">Abonnement</button>
            <button type="button" [class.secondary]="panelMode() !== 'connection'" (click)="openPanel('connection')">Temps</button>
          </div>

          <label class="field">
            <span>Client ou abonne</span>
            <input [formControl]="customerSearchForm.controls.term" placeholder="Saisir le nom du client" />
          </label>

          <div class="selected-customer" *ngIf="selectedCustomer() as customer">
            <strong>{{ customer.name }}</strong>
            <span>{{ customer.type === 'SUBSCRIBER' ? 'Abonne' : 'Client' }}</span>
            <button type="button" class="ghost" (click)="clearSelectedCustomer()">Changer</button>
          </div>

          <div class="suggestions" *ngIf="customerSuggestions().length > 0 && !selectedCustomer()">
            <button type="button" class="suggestion" *ngFor="let customer of customerSuggestions()" (click)="selectCustomer(customer)">
              <strong>{{ customer.name }}</strong>
              <span>{{ customer.type === 'SUBSCRIBER' ? 'Abonne' : 'Client' }}</span>
            </button>
          </div>

          <form class="stack" [formGroup]="productSaleForm" (ngSubmit)="sellProduct()" *ngIf="panelMode() === 'product'">
            <label class="field">
              <span>Produit</span>
              <select formControlName="productId">
                <option value="">Choisir un produit</option>
                <option *ngFor="let item of products()" [value]="item.productId">{{ item.name }} - {{ item.price | number:'1.2-2' }} EUR</option>
              </select>
            </label>
            <label class="field">
              <span>Quantite</span>
              <input type="number" min="1" formControlName="quantity" />
            </label>
            <label class="checkbox">
              <input type="checkbox" formControlName="createDebt" />
              <span>Creer une dette au lieu d'encaisser</span>
            </label>
            <button type="submit" [disabled]="!selectedCustomer() || productSaleForm.invalid">Enregistrer la vente</button>
          </form>

          <form class="stack" [formGroup]="subscriptionSaleForm" (ngSubmit)="sellSubscription()" *ngIf="panelMode() === 'subscription'">
            <label class="field">
              <span>Offre d'abonnement</span>
              <select formControlName="subscriptionOfferId">
                <option value="">Choisir une offre</option>
                <option *ngFor="let item of offers()" [value]="item.offerId">{{ item.name }} - {{ item.includedMinutes }} min</option>
              </select>
            </label>
            <p class="helper">Les abonnements se cumulent sur le credit existant.</p>
            <label class="checkbox">
              <input type="checkbox" formControlName="createDebt" />
              <span>Creer une dette au lieu d'encaisser</span>
            </label>
            <button type="submit" [disabled]="!selectedCustomer() || subscriptionSaleForm.invalid">Enregistrer la vente</button>
          </form>

          <form class="stack" [formGroup]="connectionSaleForm" (ngSubmit)="sellConnection()" *ngIf="panelMode() === 'connection'">
            <label class="field">
              <span>Duree en minutes</span>
              <input type="number" min="1" formControlName="minutes" />
            </label>
            <label class="checkbox">
              <input type="checkbox" formControlName="createDebt" />
              <span>Creer une dette au lieu d'encaisser</span>
            </label>
            <button type="submit" [disabled]="!selectedCustomer() || connectionSaleForm.invalid">Enregistrer la vente</button>
          </form>
        </article>
      </div>
    </section>
  `,
  styles: `
    .page, .stack { display: grid; gap: 1rem; }
    .hero { display: flex; justify-content: space-between; gap: 1rem; align-items: start; flex-wrap: wrap; }
    .hero h2 { margin: 0.35rem 0 0.5rem; }
    .eyebrow { margin: 0; text-transform: uppercase; letter-spacing: 0.16em; font-size: 0.72rem; color: #9a3412; }
    .lede { margin: 0; color: #475569; max-width: 52rem; }
    .hero-actions { display: flex; gap: 0.75rem; flex-wrap: wrap; }
    .grid { display: grid; grid-template-columns: minmax(0, 1fr) 0fr; gap: 1rem; align-items: start; transition: grid-template-columns 220ms ease-out; }
    .grid.panel-open { grid-template-columns: minmax(0, 1.35fr) minmax(24rem, 0.95fr); }
    .panel { background: #fff; padding: 1.25rem; border-radius: 1.25rem; box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08); }
    .list-panel { overflow: hidden; }
    .panel-header, .side-panel-header { display: flex; justify-content: space-between; gap: 1rem; align-items: start; }
    .panel-header h3, .side-panel-header h3 { margin: 0; }
    .panel-header p, .side-panel-header p { margin: 0.2rem 0 0; color: #475569; }
    .table { width: 100%; border-collapse: collapse; table-layout: fixed; }
    .table th, .table td { padding: 0.82rem 0.75rem; border-bottom: 1px solid #e2e8f0; text-align: left; }
    .table th { font-size: 0.78rem; letter-spacing: 0.08em; text-transform: uppercase; color: #64748b; }
    .muted { color: #64748b; }
    .empty { text-align: center; color: #64748b; }
    .pricing-summary { display: grid; gap: 0.75rem; padding: 1rem; border-radius: 1rem; background: #f8fafc; }
    .pricing-header { display: flex; justify-content: space-between; gap: 1rem; align-items: start; }
    .pricing-header p { margin: 0.25rem 0 0; color: #475569; }
    .pricing-list { list-style: none; padding: 0; margin: 0; display: grid; gap: 0.5rem; }
    .pricing-list li { display: flex; justify-content: space-between; gap: 1rem; padding: 0.75rem 1rem; background: #fff; border-radius: 0.85rem; border: 1px solid #e2e8f0; }
    .mode-switches { display: flex; gap: 0.75rem; flex-wrap: wrap; }
    .field { display: grid; gap: 0.45rem; }
    .field span { font-size: 0.86rem; font-weight: 700; color: #334155; }
    .helper { margin: 0; color: #64748b; font-size: 0.9rem; }
    .checkbox { display: flex; gap: 0.65rem; align-items: center; color: #334155; }
    input, select { padding: 0.85rem 0.95rem; border-radius: 0.85rem; border: 1px solid #cbd5e1; font: inherit; background: #fff; }
    .suggestions { display: grid; gap: 0.5rem; }
    .suggestion { display: flex; justify-content: space-between; align-items: center; padding: 0.9rem 1rem; border-radius: 0.9rem; background: #fff7ed; color: #111827; border: 1px solid #fed7aa; }
    .suggestion span { color: #9a3412; }
    .selected-customer { display: flex; justify-content: space-between; gap: 1rem; align-items: center; padding: 0.95rem 1rem; border-radius: 1rem; background: #eff6ff; border: 1px solid #bfdbfe; }
    .selected-customer span { color: #1d4ed8; font-weight: 600; }
    .side-panel {
      overflow: hidden;
      opacity: 0;
      transform: translateX(14px) scale(0.985);
      transform-origin: right center;
      pointer-events: none;
      max-width: 0;
      padding-inline: 0;
      transition:
        opacity 170ms ease-out,
        transform 220ms ease-out,
        max-width 220ms ease-out,
        padding-inline 220ms ease-out;
    }
    .side-panel.open {
      opacity: 1;
      transform: translateX(0) scale(1);
      pointer-events: auto;
      max-width: 100%;
      padding-inline: 1.25rem;
    }
    .error { margin: 0; color: #991b1b; font-weight: 700; }
    @media (max-width: 1100px) {
      .grid, .grid.panel-open { grid-template-columns: 1fr; }
      .side-panel, .side-panel.open { max-width: none; padding-inline: 1.25rem; opacity: 1; transform: none; }
      .side-panel:not(.open) { display: none; }
    }
  `,
})
export class SalesPageComponent {
  private readonly customersApi = inject(CustomersApiService);
  private readonly productsApi = inject(ProductsApiService);
  private readonly offersApi = inject(SubscriptionOffersApiService);
  private readonly salesApi = inject(SalesApiService);
  private readonly fb = inject(FormBuilder);

  readonly products = signal<Product[]>([]);
  readonly offers = signal<SubscriptionOffer[]>([]);
  readonly sales = signal<Sale[]>([]);
  readonly pricingTiers = signal<ConnectionPricingTier[]>([]);
  readonly customerSuggestions = signal<Customer[]>([]);
  readonly selectedCustomer = signal<Customer | null>(null);
  readonly panelMode = signal<SalesPanelMode>('product');
  readonly isPanelOpen = signal(false);
  readonly error = signal('');

  readonly customerSearchForm = this.fb.nonNullable.group({ term: [''] });
  readonly productSaleForm = this.fb.nonNullable.group({ productId: ['', Validators.required], quantity: [1, [Validators.required, Validators.min(1)]], createDebt: [false] });
  readonly subscriptionSaleForm = this.fb.nonNullable.group({ subscriptionOfferId: ['', Validators.required], createDebt: [false] });
  readonly connectionSaleForm = this.fb.nonNullable.group({ minutes: [60, [Validators.required, Validators.min(1)]], createDebt: [false] });

  constructor() {
    this.productsApi.search('', 'ACTIVE', '').subscribe((value) => this.products.set(value));
    this.offersApi.search('', 'ACTIVE').subscribe((value) => this.offers.set(value));
    this.reload();
    this.salesApi.pricing().subscribe((value) => this.pricingTiers.set(value.tiers));

    this.customerSearchForm.controls.term.valueChanges.pipe(
      map((value) => value.trim()),
      debounceTime(150),
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe((term) => this.autocompleteCustomers(term));
  }

  panelTitle(): string {
    if (this.panelMode() === 'subscription') {
      return "Vente d'abonnement";
    }
    if (this.panelMode() === 'connection') {
      return 'Vente de temps';
    }
    return 'Vente de produit';
  }

  openPanel(mode: SalesPanelMode): void {
    this.panelMode.set(mode);
    this.isPanelOpen.set(true);
    this.error.set('');
  }

  closePanel(): void {
    this.isPanelOpen.set(false);
    this.clearSelectedCustomer();
    this.resetForms();
    this.error.set('');
  }

  clearSelectedCustomer(): void {
    this.selectedCustomer.set(null);
    this.customerSearchForm.reset({ term: '' });
    this.customerSuggestions.set([]);
  }

  selectCustomer(customer: Customer): void {
    this.selectedCustomer.set(customer);
    this.customerSearchForm.patchValue({ term: customer.name }, { emitEvent: false });
    this.customerSuggestions.set([]);
  }

  autocompleteCustomers(term: string): void {
    if (term.length < 2) {
      if (!this.selectedCustomer()) {
        this.customerSuggestions.set([]);
      }
      return;
    }
    this.customersApi.search(term).subscribe({
      next: (customers) => {
        this.customerSuggestions.set(customers);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Recherche des clients impossible'),
    });
  }

  reload(): void {
    this.salesApi.day().subscribe((value) => this.sales.set(value));
  }

  sellProduct(): void {
    const customer = this.selectedCustomer();
    const { productId, quantity, createDebt } = this.productSaleForm.getRawValue();
    if (!customer) {
      this.error.set('Selectionne un client ou un abonne.');
      return;
    }
    this.salesApi.productSale({ customerId: customer.customerId, lines: [{ productId, quantity }], createDebt }).subscribe({
      next: () => {
        this.closePanel();
        this.reload();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Vente produit impossible'),
    });
  }

  sellSubscription(): void {
    const customer = this.selectedCustomer();
    const { subscriptionOfferId, createDebt } = this.subscriptionSaleForm.getRawValue();
    if (!customer) {
      this.error.set('Selectionne un client ou un abonne.');
      return;
    }
    this.salesApi.subscriptionSale({ customerId: customer.customerId, subscriptionOfferId, createDebt }).subscribe({
      next: () => {
        this.closePanel();
        this.reload();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Vente abonnement impossible'),
    });
  }

  sellConnection(): void {
    const customer = this.selectedCustomer();
    const { minutes, createDebt } = this.connectionSaleForm.getRawValue();
    if (!customer) {
      this.error.set('Selectionne un client ou un abonne.');
      return;
    }
    this.salesApi.connectionSale({ customerId: customer.customerId, minutes, createDebt }).subscribe({
      next: () => {
        this.closePanel();
        this.reload();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Vente de temps impossible'),
    });
  }

  formatDuration(tier: ConnectionPricingTier): string {
    const parts: string[] = [];
    if (tier.hours > 0) {
      parts.push(`${tier.hours} h`);
    }
    if (tier.minutes > 0) {
      parts.push(`${tier.minutes} min`);
    }
    return parts.join(' ') || `${tier.durationMinutes} min`;
  }

  private resetForms(): void {
    this.productSaleForm.reset({ productId: '', quantity: 1, createDebt: false });
    this.subscriptionSaleForm.reset({ subscriptionOfferId: '', createDebt: false });
    this.connectionSaleForm.reset({ minutes: 60, createDebt: false });
  }
}
