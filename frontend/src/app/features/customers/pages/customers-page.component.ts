import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import type { Customer, CustomerDetails } from '../models/customer.models';
import { CustomersApiService } from '../services/customers-api.service';
import type { Product } from '../../products/models/product.models';
import { ProductsApiService } from '../../products/services/products-api.service';
import { SalesApiService } from '../../sales/services/sales-api.service';
import type { SubscriptionOffer } from '../../subscriptions/models/subscription-offer.models';
import { SubscriptionOffersApiService } from '../../subscriptions/services/subscription-offers-api.service';

@Component({
  selector: 'app-customers-page',
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="page">
      <header class="hero">
        <div>
          <p class="eyebrow">Clientele</p>
          <h2>Clients</h2>
        </div>
        <p class="lede">Les clients journaliers et abonnes crees depuis l'ecran sessions apparaissent ici. Cette page permet aussi de creer des clients et des abonnes.</p>

        <div class="filters" [formGroup]="filters">
          <input formControlName="term" placeholder="Filtrer par nom" />

          <select formControlName="pageSize">
            <option *ngFor="let size of pageSizeOptions" [value]="size">{{ size }} / page</option>
          </select>

          <button *ngIf="!isPanelOpen()" type="button" class="secondary" (click)="openCreatePanel()">Nouveau client</button>
        </div>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>

      <div class="toolbar">
        <p class="summary">
          {{ filteredCustomers().length }} client{{ filteredCustomers().length > 1 ? 's' : '' }}
          <span *ngIf="filteredCustomers().length !== customers().length">sur {{ customers().length }}</span>
        </p>

        <div class="pager" *ngIf="totalPages() > 1">
          <button type="button" class="ghost" (click)="previousPage()" [disabled]="currentPage() === 1">Precedent</button>
          <span>Page {{ currentPage() }} / {{ totalPages() }}</span>
          <button type="button" class="ghost" (click)="nextPage()" [disabled]="currentPage() === totalPages()">Suivant</button>
        </div>
      </div>

      <div class="grid" [class.panel-open]="isPanelOpen()">
        <article class="panel list-panel">
          <div class="panel-header">
            <h3>Liste</h3>
          </div>

          <table class="table">
            <thead>
              <tr>
                <th>Nom</th>
                <th>Type</th>
                <th>Abonnement</th>
                <th>Credit</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of paginatedCustomers()" [class.active]="selected()?.customerId === item.customerId">
                <td><strong>{{ item.name }}</strong></td>
                <td>{{ item.type === 'SUBSCRIBER' ? 'Abonne' : 'Client' }}</td>
                <td>{{ item.type === 'SUBSCRIBER' ? 'Actif' : 'Aucun' }}</td>
                <td>{{ item.remainingMinutes }} min</td>
                <td><button type="button" (click)="open(item.customerId)">Ouvrir</button></td>
              </tr>

              <tr *ngIf="paginatedCustomers().length === 0">
                <td colspan="5" class="empty">Aucun client pour ce filtre.</td>
              </tr>
            </tbody>
          </table>
        </article>

        <article class="panel side-panel stack" [class.open]="isPanelOpen()" [attr.aria-hidden]="!isPanelOpen()">
          <div class="side-panel-header">
            <h3>{{ panelMode() === 'edit' ? 'Fiche client' : 'Creation client' }}</h3>
            <button type="button" class="icon-button" (click)="closePanel()">Fermer</button>
          </div>

          <section class="stack" *ngIf="panelMode() === 'create'; else editPanel">
            <form [formGroup]="createForm" (ngSubmit)="createCustomer()" class="stack">
              <label class="field">
                <span>Nom du client</span>
                <input formControlName="name" placeholder="Ex. Nadia Benali" />
              </label>
              <label class="field">
                <span>Type de client</span>
                <select formControlName="type">
                  <option value="WALK_IN">Client</option>
                  <option value="SUBSCRIBER">Abonne</option>
                </select>
              </label>
              <label class="field" *ngIf="createForm.controls.type.value === 'SUBSCRIBER'">
                <span>Offre d'abonnement initiale</span>
                <select formControlName="subscriptionOfferId">
                  <option value="">Choisir une offre</option>
                  <option *ngFor="let offer of offers()" [value]="offer.offerId">{{ offer.name }} - {{ offer.includedMinutes }} min</option>
                </select>
              </label>
              <button type="submit" [disabled]="createForm.invalid">Creer</button>
            </form>
          </section>

          <ng-template #editPanel>
            <section class="stack" *ngIf="selected() as customer">
              <form [formGroup]="editForm" (ngSubmit)="save(customer.customerId)" class="stack">
                <label class="field">
                  <span>Nom du client</span>
                  <input formControlName="name" />
                </label>
                <div class="facts">
                  <p><strong>Type :</strong> {{ customer.type === 'SUBSCRIBER' ? 'Abonne' : 'Client' }}</p>
                  <p><strong>Abonnements :</strong> {{ customer.currentSubscriptionLabel ?? 'Aucun' }}</p>
                  <p *ngIf="customer.type === 'SUBSCRIBER'" class="muted">Les abonnements se cumulent.</p>
                  <p><strong>Credit disponible :</strong> {{ customer.remainingMinutes }} min</p>
                  <p><strong>Total paye sur tous les achats :</strong> {{ totalPaidPurchases(customer) | number:'1.2-2' }} EUR</p>
                  <p><strong>Total paye sur forfaits et produits :</strong> {{ totalPaidSales(customer) | number:'1.2-2' }} EUR</p>
                  <p *ngIf="customer.debts.length > 0" class="debt-summary"><strong>Dettes :</strong> <span class="money-alert-icon debt-indicator" title="Dettes ouvertes"><span class="bill back"></span><span class="bill front"></span><span class="slash"></span></span></p>
                </div>
                <div class="actions">
                  <button type="submit" [disabled]="editForm.invalid">Enregistrer</button>
                </div>
              </form>

              <div class="split-actions">
                <section class="stack action-block">
                  <h4>Ajouter un abonnement</h4>
                  <form [formGroup]="subscriptionSaleForm" (ngSubmit)="sellSubscription(customer.customerId)" class="stack">
                    <label class="field">
                      <span>Offre</span>
                      <select formControlName="subscriptionOfferId">
                        <option value="">Choisir une offre</option>
                        <option *ngFor="let offer of offers()" [value]="offer.offerId">{{ offer.name }} - {{ offer.includedMinutes }} min</option>
                      </select>
                    </label>
                    <label class="checkbox">
                      <input type="checkbox" formControlName="createDebt" />
                      <span>Creer une dette au lieu d'encaisser</span>
                    </label>
                    <button type="submit" [disabled]="subscriptionSaleForm.invalid">Vendre l'abonnement</button>
                  </form>
                </section>

                <section class="stack action-block">
                  <h4>Vendre un produit</h4>
                  <form [formGroup]="productSaleForm" (ngSubmit)="sellProduct(customer.customerId)" class="stack">
                    <label class="field">
                      <span>Produit</span>
                      <select formControlName="productId">
                        <option value="">Choisir un produit</option>
                        <option *ngFor="let product of products()" [value]="product.productId">{{ product.name }} - {{ product.price | number:'1.2-2' }} EUR</option>
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
                    <button type="submit" [disabled]="productSaleForm.invalid">Vendre le produit</button>
                  </form>
                </section>
              </div>

              <div class="stack">
                <h4>Achats du client</h4>
                <table class="table compact" *ngIf="customer.purchases.length > 0; else emptyPurchases">
                  <thead>
                    <tr><th>Quand</th><th>Type</th><th>Detail</th><th>Total</th></tr>
                  </thead>
                  <tbody>
                    <tr *ngFor="let purchase of customer.purchases">
                      <td>{{ purchase.soldAt | date:'dd/MM/yyyy HH:mm' }}</td>
                      <td>{{ purchase.type }}</td>
                      <td>{{ purchase.label }}</td>
                      <td>{{ purchase.totalAmount | number:'1.2-2' }} EUR</td>
                    </tr>
                  </tbody>
                  <tfoot>
                    <tr>
                      <td colspan="3"><strong>Total paye</strong></td>
                      <td><strong>{{ totalPaidPurchases(customer) | number:'1.2-2' }} EUR</strong></td>
                    </tr>
                  </tfoot>
                </table>
                <ng-template #emptyPurchases><p class="muted">Aucun achat enregistre.</p></ng-template>
              </div>
            </section>
          </ng-template>
        </article>
      </div>
    </section>
  `,
  styles: `
    .page, .stack { display: grid; gap: 1rem; }
    .hero { display: grid; gap: 0.8rem; }
    .eyebrow { margin: 0; color: #9a3412; text-transform: uppercase; letter-spacing: 0.12em; font-size: 0.72rem; }
    .lede { margin: 0; max-width: 56rem; color: #475569; }
    .filters { display: flex; gap: 0.75rem; flex-wrap: wrap; align-items: center; }
    input, select { padding: 0.85rem 0.95rem; border-radius: 0.85rem; border: 1px solid #cbd5e1; font: inherit; background: #fff; }
    .toolbar { display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap; }
    .summary { margin: 0; color: #334155; font-weight: 600; }
    .pager { display: inline-flex; align-items: center; gap: 0.75rem; color: #475569; }
    .grid { display: grid; grid-template-columns: minmax(0, 1fr) 0fr; gap: 1.25rem; align-items: start; transition: grid-template-columns 220ms ease-out; }
    .grid.panel-open { grid-template-columns: minmax(0, 1.2fr) minmax(24rem, 1fr); }
    .panel { background: linear-gradient(180deg, #fff, #fff7ed); padding: 1.25rem; border-radius: 1.25rem; box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08); }
    .list-panel { overflow: hidden; }
    .panel-header, .side-panel-header { display: flex; justify-content: space-between; gap: 1rem; align-items: center; }
    .panel-header h3, .side-panel-header h3 { margin: 0; }
    .field { display: grid; gap: 0.45rem; }
    .field span { font-size: 0.86rem; font-weight: 700; color: #334155; }
    .table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 1rem; overflow: hidden; table-layout: fixed; }
    .table th, .table td { padding: 0.78rem 0.8rem; border-bottom: 1px solid #e2e8f0; text-align: left; vertical-align: top; }
    .table th { font-size: 0.78rem; letter-spacing: 0.08em; text-transform: uppercase; color: #64748b; }
    .compact th, .compact td { padding: 0.6rem; }
    tbody tr { transition: background 160ms ease; }
    tbody tr:hover, tbody tr.active { background: #fff7ed; }
    .facts { display: grid; gap: 0.35rem; padding: 0.9rem 1rem; background: #fff; border-radius: 1rem; border: 1px solid #fed7aa; }
    .facts p { margin: 0; }
    .split-actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.85rem; }
    .action-block { padding: 0.9rem 1rem; background: #fff; border-radius: 1rem; border: 1px solid #e2e8f0; }
    .action-block h4 { margin: 0; }
    .debt-summary { display: flex; gap: 0.5rem; align-items: center; }
    .money-alert-icon { position: relative; display: inline-block; width: 1.3rem; height: 1.1rem; }
    .money-alert-icon .bill { position: absolute; border-radius: 0.2rem; background: linear-gradient(180deg, #fecaca, #fca5a5); border: 1px solid #b91c1c; box-shadow: inset 0 0 0 1px rgba(255,255,255,.24); }
    .money-alert-icon .bill::after { content: ''; position: absolute; inset: 0.22rem 0.28rem; border: 1px solid rgba(127,29,29,.55); border-radius: 0.16rem; }
    .money-alert-icon .bill.back { width: 0.82rem; height: 0.56rem; top: 0.26rem; left: 0.1rem; opacity: 0.78; }
    .money-alert-icon .bill.front { width: 0.88rem; height: 0.6rem; top: 0.08rem; left: 0.28rem; }
    .money-alert-icon .slash { position: absolute; width: 0.16rem; height: 1.18rem; background: #991b1b; border-radius: 999px; transform: rotate(38deg); top: -0.04rem; left: 0.56rem; box-shadow: 0 0 0 1px rgba(255,255,255,.2); }
    .actions { display: flex; gap: 0.75rem; flex-wrap: wrap; }
    .muted { margin: 0; color: #64748b; }
    .empty { text-align: center; color: #64748b; padding: 1rem; }
    .error { margin: 0; color: #991b1b; font-weight: 700; }
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

    @media (max-width: 1100px) {
      .grid, .grid.panel-open { grid-template-columns: 1fr; }
      .split-actions { grid-template-columns: 1fr; }
      .side-panel, .side-panel.open { max-width: none; padding-inline: 1.25rem; opacity: 1; transform: none; }
      .side-panel:not(.open) { display: none; }
    }
  `,
})
export class CustomersPageComponent {
  private readonly api = inject(CustomersApiService);
  private readonly productsApi = inject(ProductsApiService);
  private readonly salesApi = inject(SalesApiService);
  private readonly offersApi = inject(SubscriptionOffersApiService);
  private readonly fb = inject(FormBuilder);

  readonly pageSizeOptions = [5, 10, 20, 50];
  readonly customers = signal<Customer[]>([]);
  readonly offers = signal<SubscriptionOffer[]>([]);
  readonly products = signal<Product[]>([]);
  readonly selected = signal<CustomerDetails | null>(null);
  readonly panelMode = signal<'create' | 'edit'>('create');
  readonly isPanelOpen = signal(false);
  readonly page = signal(1);
  readonly error = signal('');

  readonly filters = this.fb.nonNullable.group({ term: [''], pageSize: [10] });
  readonly filterState = signal(this.filters.getRawValue());
  readonly createForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    type: ['WALK_IN' as 'WALK_IN' | 'SUBSCRIBER'],
    subscriptionOfferId: [''],
  });
  readonly editForm = this.fb.nonNullable.group({ name: ['', Validators.required] });
  readonly subscriptionSaleForm = this.fb.nonNullable.group({
    subscriptionOfferId: ['', Validators.required],
    createDebt: [false],
  });
  readonly productSaleForm = this.fb.nonNullable.group({
    productId: ['', Validators.required],
    quantity: [1, [Validators.required, Validators.min(1)]],
    createDebt: [false],
  });

  readonly filteredCustomers = computed(() => {
    const term = this.filterState().term.trim().toLocaleLowerCase();
    return this.customers().filter((item) => term.length === 0 || item.name.toLocaleLowerCase().includes(term));
  });

  readonly totalPages = computed(() => {
    const pageSize = Number(this.filterState().pageSize) || 10;
    return Math.max(1, Math.ceil(this.filteredCustomers().length / pageSize));
  });

  readonly currentPage = computed(() => Math.min(this.page(), this.totalPages()));

  readonly paginatedCustomers = computed(() => {
    const pageSize = Number(this.filterState().pageSize) || 10;
    const start = (this.currentPage() - 1) * pageSize;
    return this.filteredCustomers().slice(start, start + pageSize);
  });

  constructor() {
    this.load();
    this.loadOffers();
    this.productsApi.search('', 'ACTIVE', '').subscribe((products) => this.products.set(products));

    this.filters.controls.term.valueChanges.pipe(
      debounceTime(150),
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe(() => {
      this.filterState.set({
        term: this.filters.controls.term.value ?? '',
        pageSize: Number(this.filters.controls.pageSize.value ?? 10),
      });
      this.page.set(1);
    });

    this.filters.controls.pageSize.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.filterState.set({
        term: this.filters.controls.term.value ?? '',
        pageSize: Number(this.filters.controls.pageSize.value ?? 10),
      });
      this.page.set(1);
    });
  }

  load(): void {
    this.api.search('').subscribe({
      next: (items) => {
        this.customers.set(items);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Chargement impossible'),
    });
  }

  loadOffers(): void {
    this.offersApi.search('', 'ACTIVE').subscribe({
      next: (offers) => this.offers.set(offers),
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Chargement des offres impossible'),
    });
  }

  openCreatePanel(): void {
    this.panelMode.set('create');
    this.selected.set(null);
    this.createForm.reset({ name: '', type: 'WALK_IN', subscriptionOfferId: '' });
    this.error.set('');
    this.isPanelOpen.set(true);
  }

  closePanel(): void {
    this.selected.set(null);
    this.isPanelOpen.set(false);
    this.editForm.reset({ name: '' });
    this.error.set('');
  }

  previousPage(): void {
    this.page.update((page) => Math.max(1, page - 1));
  }

  nextPage(): void {
    this.page.update((page) => Math.min(this.totalPages(), page + 1));
  }

  createCustomer(): void {
    const payload = this.createForm.getRawValue();
    if (!payload.name.trim()) {
      this.error.set('Le nom du client est requis');
      return;
    }
    if (payload.type === 'SUBSCRIBER' && !payload.subscriptionOfferId) {
      this.error.set('Une offre d abonnement est requise');
      return;
    }
    this.api.create({
      name: payload.name.trim(),
      type: payload.type,
      subscriptionOfferId: payload.type === 'SUBSCRIBER' ? payload.subscriptionOfferId : null,
    }).subscribe({
      next: () => {
        this.closePanel();
        this.load();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Creation impossible'),
    });
  }

  open(customerId: string): void {
    this.api.get(customerId).subscribe({
      next: (customer) => {
        this.selected.set(customer);
        this.panelMode.set('edit');
        this.editForm.patchValue({ name: customer.name });
        this.subscriptionSaleForm.reset({ subscriptionOfferId: '', createDebt: false });
        this.productSaleForm.reset({ productId: '', quantity: 1, createDebt: false });
        this.error.set('');
        this.isPanelOpen.set(true);
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Chargement de la fiche impossible'),
    });
  }

  save(customerId: string): void {
    const payload = this.editForm.getRawValue();
    this.api.update(customerId, payload).subscribe({
      next: () => {
        this.open(customerId);
        this.load();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Sauvegarde impossible'),
    });
  }

  sellSubscription(customerId: string): void {
    if (this.subscriptionSaleForm.invalid) {
      this.subscriptionSaleForm.markAllAsTouched();
      return;
    }
    const payload = this.subscriptionSaleForm.getRawValue();
    this.salesApi.subscriptionSale({
      customerId,
      subscriptionOfferId: payload.subscriptionOfferId,
      createDebt: payload.createDebt,
    }).subscribe({
      next: () => {
        this.subscriptionSaleForm.reset({ subscriptionOfferId: '', createDebt: false });
        this.open(customerId);
        this.load();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Vente d abonnement impossible'),
    });
  }

  sellProduct(customerId: string): void {
    if (this.productSaleForm.invalid) {
      this.productSaleForm.markAllAsTouched();
      return;
    }
    const payload = this.productSaleForm.getRawValue();
    this.salesApi.productSale({
      customerId,
      lines: [{ productId: payload.productId, quantity: payload.quantity }],
      createDebt: payload.createDebt,
    }).subscribe({
      next: () => {
        this.productSaleForm.reset({ productId: '', quantity: 1, createDebt: false });
        this.open(customerId);
        this.load();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Vente produit impossible'),
    });
  }

  totalPaidPurchases(customer: CustomerDetails): number {
    return customer.purchases
      .filter((purchase) => !purchase.openDebt)
      .reduce((total, purchase) => total + purchase.totalAmount, 0);
  }

  totalPaidSales(customer: CustomerDetails): number {
    return customer.purchases
      .filter((purchase) => !purchase.openDebt)
      .filter((purchase) => purchase.type === 'SUBSCRIPTION' || purchase.type === 'PRODUCTS')
      .reduce((total, purchase) => total + purchase.totalAmount, 0);
  }
}
