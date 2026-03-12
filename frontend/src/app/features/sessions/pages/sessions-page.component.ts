import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, map } from 'rxjs';

import type { Customer, CustomerDetails, CustomerPurchase } from '../../customers/models/customer.models';
import { CustomersApiService } from '../../customers/services/customers-api.service';
import type { Product } from '../../products/models/product.models';
import { ProductsApiService } from '../../products/services/products-api.service';
import type { SubscriptionOffer } from '../../subscriptions/models/subscription-offer.models';
import { SubscriptionOffersApiService } from '../../subscriptions/services/subscription-offers-api.service';
import { SalesApiService } from '../../sales/services/sales-api.service';
import type { CafeSession } from '../models/session.models';
import { SessionDisplaySettingsService } from '../services/session-display-settings.service';
import { SessionsApiService } from '../services/sessions-api.service';

type SessionDetailEntry = {
  kind: 'purchase' | 'debt';
  label: string;
  occurredAt: string;
  amount: number;
};

@Component({
  selector: 'app-sessions-page',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="page">
      <header class="hero">
        <div>
          <p class="eyebrow">Flux comptoir</p>
          <h2>Sessions de connexion</h2>
        </div>
        <div class="hero-actions">
          <p class="lede">Lance une session journaliere, cree un client abonne ou retrouve rapidement un client existant par autocompletion.</p>
          <a routerLink="/sessions/settings" class="ghost-link">Configurer l'affichage</a>
        </div>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>
      <p class="notice" *ngIf="notice()">{{ notice() }}</p>

      <div class="grid">
        <article class="panel panel-left stack">
          <section class="mode-card" [class.open]="openSection() === 'walk-in'">
            <button type="button" class="mode-toggle" (click)="setSection('walk-in')">
              <span>Client</span>
              <small>{{ openSection() === 'walk-in' ? 'Refermer' : 'Ouvrir' }}</small>
            </button>
            <div class="mode-body">
              <form [formGroup]="walkInForm" (ngSubmit)="startWalkIn()" class="stack">
                <label class="field">
                  <span>Nom du client</span>
                  <input formControlName="customerName" placeholder="Ex. Samir" />
                </label>
                <button type="submit">Demarrer la session</button>
              </form>
            </div>
          </section>

          <section class="mode-card" [class.open]="openSection() === 'subscriber-existing'">
            <button type="button" class="mode-toggle" (click)="setSection('subscriber-existing')">
              <span>Abonne existant</span>
              <small>{{ openSection() === 'subscriber-existing' ? 'Refermer' : 'Ouvrir' }}</small>
            </button>
            <div class="mode-body stack">
              <label class="field">
                <span>Recherche d'un abonne</span>
                <input [formControl]="subscriberSearchForm.controls.term" placeholder="Nom de l'abonne" />
              </label>
              <div class="suggestions" *ngIf="subscriberResults().length > 0">
                <button type="button" class="suggestion" *ngFor="let item of subscriberResults()" (click)="startSubscriber(item)">
                  <strong>{{ item.name }}</strong>
                  <span>{{ item.remainingMinutes }} min disponibles</span>
                </button>
              </div>
            </div>
          </section>

          <section class="mode-card" [class.open]="openSection() === 'subscriber-new'">
            <button type="button" class="mode-toggle" (click)="setSection('subscriber-new')">
              <span>Nouvel abonne</span>
              <small>{{ openSection() === 'subscriber-new' ? 'Refermer' : 'Ouvrir' }}</small>
            </button>
            <div class="mode-body">
              <form [formGroup]="newSubscriberForm" (ngSubmit)="createAndStartSubscriber()" class="stack">
                <label class="field">
                  <span>Nom du nouvel abonne</span>
                  <input formControlName="name" placeholder="Ex. Lina Dupont" />
                </label>
                <label class="field">
                  <span>Abonnement initial</span>
                  <select formControlName="subscriptionOfferId">
                    <option value="">Choisir une offre</option>
                    <option *ngFor="let offer of offers()" [value]="offer.offerId">{{ offer.name }} - {{ offer.includedMinutes }} min</option>
                  </select>
                </label>
                <button type="submit">Creer et demarrer</button>
              </form>
            </div>
          </section>

          <section class="mode-card accent open" *ngIf="selectedWalkInSession() as session">
            <div class="mode-toggle static">
              <span>Convertir la session en abonne</span>
            </div>
            <div class="mode-body visible">
              <form [formGroup]="convertForm" (ngSubmit)="convertSelectedWalkIn()" class="stack">
                <p class="muted">{{ session.customerName }}</p>
                <label class="field">
                  <span>Offre d'abonnement</span>
                  <select formControlName="subscriptionOfferId">
                    <option value="">Choisir une offre</option>
                    <option *ngFor="let offer of offers()" [value]="offer.offerId">{{ offer.name }} - {{ offer.includedMinutes }} min</option>
                  </select>
                </label>
                <label class="checkbox">
                  <input type="checkbox" formControlName="deductCurrentSession" />
                  <span>Deduir la session en cours</span>
                </label>
                <div class="actions">
                  <button type="submit">Convertir</button>
                  <button type="button" class="ghost" (click)="selectedWalkInSession.set(null)">Fermer</button>
                </div>
              </form>
            </div>
          </section>

          <section class="sale-card stack" *ngIf="saleTargetSession() as session">
            <div class="section-head">
              <div>
                <h3>Vendre un produit</h3>
                <p class="meta">{{ session.customerName }} - {{ session.customerType === 'SUBSCRIBER' ? 'Abonne' : 'Client' }}</p>
              </div>
              <button type="button" class="ghost" (click)="closeSalePanel()">Fermer</button>
            </div>
            <form [formGroup]="productSaleForm" (ngSubmit)="sellProductForSession()" class="stack">
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
              <button type="submit" [disabled]="productSaleForm.invalid">Enregistrer la vente</button>
            </form>
          </section>
        </article>

        <article class="panel panel-right stack">
          <section class="stack current-section">
            <div class="section-head">
              <h3>Sessions en cours</h3>
              <p class="meta">{{ filteredCurrent().length }} resultat(s)</p>
            </div>
            <div class="filters-grid">
              <label class="field">
                <span>Recherche par nom</span>
                <input [formControl]="currentFilters.controls.term" placeholder="Nom du client" />
              </label>
              <label class="field">
                <span>Debut a partir de</span>
                <input type="datetime-local" [formControl]="currentFilters.controls.startedAfter" />
              </label>
            </div>
            <table class="table">
              <thead>
                <tr><th>Client</th><th>Type</th><th>Credit</th><th>Debut</th><th></th><th></th><th></th></tr>
              </thead>
              <tbody>
                <ng-container *ngFor="let item of pagedCurrent()">
                  <tr>
                    <td>{{ item.customerName }}</td>
                    <td>{{ item.customerType === 'SUBSCRIBER' ? 'Abonne' : 'Client' }}</td>
                    <td>{{ item.customerType === 'SUBSCRIBER' ? (item.remainingMinutes + ' min') : '-' }}</td>
                    <td>{{ item.startedAt | date:'dd/MM/yyyy HH:mm' }}</td>
                    <td><button type="button" (click)="stop(item.sessionId)">Arreter</button></td>
                    <td><button type="button" class="ghost" (click)="toggleDetails(item)">Detail</button></td>
                    <td><button type="button" class="ghost" *ngIf="item.customerType === 'WALK_IN'" (click)="selectWalkInSession(item)">Convertir</button></td>
                  </tr>
                  <tr class="detail-row" *ngIf="expandedSessionId() === item.sessionId">
                    <td colspan="7">
                      <div class="detail-card" *ngIf="sessionDetails()[item.customerId] as details; else detailLoading">
                        <div class="detail-grid">
                          <section class="stack">
                            <h4>Achats du jour</h4>
                            <div class="purchase-list" *ngIf="todayEntries(details).length > 0; else noPurchases">
                              <article class="purchase-item compact" [class.debt-entry]="entry.kind === 'debt'" *ngFor="let entry of todayEntries(details)">
                                <strong>
                                  <span class="money-alert-icon debt-entry-icon" *ngIf="entry.kind === 'debt'" title="Dette ouverte"><span class="bill back"></span><span class="bill front"></span><span class="slash"></span></span>
                                  {{ formatEntryLabel(entry.label) }}
                                </strong>
                                <span>{{ entry.occurredAt | date:'dd/MM/yyyy HH:mm' }}</span>
                                <span>{{ entry.amount | number:'1.2-2' }} EUR</span>
                              </article>
                            </div>
                            <ng-template #noPurchases><p class="muted">Aucun achat aujourd'hui.</p></ng-template>
                          </section>

                          <section class="stack">
                            <h4>Resume abonnement</h4>
                            <p class="muted"><strong>Type :</strong> {{ details.type === 'SUBSCRIBER' ? 'Abonne' : 'Client' }}</p>
                            <p class="muted"><strong>Abonnements :</strong> {{ details.currentSubscriptionLabel ?? 'Aucun' }}</p>
                            <p class="muted" *ngIf="details.type === 'SUBSCRIBER'">Les abonnements se cumulent.</p>
                            <p class="muted"><strong>Credit :</strong> {{ details.remainingMinutes }} min</p>
                            <p class="muted debt-summary" *ngIf="details.debts.length > 0"><strong>Dettes :</strong> <span class="money-alert-icon debt-indicator" title="Dettes ouvertes"><span class="bill back"></span><span class="bill front"></span><span class="slash"></span></span></p>
                            <div class="actions">
                              <button type="button" class="ghost" (click)="openSalePanel(item)">Vendre un produit</button>
                            </div>
                          </section>
                        </div>
                      </div>
                      <ng-template #detailLoading>
                        <p class="muted">Chargement du detail...</p>
                      </ng-template>
                    </td>
                  </tr>
                </ng-container>
              </tbody>
            </table>
            <div class="pager" *ngIf="currentPageCount() > 1">
              <button type="button" class="ghost" (click)="changeCurrentPage(-1)" [disabled]="currentPage() === 1">Precedent</button>
              <span>Page {{ currentPage() }} / {{ currentPageCount() }}</span>
              <button type="button" class="ghost" (click)="changeCurrentPage(1)" [disabled]="currentPage() === currentPageCount()">Suivant</button>
            </div>
          </section>
        </article>
      </div>

      <article class="panel day-panel stack">
        <section class="stack">
          <div class="section-head">
            <h3>Sessions du jour</h3>
            <p class="meta">{{ filteredDay().length }} resultat(s)</p>
          </div>
          <div class="filters-grid">
            <label class="field">
              <span>Recherche par nom</span>
              <input [formControl]="dayFilters.controls.term" placeholder="Nom du client" />
            </label>
            <label class="field">
              <span>Debut a partir de</span>
              <input type="datetime-local" [formControl]="dayFilters.controls.startedAfter" />
            </label>
          </div>
          <table class="table">
            <thead>
              <tr><th>Client</th><th>Type</th><th>Duree</th><th>Montant</th><th>Debut</th><th></th></tr>
            </thead>
            <tbody>
              <ng-container *ngFor="let item of pagedDay()">
                <tr>
                  <td>{{ item.customerName }}</td>
                  <td>{{ item.customerType === 'SUBSCRIBER' ? 'Abonne' : 'Client' }}</td>
                  <td>{{ item.consumedMinutes }} min</td>
                  <td>{{ item.calculatedPrice | number:'1.2-2' }} EUR</td>
                  <td>{{ item.startedAt | date:'dd/MM/yyyy HH:mm' }}</td>
                  <td><button type="button" class="ghost" (click)="toggleDayDetails(item)">Detail</button></td>
                </tr>
                <tr class="detail-row" *ngIf="expandedDaySessionId() === item.sessionId">
                  <td colspan="6">
                    <div class="detail-card" *ngIf="sessionDetails()[item.customerId] as details; else dayDetailLoading">
                      <div class="detail-grid">
                        <section class="stack">
                          <h4>Achats du jour</h4>
                          <div class="purchase-list" *ngIf="todayEntries(details).length > 0; else noDayPurchases">
                            <article class="purchase-item compact" [class.debt-entry]="entry.kind === 'debt'" *ngFor="let entry of todayEntries(details)">
                              <strong>
                                <span class="money-alert-icon debt-entry-icon" *ngIf="entry.kind === 'debt'" title="Dette ouverte"><span class="bill back"></span><span class="bill front"></span><span class="slash"></span></span>
                                {{ formatEntryLabel(entry.label) }}
                              </strong>
                              <span>{{ entry.occurredAt | date:'dd/MM/yyyy HH:mm' }}</span>
                              <span>{{ entry.amount | number:'1.2-2' }} EUR</span>
                            </article>
                          </div>
                          <ng-template #noDayPurchases><p class="muted">Aucun achat aujourd'hui.</p></ng-template>
                        </section>

                        <section class="stack">
                          <h4>Resume abonnement</h4>
                          <p class="muted"><strong>Type :</strong> {{ details.type === 'SUBSCRIBER' ? 'Abonne' : 'Client' }}</p>
                          <p class="muted"><strong>Abonnements :</strong> {{ details.currentSubscriptionLabel ?? 'Aucun' }}</p>
                          <p class="muted" *ngIf="details.type === 'SUBSCRIBER'">Les abonnements se cumulent.</p>
                          <p class="muted"><strong>Credit :</strong> {{ details.remainingMinutes }} min</p>
                          <p class="muted debt-summary" *ngIf="details.debts.length > 0"><strong>Dettes :</strong> <span class="money-alert-icon debt-indicator" title="Dettes ouvertes"><span class="bill back"></span><span class="bill front"></span><span class="slash"></span></span></p>
                        </section>
                      </div>
                    </div>
                    <ng-template #dayDetailLoading>
                      <p class="muted">Chargement du detail...</p>
                    </ng-template>
                  </td>
                </tr>
              </ng-container>
            </tbody>
          </table>
          <div class="pager" *ngIf="dayPageCount() > 1">
            <button type="button" class="ghost" (click)="changeDayPage(-1)" [disabled]="dayPage() === 1">Precedent</button>
            <span>Page {{ dayPage() }} / {{ dayPageCount() }}</span>
            <button type="button" class="ghost" (click)="changeDayPage(1)" [disabled]="dayPage() === dayPageCount()">Suivant</button>
          </div>
        </section>
      </article>
    </section>
  `,
  styles: `.page,.stack{display:grid;gap:1rem}.hero{display:grid;gap:.75rem}.hero-actions{display:flex;justify-content:space-between;gap:1rem;align-items:flex-start;flex-wrap:wrap}.eyebrow{margin:0;color:#0f766e;text-transform:uppercase;letter-spacing:.12em;font-size:.72rem}.lede{margin:0;max-width:56rem;color:#475569}.ghost-link{text-decoration:none;background:#fff;color:#334155;border:1px solid #cbd5e1;padding:.8rem 1rem;border-radius:.9rem}.notice{margin:0;color:#9a3412;font-weight:700;background:#fff7ed;border:1px solid #fed7aa;padding:.8rem 1rem;border-radius:.9rem}.grid{display:grid;grid-template-columns:minmax(320px,430px) minmax(0,1fr);gap:1.1rem;align-items:stretch}.panel{background:linear-gradient(180deg,#f8fafc,#ffffff);padding:1.1rem;border-radius:1.35rem;box-shadow:0 18px 40px rgba(15,23,42,.08)}.panel-left{align-self:stretch;max-height:78vh;overflow:auto}.panel-right{align-self:stretch;max-height:78vh;overflow:hidden;min-height:0}.current-section{min-height:0;overflow:auto;padding-right:.15rem}.day-panel{margin-top:.2rem}.mode-card{border:1px solid #e2e8f0;border-radius:1rem;background:#fff;overflow:hidden;transition:max-height .28s ease,transform .28s ease,box-shadow .28s ease,opacity .24s ease;max-height:4.1rem;opacity:.9}.mode-card.open{max-height:30rem;opacity:1;transform:translateY(0);box-shadow:0 14px 30px rgba(15,23,42,.06)}.mode-card.accent{border-color:#fdba74;background:#fff7ed;max-height:24rem}.mode-toggle{width:100%;display:flex;justify-content:space-between;align-items:center;padding:.9rem 1rem;background:transparent;color:#0f172a;border:0;font-weight:800;cursor:pointer}.mode-toggle small{color:#64748b;font-weight:600}.mode-toggle.static{cursor:default}.mode-body{padding:0 1rem;opacity:0;transform:translateY(-8px);transition:opacity .24s ease,transform .24s ease,padding .24s ease;pointer-events:none}.mode-card.open .mode-body,.mode-body.visible{padding:0 1rem 1rem;opacity:1;transform:translateY(0);pointer-events:auto}.field{display:grid;gap:.4rem}.field span{font-size:.83rem;font-weight:700;color:#334155}.filters-grid{display:grid;grid-template-columns:1fr 1fr;gap:.75rem}.suggestions{display:grid;gap:.45rem}.suggestion{display:flex;justify-content:space-between;align-items:center;padding:.75rem .9rem;border-radius:.9rem;background:#fff7ed;color:#111827;border:1px solid #fed7aa}.suggestion span{color:#9a3412}.section-head{display:flex;justify-content:space-between;gap:1rem;align-items:center;flex-wrap:wrap}.section-head h3,.detail-grid h4{margin:0}.meta{margin:0;color:#64748b}.table{width:100%;border-collapse:collapse;font-size:.94rem}.table th,.table td{padding:.62rem .68rem;border-bottom:1px solid #e2e8f0;text-align:left;vertical-align:top}.table th{font-size:.8rem;letter-spacing:.02em;color:#475569}.detail-row td{background:#fffaf4;padding-top:.28rem;padding-bottom:.4rem}.detail-card{padding:.2rem 0}.detail-grid{display:grid;grid-template-columns:1.1fr .9fr;gap:.75rem}.purchase-list{display:grid;gap:.28rem}.purchase-item{display:grid;gap:.08rem;padding:.34rem .52rem;border:1px solid #e2e8f0;border-radius:.7rem;background:#fff}.purchase-item.compact{grid-template-columns:minmax(0,1fr) auto auto;align-items:center;column-gap:.55rem}.purchase-item.compact strong,.purchase-item.compact span{font-size:.82rem;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.purchase-item.compact strong{display:flex;align-items:center;gap:.35rem}.purchase-item.compact span:last-child{font-weight:700;color:#0f172a}.purchase-item.debt-entry{background:#fff1f2;border-color:#fecdd3}.purchase-item.debt-entry strong,.purchase-item.debt-entry span:last-child{color:#b91c1c}.money-alert-icon{position:relative;display:inline-block;width:1.3rem;height:1.1rem;flex:0 0 auto}.money-alert-icon .bill{position:absolute;border-radius:.2rem;background:linear-gradient(180deg,#fecaca,#fca5a5);border:1px solid #b91c1c;box-shadow:inset 0 0 0 1px rgba(255,255,255,.24)}.money-alert-icon .bill::after{content:'';position:absolute;inset:.22rem .28rem;border:1px solid rgba(127,29,29,.55);border-radius:.16rem}.money-alert-icon .bill.back{width:.82rem;height:.56rem;top:.26rem;left:.1rem;opacity:.78}.money-alert-icon .bill.front{width:.88rem;height:.6rem;top:.08rem;left:.28rem}.money-alert-icon .slash{position:absolute;width:.16rem;height:1.18rem;background:#991b1b;border-radius:999px;transform:rotate(38deg);top:-.04rem;left:.56rem;box-shadow:0 0 0 1px rgba(255,255,255,.2)}.debt-summary{display:flex;gap:.5rem;align-items:center}.checkbox{display:flex;gap:.65rem;align-items:center}.actions,.pager{display:flex;gap:.75rem;align-items:center;flex-wrap:wrap}.muted{margin:0;color:#64748b;font-size:.92rem}.error{margin:0;color:#991b1b;font-weight:700}.sale-card{padding:.95rem;border-radius:1rem;background:#fff7ed;border:1px solid #fed7aa}input,select,button{padding:.78rem .9rem;border-radius:.85rem;border:1px solid #cbd5e1;font:inherit}button{background:#0f172a;color:#fff;border:0;font-weight:700;cursor:pointer}.ghost{background:#fff;color:#334155;border:1px solid #cbd5e1}@media(max-width:1100px){.grid{grid-template-columns:1fr}.panel-left,.panel-right{max-height:none;overflow:visible}.current-section{overflow:visible}.filters-grid,.detail-grid,.purchase-item.compact{grid-template-columns:1fr}}`,
})
export class SessionsPageComponent {
  private readonly customersApi = inject(CustomersApiService);
  private readonly offersApi = inject(SubscriptionOffersApiService);
  private readonly productsApi = inject(ProductsApiService);
  private readonly salesApi = inject(SalesApiService);
  private readonly api = inject(SessionsApiService);
  private readonly settingsService = inject(SessionDisplaySettingsService);
  private readonly fb = inject(FormBuilder);

  readonly current = signal<CafeSession[]>([]);
  readonly day = signal<CafeSession[]>([]);
  readonly subscriberResults = signal<Customer[]>([]);
  readonly offers = signal<SubscriptionOffer[]>([]);
  readonly products = signal<Product[]>([]);
  readonly selectedWalkInSession = signal<CafeSession | null>(null);
  readonly saleTargetSession = signal<CafeSession | null>(null);
  readonly expandedSessionId = signal<string | null>(null);
  readonly expandedDaySessionId = signal<string | null>(null);
  readonly sessionDetails = signal<Record<string, CustomerDetails>>({});
  readonly error = signal('');
  readonly notice = signal('');
  readonly openSection = signal<'walk-in' | 'subscriber-existing' | 'subscriber-new' | null>('walk-in');
  readonly currentPage = signal(1);
  readonly dayPage = signal(1);
  readonly currentTerm = signal('');
  readonly currentStartedAfter = signal('');
  readonly dayTerm = signal('');
  readonly dayStartedAfter = signal('');

  readonly walkInForm = this.fb.nonNullable.group({ customerName: [''] });
  readonly subscriberSearchForm = this.fb.nonNullable.group({ term: [''] });
  readonly newSubscriberForm = this.fb.nonNullable.group({ name: [''], subscriptionOfferId: [''] });
  readonly convertForm = this.fb.nonNullable.group({ subscriptionOfferId: [''], deductCurrentSession: [false] });
  readonly productSaleForm = this.fb.nonNullable.group({ productId: ['', Validators.required], quantity: [1, [Validators.required, Validators.min(1)]], createDebt: [false] });
  readonly currentFilters = this.fb.nonNullable.group({ term: [''], startedAfter: [''] });
  readonly dayFilters = this.fb.nonNullable.group({ term: [''], startedAfter: [''] });

  readonly filteredCurrent = computed(() => this.filterSessions(this.current(), this.currentTerm(), this.currentStartedAfter()));
  readonly filteredDay = computed(() => this.filterSessions(this.day(), this.dayTerm(), this.dayStartedAfter()));
  readonly currentPageCount = computed(() => this.getPageCount(this.filteredCurrent().length, this.settingsService.settings().currentPageSize));
  readonly dayPageCount = computed(() => this.getPageCount(this.filteredDay().length, this.settingsService.settings().dayPageSize));
  readonly pagedCurrent = computed(() => this.paginate(this.filteredCurrent(), this.currentPage(), this.settingsService.settings().currentPageSize));
  readonly pagedDay = computed(() => this.paginate(this.filteredDay(), this.dayPage(), this.settingsService.settings().dayPageSize));

  constructor() {
    this.reload();
    this.loadOffers();
    this.productsApi.search('', 'ACTIVE', '').subscribe((value) => this.products.set(value));

    this.subscriberSearchForm.controls.term.valueChanges.pipe(
      map((value) => value.trim()),
      debounceTime(150),
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe((term) => this.autocompleteSubscribers(term));

    this.currentFilters.controls.term.valueChanges.pipe(
      map((value) => value.trim()),
      debounceTime(120),
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe((value) => {
      this.currentTerm.set(value);
      this.currentPage.set(1);
    });

    this.currentFilters.controls.startedAfter.valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe((value) => {
      this.currentStartedAfter.set(value);
      this.currentPage.set(1);
    });

    this.dayFilters.controls.term.valueChanges.pipe(
      map((value) => value.trim()),
      debounceTime(120),
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe((value) => {
      this.dayTerm.set(value);
      this.dayPage.set(1);
    });

    this.dayFilters.controls.startedAfter.valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe((value) => {
      this.dayStartedAfter.set(value);
      this.dayPage.set(1);
    });
  }

  setSection(section: 'walk-in' | 'subscriber-existing' | 'subscriber-new'): void {
    this.openSection.set(this.openSection() === section ? null : section);
  }

  loadOffers(): void {
    this.offersApi.search('', 'ACTIVE').subscribe({
      next: (offers) => this.offers.set(offers),
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Chargement des offres impossible'),
    });
  }

  reload(): void {
    this.api.current().subscribe({
      next: (value) => {
        this.current.set(value.sessions);
        this.currentPage.set(1);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Chargement des sessions en cours impossible'),
    });
    this.api.day().subscribe({
      next: (value) => {
        this.day.set(value);
        this.dayPage.set(1);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Chargement des sessions du jour impossible'),
    });
  }

  autocompleteSubscribers(term: string): void {
    if (term.length < 2) {
      this.subscriberResults.set([]);
      return;
    }
    this.customersApi.search(term, 'SUBSCRIBER').subscribe({
      next: (customers) => {
        this.subscriberResults.set(customers);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Recherche des abonnes impossible'),
    });
  }

  startWalkIn(): void {
    const customerName = this.walkInForm.getRawValue().customerName.trim();
    if (!customerName) {
      this.error.set('Le nom du client est requis');
      return;
    }
    this.api.start({ customerName }).subscribe({
      next: () => {
        this.walkInForm.reset({ customerName: '' });
        this.notice.set('');
        this.reload();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Demarrage impossible'),
    });
  }

  startSubscriber(customer: Customer): void {
    this.api.start({ customerId: customer.customerId }).subscribe({
      next: () => {
        this.subscriberSearchForm.reset({ term: '' });
        this.subscriberResults.set([]);
        this.notice.set('');
        this.reload();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Demarrage impossible'),
    });
  }

  createAndStartSubscriber(): void {
    const payload = this.newSubscriberForm.getRawValue();
    if (!payload.name.trim() || !payload.subscriptionOfferId) {
      this.error.set('Le nom et l offre d abonnement sont requis');
      return;
    }
    this.customersApi.create({ name: payload.name.trim(), type: 'SUBSCRIBER', subscriptionOfferId: payload.subscriptionOfferId }).subscribe({
      next: (customer) => {
        this.newSubscriberForm.reset({ name: '', subscriptionOfferId: '' });
        this.startSubscriber(customer);
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Creation de l abonne impossible'),
    });
  }

  stop(sessionId: string): void {
    this.api.stop(sessionId).subscribe({
      next: (stoppedSession) => {
        const total = Number(stoppedSession.calculatedPrice ?? 0);
        this.notice.set(total > 0
          ? `Session arretee. Total a payer : ${total.toFixed(2)} EUR`
          : 'Session arretee. Aucun montant a payer.');
        this.closeSalePanel();
        this.reload();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Arret impossible'),
    });
  }

  openSalePanel(session: CafeSession): void {
    this.saleTargetSession.set(session);
    this.productSaleForm.reset({ productId: '', quantity: 1, createDebt: false });
    this.error.set('');
    this.notice.set('');
  }

  closeSalePanel(): void {
    this.saleTargetSession.set(null);
    this.productSaleForm.reset({ productId: '', quantity: 1, createDebt: false });
  }

  sellProductForSession(): void {
    const session = this.saleTargetSession();
    if (!session || this.productSaleForm.invalid) {
      this.productSaleForm.markAllAsTouched();
      return;
    }
    const { productId, quantity, createDebt } = this.productSaleForm.getRawValue();
    this.salesApi.productSale({ customerId: session.customerId, lines: [{ productId, quantity }], createDebt }).subscribe({
      next: () => {
        this.refreshCustomerDetails(session.customerId);
        this.closeSalePanel();
        this.reload();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, 'Vente produit impossible')),
    });
  }

  toggleDetails(session: CafeSession): void {
    if (this.expandedSessionId() === session.sessionId) {
      this.expandedSessionId.set(null);
      return;
    }
    this.expandedSessionId.set(session.sessionId);
    this.loadCustomerDetails(session.customerId);
  }

  toggleDayDetails(session: CafeSession): void {
    if (this.expandedDaySessionId() === session.sessionId) {
      this.expandedDaySessionId.set(null);
      return;
    }
    this.expandedDaySessionId.set(session.sessionId);
    this.loadCustomerDetails(session.customerId);
  }

  selectWalkInSession(session: CafeSession): void {
    this.selectedWalkInSession.set(session);
    this.convertForm.reset({ subscriptionOfferId: '', deductCurrentSession: false });
  }

  convertSelectedWalkIn(): void {
    const session = this.selectedWalkInSession();
    const payload = this.convertForm.getRawValue();
    if (!session || !payload.subscriptionOfferId) {
      this.error.set('Une offre d abonnement est requise');
      return;
    }
    this.customersApi.convert(session.customerId, payload as { subscriptionOfferId: string; deductCurrentSession: boolean }).subscribe({
      next: () => {
        this.selectedWalkInSession.set(null);
        this.convertForm.reset({ subscriptionOfferId: '', deductCurrentSession: false });
        this.reload();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Conversion impossible'),
    });
  }

  changeCurrentPage(step: number): void {
    this.currentPage.set(Math.min(this.currentPageCount(), Math.max(1, this.currentPage() + step)));
  }

  changeDayPage(step: number): void {
    this.dayPage.set(Math.min(this.dayPageCount(), Math.max(1, this.dayPage() + step)));
  }

  todayPurchases(purchases: CustomerPurchase[]): CustomerPurchase[] {
    const today = new Date().toDateString();
    return purchases.filter((purchase) => new Date(purchase.soldAt).toDateString() === today);
  }

  todayEntries(details: CustomerDetails): SessionDetailEntry[] {
    const today = new Date().toDateString();
    const purchases = details.purchases
      .filter((purchase) => new Date(purchase.soldAt).toDateString() === today)
      .map((purchase) => ({
        kind: 'purchase' as const,
        label: purchase.label,
        occurredAt: purchase.soldAt,
        amount: purchase.totalAmount,
      }));
    const debts = details.debts
      .filter((debt) => debt.status === 'OPEN' && new Date(debt.createdAt).toDateString() === today)
      .map((debt) => ({
        kind: 'debt' as const,
        label: this.normalizeDebtLabel(debt.label),
        occurredAt: debt.createdAt,
        amount: debt.amount,
      }));
    const mergedPurchases = purchases.map((purchase) => {
      const matchedDebt = debts.find((debt) => this.isSameDebtAsPurchase(debt, purchase));
      return matchedDebt
        ? { ...purchase, kind: 'debt' as const, occurredAt: matchedDebt.occurredAt }
        : purchase;
    });
    const remainingDebts = debts.filter((debt) => !purchases.some((purchase) => this.isSameDebtAsPurchase(debt, purchase)));

    return [...mergedPurchases, ...remainingDebts].sort((left, right) => new Date(right.occurredAt).getTime() - new Date(left.occurredAt).getTime());
  }

  formatEntryLabel(label: string): string {
    const match = label.match(/^(.*\sdu\s)(\d{4}-\d{2}-\d{2}T\d{2}:\d{2})(?::\d{2}(?:\.\d+)?)?$/);
    if (!match) {
      return label;
    }

    const parsed = new Date(match[2]);
    if (Number.isNaN(parsed.getTime())) {
      return label;
    }

    const day = String(parsed.getDate()).padStart(2, '0');
    const month = String(parsed.getMonth() + 1).padStart(2, '0');
    const year = parsed.getFullYear();
    const hours = String(parsed.getHours()).padStart(2, '0');
    const minutes = String(parsed.getMinutes()).padStart(2, '0');
    return `${match[1]}${day}/${month}/${year} ${hours}:${minutes}`;
  }

  private normalizeDebtLabel(label: string): string {
    if (label.startsWith('Vente produits du ')) {
      return 'Produit';
    }
    if (label.startsWith('Vente abonnement du ')) {
      return 'Abonnement';
    }
    if (label.startsWith('Vente temps du ')) {
      return 'Temps';
    }
    if (label.startsWith('Session du ')) {
      return 'Session';
    }
    return this.formatEntryLabel(label);
  }

  private isSameDebtAsPurchase(debt: SessionDetailEntry, purchase: SessionDetailEntry): boolean {
    if (debt.kind !== 'debt' || purchase.kind !== 'purchase') {
      return false;
    }
    if (Math.abs(debt.amount - purchase.amount) > 0.001) {
      return false;
    }
    return this.toMinuteKey(debt.occurredAt) === this.toMinuteKey(purchase.occurredAt);
  }

  private toMinuteKey(value: string): string {
    const date = new Date(value);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}-${hours}-${minutes}`;
  }

  private filterSessions(sessions: CafeSession[], term: string, startedAfter: string): CafeSession[] {
    const normalizedTerm = term.trim().toLowerCase();
    const normalizedDate = startedAfter ? new Date(startedAfter).getTime() : null;
    return sessions
      .filter((session) => {
        const matchesTerm = !normalizedTerm || session.customerName.toLowerCase().includes(normalizedTerm);
        const matchesDate = !normalizedDate || new Date(session.startedAt).getTime() >= normalizedDate;
        return matchesTerm && matchesDate;
      })
      .sort((left, right) => {
        const dateDiff = new Date(right.startedAt).getTime() - new Date(left.startedAt).getTime();
        if (dateDiff !== 0) {
          return dateDiff;
        }
        return left.customerName.localeCompare(right.customerName, 'fr');
      });
  }

  private paginate(items: CafeSession[], page: number, pageSize: number): CafeSession[] {
    const start = (page - 1) * pageSize;
    return items.slice(start, start + pageSize);
  }

  private getPageCount(total: number, pageSize: number): number {
    return Math.max(1, Math.ceil(total / pageSize));
  }

  private loadCustomerDetails(customerId: string): void {
    if (this.sessionDetails()[customerId]) {
      return;
    }
    this.fetchCustomerDetails(customerId);
  }

  private refreshCustomerDetails(customerId: string): void {
    const nextDetails = { ...this.sessionDetails() };
    delete nextDetails[customerId];
    this.sessionDetails.set(nextDetails);
    this.fetchCustomerDetails(customerId);
  }

  private fetchCustomerDetails(customerId: string): void {
    this.customersApi.get(customerId).subscribe({
      next: (details) => {
        this.sessionDetails.set({
          ...this.sessionDetails(),
          [customerId]: details,
        });
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, 'Chargement du detail client impossible')),
    });
  }

  private resolveHttpError(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }
    if (error.error?.message) {
      return error.error.message;
    }
    if (error.status === 0) {
      return 'Serveur inaccessible ou non redemarre.';
    }
    if (error.status === 401) {
      return 'Session expiree. Reconnecte-toi.';
    }
    if (error.status === 403) {
      return 'Action reservee a un administrateur.';
    }
    if (error.status === 404) {
      return 'Endpoint introuvable. Redemarre probablement le backend.';
    }
    return fallback;
  }
}
