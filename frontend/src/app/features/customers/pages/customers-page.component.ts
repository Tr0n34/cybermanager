import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, map } from 'rxjs';

import type { Customer, CustomerDetails } from '../models/customer.models';
import { CustomersApiService } from '../services/customers-api.service';
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
          <h2>Clients specifiques</h2>
        </div>
        <p class="lede">Les clients journaliers et abonnes crees depuis l'ecran sessions apparaissent ici. Cette page permet aussi de creer des clients specifiques et des abonnes.</p>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>

      <div class="grid">
        <article class="panel stack">
          <section class="stack">
            <label class="field">
              <span>Recherche client</span>
              <input [formControl]="filters.controls.term" placeholder="Nom du client ou abonne" />
            </label>
          </section>

          <table class="table">
            <tr><th>Nom</th><th>Type</th><th>Abonnement</th><th>Credit</th><th></th></tr>
            <tr *ngFor="let item of customers()">
              <td>{{ item.name }}</td>
              <td>{{ item.type === 'SUBSCRIBER' ? 'Abonne' : 'Client specifique' }}</td>
              <td>{{ item.type === 'SUBSCRIBER' ? 'Actif' : 'Aucun' }}</td>
              <td>{{ item.remainingMinutes }} min</td>
              <td><button type="button" (click)="open(item.customerId)">Ouvrir</button></td>
            </tr>
          </table>
        </article>

        <article class="panel stack">
          <section class="stack">
            <h3>Creer un client</h3>
            <form [formGroup]="createForm" (ngSubmit)="createCustomer()" class="stack">
              <label class="field">
                <span>Nom du client</span>
                <input formControlName="name" placeholder="Ex. Nadia Benali" />
              </label>
              <label class="field">
                <span>Type de client</span>
                <select formControlName="type">
                  <option value="WALK_IN">Client specifique</option>
                  <option value="SUBSCRIBER">Client abonne</option>
                </select>
              </label>
              <label class="field" *ngIf="createForm.controls.type.value === 'SUBSCRIBER'">
                <span>Offre d'abonnement initiale</span>
                <select formControlName="subscriptionOfferId">
                  <option value="">Choisir une offre</option>
                  <option *ngFor="let offer of offers()" [value]="offer.offerId">{{ offer.name }} - {{ offer.includedMinutes }} min</option>
                </select>
              </label>
              <button type="submit">Creer</button>
            </form>
          </section>

          <section class="stack" *ngIf="selected() as customer">
            <h3>Fiche client</h3>
            <form [formGroup]="editForm" (ngSubmit)="save(customer.customerId)" class="stack">
              <label class="field">
                <span>Nom du client</span>
                <input formControlName="name" />
              </label>
              <div class="facts">
                <p><strong>Type :</strong> {{ customer.type === 'SUBSCRIBER' ? 'Abonne' : 'Client specifique' }}</p>
                <p><strong>Abonnement en cours :</strong> {{ customer.currentSubscriptionLabel ?? 'Aucun' }}</p>
                <p><strong>Credit disponible :</strong> {{ customer.remainingMinutes }} min</p>
              </div>
              <div class="actions">
                <button type="submit">Enregistrer</button>
                <button type="button" class="ghost" (click)="clearSelection()">Fermer</button>
              </div>
            </form>

            <div class="stack">
              <h4>Achats du client</h4>
              <table class="table compact" *ngIf="customer.purchases.length > 0; else emptyPurchases">
                <tr><th>Quand</th><th>Type</th><th>Detail</th><th>Total</th></tr>
                <tr *ngFor="let purchase of customer.purchases">
                  <td>{{ purchase.soldAt | date:'short' }}</td>
                  <td>{{ purchase.type }}</td>
                  <td>{{ purchase.label }}</td>
                  <td>{{ purchase.totalAmount | number:'1.2-2' }} EUR</td>
                </tr>
              </table>
              <ng-template #emptyPurchases><p class="muted">Aucun achat enregistre.</p></ng-template>
            </div>
          </section>
        </article>
      </div>
    </section>
  `,
  styles: `.page,.stack{display:grid;gap:1rem}.hero{display:grid;gap:.5rem}.eyebrow{margin:0;color:#9a3412;text-transform:uppercase;letter-spacing:.12em;font-size:.72rem}.lede{margin:0;max-width:56rem;color:#475569}.grid{display:grid;grid-template-columns:1.1fr 1fr;gap:1.25rem}.panel{background:linear-gradient(180deg,#fff,#fff7ed);padding:1.25rem;border-radius:1.25rem;box-shadow:0 18px 40px rgba(15,23,42,.08)}.field{display:grid;gap:.45rem}.field span{font-size:.86rem;font-weight:700;color:#334155}.table{width:100%;border-collapse:collapse}.table th,.table td{padding:.8rem;border-bottom:1px solid #e2e8f0;text-align:left;vertical-align:top}.compact th,.compact td{padding:.6rem}.facts{display:grid;gap:.35rem;padding:.9rem 1rem;background:#fff;border-radius:1rem;border:1px solid #fed7aa}.facts p{margin:0}.actions{display:flex;gap:.75rem;flex-wrap:wrap}.ghost{background:#fff;color:#334155;border:1px solid #cbd5e1}.muted{margin:0;color:#64748b}.error{margin:0;color:#991b1b;font-weight:700}input,select,button{padding:.85rem .95rem;border-radius:.85rem;border:1px solid #cbd5e1;font:inherit}button{background:#0f172a;color:#fff;border:0;font-weight:700;cursor:pointer}@media(max-width:1100px){.grid{grid-template-columns:1fr}}`,
})
export class CustomersPageComponent {
  private readonly api = inject(CustomersApiService);
  private readonly offersApi = inject(SubscriptionOffersApiService);
  private readonly fb = inject(FormBuilder);

  readonly customers = signal<Customer[]>([]);
  readonly offers = signal<SubscriptionOffer[]>([]);
  readonly selected = signal<CustomerDetails | null>(null);
  readonly error = signal('');

  readonly filters = this.fb.nonNullable.group({ term: [''] });
  readonly createForm = this.fb.nonNullable.group({
    name: [''],
    type: ['WALK_IN' as 'WALK_IN' | 'SUBSCRIBER'],
    subscriptionOfferId: [''],
  });
  readonly editForm = this.fb.nonNullable.group({ name: [''] });

  constructor() {
    this.load();
    this.loadOffers();
    this.filters.controls.term.valueChanges.pipe(
      map((value) => value.trim()),
      debounceTime(150),
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe(() => this.load());
  }

  load(): void {
    const term = this.filters.getRawValue().term;
    this.api.search(term).subscribe({
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
        this.createForm.reset({ name: '', type: 'WALK_IN', subscriptionOfferId: '' });
        this.load();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Creation impossible'),
    });
  }

  open(customerId: string): void {
    this.api.get(customerId).subscribe({
      next: (customer) => {
        this.selected.set(customer);
        this.editForm.patchValue({ name: customer.name });
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Chargement de la fiche impossible'),
    });
  }

  clearSelection(): void {
    this.selected.set(null);
    this.editForm.reset({ name: '' });
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
}
