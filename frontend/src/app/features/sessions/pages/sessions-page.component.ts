import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, forkJoin, interval, map } from 'rxjs';

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
  saleIds: string[];
  debtIds: string[];
  label: string;
  rawLabel?: string;
  occurredAt: string;
  amount: number;
  openDebt: boolean;
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
          <div class="accordion-group">
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
          </div>

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

        </article>

        <article class="panel panel-right stack">
          <section class="stack current-section">
            <div class="section-head">
              <div class="section-title-group">
                <h3>Sessions en cours</h3>
                <button type="button" class="ghost filter-toggle" (click)="showCurrentFilters.set(!showCurrentFilters())" [attr.aria-expanded]="showCurrentFilters()">
                  <span class="filter-icon" aria-hidden="true"></span>
                  <span>Filtres</span>
                </button>
              </div>
              <div class="section-tools">
                <p class="meta">{{ filteredCurrent().length }} resultat(s)</p>
              </div>
            </div>
            <div class="filters-grid" *ngIf="showCurrentFilters()">
              <label class="field">
                <span>Recherche par nom</span>
                <input [formControl]="currentFilters.controls.term" placeholder="Nom du client" />
              </label>
              <label class="field">
                <span>Debut a partir de</span>
                <input type="datetime-local" [formControl]="currentFilters.controls.startedAfter" />
              </label>
            </div>
            <div class="table-shell fixed-shell">
              <table class="table">
                <thead>
                  <tr><th>Client</th><th class="center-cell">Type</th><th class="center-cell">Etat</th><th class="center-cell">Credit</th><th class="center-cell">A payer</th><th class="center-cell">Consomme</th><th class="center-cell">Debut</th><th class="actions-head" colspan="4"></th></tr>
                </thead>
                <tbody>
                  <ng-container *ngFor="let item of pagedCurrent()">
                    <tr [class.active-row]="sessionState(item) === 'En cours'" [class.paused-row]="sessionState(item) === 'Pause'" [class.paid-row]="sessionState(item) === 'Paye'">
                      <td class="client-cell">{{ item.customerName }}</td>
                      <td class="center-cell">{{ item.customerType === 'SUBSCRIBER' ? 'Abonne' : 'Client' }}</td>
                      <td class="center-cell"><span class="inline-state" [class.paused-chip]="sessionState(item) === 'Pause'" [class.active-chip]="sessionState(item) === 'En cours'" [class.paid-chip]="sessionState(item) === 'Paye'">{{ sessionState(item) }}</span></td>
                      <td class="center-cell">
                        <span *ngIf="item.customerType === 'SUBSCRIBER'; else noCredit">{{ formatRemaining(item) }}</span>
                        <ng-template #noCredit><span class="empty-chip">Aucun</span></ng-template>
                      </td>
                      <td class="center-cell">{{ item.totalAmountDue | number:'1.2-2' }} EUR</td>
                      <td class="center-cell">{{ formatElapsed(item) }}</td>
                      <td class="center-cell">{{ item.startedAt | date:'dd/MM/yyyy HH:mm' }}</td>
                      <td class="action-cell"><button type="button" class="ghost" (click)="togglePause(item)">{{ item.paused ? 'Reprendre' : 'Pause' }}</button></td>
                      <td class="action-cell"><button type="button" (click)="openStopDialog(item)">Arreter</button></td>
                      <td class="action-cell"><button type="button" class="ghost" (click)="toggleDetails(item)">Detail</button></td>
                      <td class="action-cell"><button type="button" class="ghost" *ngIf="item.customerType === 'WALK_IN'" (click)="selectWalkInSession(item)">Convertir</button></td>
                    </tr>
                    <tr class="detail-row" *ngIf="expandedSessionId() === item.sessionId">
                      <td colspan="11">
                        <div class="detail-card" *ngIf="sessionDetails()[item.customerId] as details; else detailLoading">
                          <div class="detail-grid">
                            <section class="stack">
                              <h4>Achats du jour</h4>
                              <div class="purchase-list" *ngIf="todayEntries(details).length > 0; else noPurchases">
                                <article class="purchase-item compact session-entry" [class.debt-entry]="entry.kind === 'debt'" *ngFor="let entry of todayEntries(details)">
                                  <strong>
                                    <span class="money-alert-icon debt-entry-icon" *ngIf="entry.openDebt" title="Dette ouverte"><span class="bill back"></span><span class="bill front"></span><span class="slash"></span></span>
                                    {{ formatEntryLabel(entry.label) }}
                                  </strong>
                                  <span class="entry-date">{{ entry.occurredAt | date:'dd/MM/yyyy HH:mm' }}</span>
                                  <span class="entry-amount">{{ entry.amount | number:'1.2-2' }} EUR</span>
                                  <label class="entry-check" title="Cocher pour mettre en dette ou regler la dette">
                                    <input type="checkbox" [checked]="entry.openDebt" (change)="toggleEntryDebt(entry, $event)" />
                                  </label>
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
                              <p class="muted"><strong>Cout connexion :</strong> {{ item.calculatedPrice | number:'1.2-2' }} EUR</p>
                              <p class="muted"><strong>Achats du jour :</strong> {{ item.purchasesAmount | number:'1.2-2' }} EUR</p>
                              <p class="muted"><strong>A payer :</strong> {{ item.totalAmountDue | number:'1.2-2' }} EUR</p>
                              <p class="muted debt-summary" *ngIf="details.debts.length > 0"><strong>Dettes :</strong> <span class="money-alert-icon debt-indicator" title="Dettes ouvertes"><span class="bill back"></span><span class="bill front"></span><span class="slash"></span></span></p>
                              <div class="actions">
                                <button type="button" class="ghost" (click)="openProductSaleModal(item)">Vendre un produit</button>
                                <button type="button" class="ghost" (click)="openSubscriptionSaleModal(item)">Vendre un abonnement</button>
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
            </div>
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
            <div class="section-title-group">
              <h3>Sessions du jour</h3>
              <button type="button" class="ghost filter-toggle" (click)="showDayFilters.set(!showDayFilters())" [attr.aria-expanded]="showDayFilters()">
                <span class="filter-icon" aria-hidden="true"></span>
                <span>Filtres</span>
              </button>
            </div>
            <div class="section-tools">
              <p class="meta">{{ filteredDay().length }} resultat(s)</p>
            </div>
          </div>
          <div class="filters-grid" *ngIf="showDayFilters()">
            <label class="field">
              <span>Recherche par nom</span>
              <input [formControl]="dayFilters.controls.term" placeholder="Nom du client" />
            </label>
            <label class="field">
              <span>Debut a partir de</span>
              <input type="datetime-local" [formControl]="dayFilters.controls.startedAfter" />
            </label>
          </div>
          <div class="table-shell">
            <table class="table">
              <thead>
                <tr><th>Client</th><th class="center-cell">Type</th><th class="center-cell">Duree</th><th class="center-cell">Credit</th><th class="center-cell">A payer</th><th class="center-cell">Debut</th><th class="center-cell">Fin</th><th></th></tr>
              </thead>
              <tbody>
                <ng-container *ngFor="let item of pagedDay()">
                  <tr>
                    <td class="client-cell">{{ item.customerName }}</td>
                    <td class="center-cell">{{ item.customerType === 'SUBSCRIBER' ? 'Abonne' : 'Client' }}</td>
                    <td class="center-cell">{{ item.consumedMinutes }} min</td>
                    <td class="center-cell">
                      <span *ngIf="item.customerType === 'SUBSCRIBER'; else noDayCredit">{{ item.remainingMinutes }} min</span>
                      <ng-template #noDayCredit><span class="empty-chip">Aucun</span></ng-template>
                    </td>
                    <td class="center-cell">{{ item.totalAmountDue | number:'1.2-2' }} EUR</td>
                    <td class="center-cell">{{ item.startedAt | date:'dd/MM/yyyy HH:mm' }}</td>
                    <td class="center-cell">{{ item.endedAt ? (item.endedAt | date:'dd/MM/yyyy HH:mm') : '-' }}</td>
                    <td class="action-cell"><button type="button" class="ghost" (click)="toggleDayDetails(item)">Detail</button></td>
                  </tr>
                  <tr class="detail-row" *ngIf="expandedDaySessionId() === item.sessionId">
                    <td colspan="8">
                      <div class="detail-card" *ngIf="sessionDetails()[item.customerId] as details; else dayDetailLoading">
                        <div class="detail-grid">
                          <section class="stack">
                            <h4>Achats du jour</h4>
                            <div class="purchase-list" *ngIf="todayEntries(details).length > 0; else noDayPurchases">
                              <article class="purchase-item compact session-entry" [class.debt-entry]="entry.kind === 'debt'" *ngFor="let entry of todayEntries(details)">
                                <strong>
                                  <span class="money-alert-icon debt-entry-icon" *ngIf="entry.openDebt" title="Dette ouverte"><span class="bill back"></span><span class="bill front"></span><span class="slash"></span></span>
                                  {{ formatEntryLabel(entry.label) }}
                                </strong>
                                <span class="entry-date">{{ entry.occurredAt | date:'dd/MM/yyyy HH:mm' }}</span>
                                <span class="entry-amount">{{ entry.amount | number:'1.2-2' }} EUR</span>
                                <label class="entry-check" title="Cocher pour mettre en dette ou regler la dette">
                                  <input type="checkbox" [checked]="entry.openDebt" (change)="toggleEntryDebt(entry, $event)" />
                                </label>
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
                            <p class="muted"><strong>Cout connexion :</strong> {{ item.calculatedPrice | number:'1.2-2' }} EUR</p>
                            <p class="muted"><strong>Achats du jour :</strong> {{ item.purchasesAmount | number:'1.2-2' }} EUR</p>
                            <p class="muted"><strong>A payer :</strong> {{ item.totalAmountDue | number:'1.2-2' }} EUR</p>
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
          </div>
          <div class="pager" *ngIf="dayPageCount() > 1">
            <button type="button" class="ghost" (click)="changeDayPage(-1)" [disabled]="dayPage() === 1">Precedent</button>
            <span>Page {{ dayPage() }} / {{ dayPageCount() }}</span>
            <button type="button" class="ghost" (click)="changeDayPage(1)" [disabled]="dayPage() === dayPageCount()">Suivant</button>
          </div>
        </section>
      </article>
      <div class="modal-backdrop" *ngIf="stopTargetSession() as session" (click)="closeStopDialog()">
        <div class="confirm-modal" (click)="$event.stopPropagation()">
          <div class="section-head">
            <div>
              <h3>Arreter la session</h3>
              <p class="meta">{{ session.customerName }}</p>
            </div>
            <button type="button" class="ghost" (click)="closeStopDialog()">Fermer</button>
          </div>
          <div class="stack confirm-body">
            <p class="muted">Confirme l'arret et precise si le montant est regle maintenant.</p>
            <div class="confirm-summary">
              <p><strong>Connexion :</strong> {{ session.calculatedPrice | number:'1.2-2' }} EUR</p>
              <p><strong>Achats du jour :</strong> {{ stopDialogPurchaseAmount(session) | number:'1.2-2' }} EUR</p>
              <p class="confirm-total"><strong>Total du jour :</strong> {{ stopDialogTotalAmount(session) | number:'1.2-2' }} EUR</p>
              <div class="confirm-debts" *ngIf="stopDialogOpenDebts().length > 0">
                <p><strong>Dettes ouvertes a rappeler :</strong> {{ stopDialogDebtAmount() | number:'1.2-2' }} EUR</p>
                <div class="purchase-list">
                  <article class="purchase-item compact session-entry debt-entry" *ngFor="let debt of stopDialogOpenDebts()">
                    <strong>
                      <span class="money-alert-icon debt-entry-icon" title="Dette ouverte"><span class="bill back"></span><span class="bill front"></span><span class="slash"></span></span>
                      {{ formatEntryLabel(debt.label) }}
                    </strong>
                    <span class="entry-date">{{ debt.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
                    <span class="entry-amount">{{ debt.amount | number:'1.2-2' }} EUR</span>
                    <span></span>
                  </article>
                </div>
              </div>
            </div>
            <div class="actions">
              <button type="button" class="ghost" (click)="confirmStop(false)">Arreter sans encaisser</button>
              <button type="button" (click)="confirmStop(true)">C'est paye</button>
            </div>
          </div>
        </div>
      </div>
      <div class="modal-backdrop" *ngIf="saleTargetSession() as session" (click)="closeSaleModal()">
        <div class="confirm-modal" (click)="$event.stopPropagation()">
          <div class="section-head">
            <div>
              <h3>{{ saleModalType() === 'subscription' ? 'Vendre un abonnement' : 'Vendre un produit' }}</h3>
              <p class="meta">{{ session.customerName }} - {{ session.customerType === 'SUBSCRIBER' ? 'Abonne' : 'Client' }}</p>
            </div>
            <button type="button" class="ghost" (click)="closeSaleModal()">Fermer</button>
          </div>
          <form *ngIf="saleModalType() === 'product'; else subscriptionSaleModal" [formGroup]="productSaleForm" (ngSubmit)="sellProductForSession()" class="stack confirm-body">
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
          <ng-template #subscriptionSaleModal>
            <form [formGroup]="subscriptionSaleForm" (ngSubmit)="sellSubscriptionForSession()" class="stack confirm-body">
              <label class="field">
                <span>Offre d'abonnement</span>
                <select formControlName="subscriptionOfferId">
                  <option value="">Choisir une offre</option>
                  <option *ngFor="let offer of offers()" [value]="offer.offerId">{{ offer.name }} - {{ offer.includedMinutes }} min</option>
                </select>
              </label>
              <label class="checkbox">
                <input type="checkbox" formControlName="createDebt" />
                <span>Creer une dette au lieu d'encaisser</span>
              </label>
              <button type="submit" [disabled]="subscriptionSaleForm.invalid">Enregistrer la vente</button>
            </form>
          </ng-template>
        </div>
      </div>
    </section>
  `,
  styleUrl: './sessions-page.component.css',
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
  readonly saleModalType = signal<'product' | 'subscription' | null>(null);
  readonly stopTargetSession = signal<CafeSession | null>(null);
  readonly expandedSessionId = signal<string | null>(null);
  readonly expandedDaySessionId = signal<string | null>(null);
  readonly sessionDetails = signal<Record<string, CustomerDetails>>({});
  readonly sessionObservedAt = signal<Record<string, number>>({});
  readonly error = signal('');
  readonly notice = signal('');
  readonly openSection = signal<'walk-in' | 'subscriber-existing' | 'subscriber-new' | null>('walk-in');
  readonly currentPage = signal(1);
  readonly dayPage = signal(1);
  readonly currentTerm = signal('');
  readonly currentStartedAfter = signal('');
  readonly dayTerm = signal('');
  readonly dayStartedAfter = signal('');
  readonly now = signal(Date.now());
  readonly showCurrentFilters = signal(false);
  readonly showDayFilters = signal(false);

  readonly walkInForm = this.fb.nonNullable.group({ customerName: [''] });
  readonly subscriberSearchForm = this.fb.nonNullable.group({ term: [''] });
  readonly newSubscriberForm = this.fb.nonNullable.group({ name: [''], subscriptionOfferId: [''] });
  readonly convertForm = this.fb.nonNullable.group({ subscriptionOfferId: [''], deductCurrentSession: [false] });
  readonly productSaleForm = this.fb.nonNullable.group({ productId: ['', Validators.required], quantity: [1, [Validators.required, Validators.min(1)]], createDebt: [false] });
  readonly subscriptionSaleForm = this.fb.nonNullable.group({ subscriptionOfferId: ['', Validators.required], createDebt: [false] });
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

    interval(1000).pipe(takeUntilDestroyed()).subscribe(() => {
      this.now.set(Date.now());
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
        this.sessionObservedAt.set(Object.fromEntries(value.sessions.map((session) => [session.sessionId, Date.now()])));
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

  openStopDialog(session: CafeSession): void {
    this.stopTargetSession.set(session);
    this.loadCustomerDetails(session.customerId);
    this.error.set('');
    this.notice.set('');
  }

  closeStopDialog(): void {
    this.stopTargetSession.set(null);
  }

  confirmStop(paid: boolean): void {
    const session = this.stopTargetSession();
    if (!session) {
      return;
    }
    this.api.stop(session.sessionId, paid).subscribe({
      next: (stoppedSession) => {
        const connection = Number(stoppedSession.calculatedPrice ?? 0);
        const purchases = this.stopDialogPurchaseAmount(session);
        const total = connection + purchases;
        this.notice.set(paid
          ? `Session arretee et reglee. Total : ${(connection + purchases).toFixed(2)} EUR (Connexion : ${connection.toFixed(2)} EUR, Achats : ${purchases.toFixed(2)} EUR)`
          : (total > 0
            ? `Session arretee. Total a payer : ${total.toFixed(2)} EUR (Connexion : ${connection.toFixed(2)} EUR, Achats : ${purchases.toFixed(2)} EUR)`
            : `Session arretee. Aucun montant a payer. (Connexion : ${connection.toFixed(2)} EUR, Achats : ${purchases.toFixed(2)} EUR)`));
        this.closeStopDialog();
        this.closeSaleModal();
        this.reload();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Arret impossible'),
    });
  }

  togglePause(session: CafeSession): void {
    const request = session.paused ? this.api.resume(session.sessionId) : this.api.pause(session.sessionId);
    request.subscribe({
      next: (updatedSession) => {
        this.notice.set(updatedSession.paused ? 'Session mise en pause.' : 'Session reprise.');
        this.current.update((sessions) => sessions.map((item) => item.sessionId === updatedSession.sessionId ? updatedSession : item));
        this.sessionObservedAt.update((observedAt) => ({ ...observedAt, [updatedSession.sessionId]: Date.now() }));
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, session.paused ? 'Reprise impossible' : 'Pause impossible')),
    });
  }

  openProductSaleModal(session: CafeSession): void {
    this.saleTargetSession.set(session);
    this.saleModalType.set('product');
    this.productSaleForm.reset({ productId: '', quantity: 1, createDebt: false });
    this.error.set('');
    this.notice.set('');
  }

  openSubscriptionSaleModal(session: CafeSession): void {
    this.saleTargetSession.set(session);
    this.saleModalType.set('subscription');
    this.subscriptionSaleForm.reset({ subscriptionOfferId: '', createDebt: false });
    this.error.set('');
    this.notice.set('');
  }

  closeSaleModal(): void {
    this.saleTargetSession.set(null);
    this.saleModalType.set(null);
    this.productSaleForm.reset({ productId: '', quantity: 1, createDebt: false });
    this.subscriptionSaleForm.reset({ subscriptionOfferId: '', createDebt: false });
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
        this.closeSaleModal();
        this.reload();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, 'Vente produit impossible')),
    });
  }

  sellSubscriptionForSession(): void {
    const session = this.saleTargetSession();
    if (!session || this.subscriptionSaleForm.invalid) {
      this.subscriptionSaleForm.markAllAsTouched();
      return;
    }
    const { subscriptionOfferId, createDebt } = this.subscriptionSaleForm.getRawValue();
    this.salesApi.subscriptionSale({ customerId: session.customerId, subscriptionOfferId, createDebt }).subscribe({
      next: () => {
        this.refreshCustomerDetails(session.customerId);
        this.closeSaleModal();
        this.reload();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, 'Vente abonnement impossible')),
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
    const allPurchases = details.purchases.map((purchase) => ({
      kind: 'purchase' as const,
      saleIds: [purchase.saleId],
      debtIds: [],
      label: purchase.label,
      occurredAt: purchase.soldAt,
      amount: purchase.totalAmount,
      openDebt: purchase.openDebt,
    }));
    const purchases = allPurchases
      .filter((purchase) => new Date(purchase.occurredAt).toDateString() === today)
      .map((purchase) => ({
        kind: 'purchase' as const,
        saleIds: purchase.saleIds,
        debtIds: [],
        label: purchase.label,
        occurredAt: purchase.occurredAt,
        amount: purchase.amount,
        openDebt: purchase.openDebt,
      }));
    const debts = details.debts
      .filter((debt) => debt.status === 'OPEN')
      .map((debt) => ({
        kind: 'debt' as const,
        saleIds: [],
        debtIds: [debt.debtId],
        label: this.normalizeDebtLabel(debt.label),
        rawLabel: debt.label,
        occurredAt: debt.createdAt,
        amount: debt.amount,
        openDebt: true,
      }));
    const mergedPurchases = purchases.map((purchase) => {
      const matchedDebt = debts.find((debt) => this.shouldMergeDebtIntoPurchase(debt, purchase));
      return matchedDebt
        ? {
            ...purchase,
            kind: 'debt' as const,
            debtIds: matchedDebt.debtIds,
            occurredAt: matchedDebt.occurredAt,
            openDebt: true,
          }
        : purchase;
    });
    const remainingDebts = debts.filter((debt) => !purchases.some((purchase) => this.shouldMergeDebtIntoPurchase(debt, purchase)));
    const relabeledDebts = remainingDebts.map((debt) => {
      const matchedPurchase = allPurchases.find((purchase) => this.isDebtLinkedToPurchase(debt, purchase));
      return matchedPurchase
        ? { ...debt, label: matchedPurchase.label, saleIds: matchedPurchase.saleIds }
        : debt;
    });

    return this.groupDebtEntries([...mergedPurchases, ...relabeledDebts])
      .sort((left, right) => new Date(right.occurredAt).getTime() - new Date(left.occurredAt).getTime());
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

  formatElapsed(session: CafeSession): string {
    return this.formatSeconds(this.liveConsumedSeconds(session));
  }

  formatRemaining(session: CafeSession): string {
    return this.formatSeconds(this.liveRemainingSeconds(session));
  }

  sessionState(session: CafeSession): 'En cours' | 'Pause' | 'Paye' {
    if (session.paused) {
      return 'Pause';
    }
    return session.paid && Number(session.openDebtAmount ?? 0) <= 0.001 ? 'Paye' : 'En cours';
  }

  stopDialogPurchaseAmount(session: CafeSession): number {
    const details = this.stopDialogDetails();
    if (!details) {
      return Number(session.purchasesAmount ?? 0);
    }
    const today = new Date().toDateString();
    return details.purchases
      .filter((purchase) => new Date(purchase.soldAt).toDateString() === today)
      .filter((purchase) => !purchase.openDebt)
      .reduce((total, purchase) => total + Number(purchase.totalAmount ?? 0), 0);
  }

  stopDialogOpenDebts() {
    const details = this.stopDialogDetails();
    if (!details) {
      return [];
    }
    return details.debts.filter((debt) => debt.status === 'OPEN');
  }

  stopDialogDebtAmount(): number {
    return this.stopDialogOpenDebts().reduce((total, debt) => total + Number(debt.amount ?? 0), 0);
  }

  stopDialogTotalAmount(session: CafeSession): number {
    return Number(session.calculatedPrice ?? 0) + this.stopDialogPurchaseAmount(session);
  }

  toggleEntryDebt(entry: SessionDetailEntry, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const requests = checked
      ? entry.saleIds.map((saleId) => this.customersApi.createDebtFromSale(saleId))
      : entry.debtIds.map((debtId) => this.customersApi.settleDebt(debtId));

    if (requests.length === 0) {
      (event.target as HTMLInputElement).checked = entry.openDebt;
      this.error.set('Action impossible sur cette ligne.');
      return;
    }

    forkJoin(requests).subscribe({
      next: () => {
        this.notice.set(checked ? 'Dette creee.' : 'Dette reglee.');
        this.refreshAfterDebtChange();
      },
      error: (error: HttpErrorResponse) => {
        (event.target as HTMLInputElement).checked = entry.openDebt;
        this.error.set(this.resolveHttpError(error, checked ? 'Creation de dette impossible' : 'Reglement de dette impossible'));
      },
    });
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

  private shouldMergeDebtIntoPurchase(debt: SessionDetailEntry, purchase: SessionDetailEntry): boolean {
    return this.isDebtLinkedToPurchase(debt, purchase) && new Date(purchase.occurredAt).toDateString() === new Date().toDateString();
  }

  private toDayKey(value: string): string {
    const date = new Date(value);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private isDebtLinkedToPurchase(debt: SessionDetailEntry, purchase: SessionDetailEntry): boolean {
    if (debt.kind !== 'debt' || purchase.kind !== 'purchase') {
      return false;
    }
    if (Math.abs(debt.amount - purchase.amount) > 0.001) {
      return false;
    }
    const saleMinuteKey = this.saleMinuteKeyFromDebtLabel(debt.rawLabel);
    if (!saleMinuteKey) {
      return false;
    }
    return saleMinuteKey === this.toMinuteKey(purchase.occurredAt);
  }

  private saleMinuteKeyFromDebtLabel(label?: string): string | null {
    if (!label) {
      return null;
    }
    const isoMatch = label.match(/du\s(\d{4}-\d{2}-\d{2}T\d{2}:\d{2})/);
    if (isoMatch) {
      return this.toMinuteKey(isoMatch[1]);
    }
    const frMatch = label.match(/du\s(\d{2}\/\d{2}\/\d{4}\s\d{2}:\d{2})/);
    if (!frMatch) {
      return null;
    }
    const [datePart, timePart] = frMatch[1].split(' ');
    const [day, month, year] = datePart.split('/');
    return `${year}-${month}-${day}-${timePart.slice(0, 2)}-${timePart.slice(3, 5)}`;
  }

  private groupDebtEntries(entries: SessionDetailEntry[]): SessionDetailEntry[] {
    const grouped = new Map<string, SessionDetailEntry>();
    for (const entry of entries) {
      if (!entry.openDebt) {
        grouped.set(`purchase-${entry.saleIds.join(',')}-${entry.occurredAt}`, entry);
        continue;
      }
      const key = `debt-${entry.label.toLowerCase()}`;
      const existing = grouped.get(key);
      if (!existing) {
        grouped.set(key, { ...entry });
        continue;
      }
      grouped.set(key, {
        ...existing,
        saleIds: [...new Set([...existing.saleIds, ...entry.saleIds])],
        debtIds: [...new Set([...existing.debtIds, ...entry.debtIds])],
        amount: existing.amount + entry.amount,
        occurredAt: new Date(existing.occurredAt).getTime() >= new Date(entry.occurredAt).getTime() ? existing.occurredAt : entry.occurredAt,
      });
    }
    return [...grouped.values()];
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

  private liveConsumedSeconds(session: CafeSession): number {
    const observedAt = this.sessionObservedAt()[session.sessionId] ?? this.now();
    const extraSeconds = session.paused ? 0 : Math.max(0, Math.floor((this.now() - observedAt) / 1000));
    return Math.max(0, session.consumedSeconds + extraSeconds);
  }

  private liveRemainingSeconds(session: CafeSession): number {
    return Math.max(0, session.remainingMinutes * 60 - this.liveConsumedSeconds(session));
  }

  private formatSeconds(totalSeconds: number): string {
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
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

  private stopDialogDetails(): CustomerDetails | null {
    const session = this.stopTargetSession();
    if (!session) {
      return null;
    }
    return this.sessionDetails()[session.customerId] ?? null;
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

  private refreshAfterDebtChange(): void {
    const customerIds = new Set<string>();
    const currentSessionId = this.expandedSessionId();
    const daySessionId = this.expandedDaySessionId();
    if (currentSessionId) {
      const session = this.current().find((item) => item.sessionId === currentSessionId);
      if (session) {
        customerIds.add(session.customerId);
      }
    }
    if (daySessionId) {
      const session = this.day().find((item) => item.sessionId === daySessionId);
      if (session) {
        customerIds.add(session.customerId);
      }
    }
    if (this.saleTargetSession()) {
      customerIds.add(this.saleTargetSession()!.customerId);
    }
    customerIds.forEach((customerId) => this.refreshCustomerDetails(customerId));
    this.reload();
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
