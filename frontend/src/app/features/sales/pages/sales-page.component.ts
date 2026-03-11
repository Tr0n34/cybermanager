import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import type { Customer } from '../../customers/models/customer.models';
import { CustomersApiService } from '../../customers/services/customers-api.service';
import type { Product } from '../../products/models/product.models';
import { ProductsApiService } from '../../products/services/products-api.service';
import type { SubscriptionOffer } from '../../subscriptions/models/subscription-offer.models';
import { SubscriptionOffersApiService } from '../../subscriptions/services/subscription-offers-api.service';
import type { ConnectionPricingTier, Sale } from '../models/sales.models';
import { SalesApiService } from '../services/sales-api.service';

@Component({
  selector: 'app-sales-page',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="page">
      <h2>Ventes et tarification</h2>
      <div class="grid">
        <div class="panel form">
          <h3>Vendre un produit</h3>
          <form [formGroup]="productSaleForm" (ngSubmit)="sellProduct()">
            <select formControlName="customerId">
              <option value="">Client</option>
              <option *ngFor="let item of customers()" [value]="item.customerId">{{ item.name }}</option>
            </select>
            <select formControlName="productId">
              <option value="">Produit</option>
              <option *ngFor="let item of products()" [value]="item.productId">{{ item.name }}</option>
            </select>
            <input type="number" formControlName="quantity" placeholder="Quantite" />
            <button type="submit">Enregistrer</button>
          </form>

          <h3>Vendre un abonnement</h3>
          <form [formGroup]="subscriptionSaleForm" (ngSubmit)="sellSubscription()">
            <select formControlName="customerId">
              <option value="">Client</option>
              <option *ngFor="let item of customers()" [value]="item.customerId">{{ item.name }}</option>
            </select>
            <select formControlName="subscriptionOfferId">
              <option value="">Offre</option>
              <option *ngFor="let item of offers()" [value]="item.offerId">{{ item.name }}</option>
            </select>
            <button type="submit">Vendre</button>
          </form>

          <h3>Vendre du temps</h3>
          <form [formGroup]="connectionSaleForm" (ngSubmit)="sellConnection()">
            <select formControlName="customerId">
              <option value="">Client</option>
              <option *ngFor="let item of customers()" [value]="item.customerId">{{ item.name }}</option>
            </select>
            <input type="number" formControlName="minutes" placeholder="Minutes" />
            <button type="submit">Vendre</button>
          </form>

          <section class="pricing-summary">
            <div class="pricing-header">
              <div>
                <h3>Tarifs de connexion</h3>
                <p>La grille active s'applique aux ventes de temps et aux sessions journalieres.</p>
              </div>
              <a routerLink="/sales/pricing" class="link-button">Configurer</a>
            </div>

            <ul class="pricing-list">
              <li *ngFor="let tier of pricingTiers()">
                <strong>{{ formatDuration(tier) }}</strong>
                <span>{{ tier.price | number:'1.2-2' }} EUR</span>
              </li>
            </ul>
          </section>
        </div>

        <div class="panel">
          <h3>Ventes du jour</h3>
          <table class="table">
            <tr><th>Heure</th><th>Type</th><th>Montant</th></tr>
            <tr *ngFor="let sale of sales()">
              <td>{{ sale.soldAt | date:'shortTime' }}</td>
              <td>{{ sale.type }}</td>
              <td>{{ sale.totalAmount | number:'1.2-2' }} EUR</td>
            </tr>
          </table>
        </div>
      </div>
    </section>
  `,
  styles: `
    .page,.form{display:grid;gap:1rem}
    .grid{display:grid;grid-template-columns:1.1fr .9fr;gap:1rem}
    .panel{background:#fff;padding:1rem;border-radius:1rem}
    .table{width:100%;border-collapse:collapse}
    .table th,.table td{padding:.75rem;border-bottom:1px solid #e2e8f0;text-align:left}
    form{display:grid;gap:.75rem}
    input,select,button,.link-button{padding:.75rem;border-radius:.75rem;border:1px solid #cbd5e1}
    button,.link-button{background:#14213d;color:#fff;border:0;text-decoration:none;text-align:center;font-weight:600}
    .pricing-summary{display:grid;gap:.75rem;padding:1rem;border-radius:1rem;background:#f8fafc}
    .pricing-header{display:flex;justify-content:space-between;gap:1rem;align-items:start}
    .pricing-header p{margin:.25rem 0 0;color:#475569}
    .pricing-list{list-style:none;padding:0;margin:0;display:grid;gap:.5rem}
    .pricing-list li{display:flex;justify-content:space-between;gap:1rem;padding:.75rem 1rem;background:#fff;border-radius:.85rem;border:1px solid #e2e8f0}
    .link-button{display:inline-flex;align-items:center;justify-content:center}
    @media(max-width:1000px){.grid{grid-template-columns:1fr}.pricing-header{flex-direction:column}}
  `,
})
export class SalesPageComponent {
  private readonly customersApi = inject(CustomersApiService);
  private readonly productsApi = inject(ProductsApiService);
  private readonly offersApi = inject(SubscriptionOffersApiService);
  private readonly salesApi = inject(SalesApiService);
  private readonly fb = inject(FormBuilder);

  readonly customers = signal<Customer[]>([]);
  readonly products = signal<Product[]>([]);
  readonly offers = signal<SubscriptionOffer[]>([]);
  readonly sales = signal<Sale[]>([]);
  readonly pricingTiers = signal<ConnectionPricingTier[]>([]);
  readonly productSaleForm = this.fb.nonNullable.group({ customerId: [''], productId: [''], quantity: [1] });
  readonly subscriptionSaleForm = this.fb.nonNullable.group({ customerId: [''], subscriptionOfferId: [''] });
  readonly connectionSaleForm = this.fb.nonNullable.group({ customerId: [''], minutes: [60] });

  constructor() {
    this.customersApi.search().subscribe((value) => this.customers.set(value));
    this.productsApi.search('', 'ACTIVE', '').subscribe((value) => this.products.set(value));
    this.offersApi.search('', 'ACTIVE').subscribe((value) => this.offers.set(value));
    this.reload();
    this.salesApi.pricing().subscribe((value) => this.pricingTiers.set(value.tiers));
  }

  reload(): void {
    this.salesApi.day().subscribe((value) => this.sales.set(value));
  }

  sellProduct(): void {
    const { customerId, productId, quantity } = this.productSaleForm.getRawValue();
    this.salesApi.productSale({ customerId, lines: [{ productId, quantity }] }).subscribe(() => this.reload());
  }

  sellSubscription(): void {
    this.salesApi.subscriptionSale(this.subscriptionSaleForm.getRawValue()).subscribe(() => this.reload());
  }

  sellConnection(): void {
    this.salesApi.connectionSale(this.connectionSaleForm.getRawValue()).subscribe(() => this.reload());
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
}
