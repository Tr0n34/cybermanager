import { CommonModule } from '@angular/common';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged, forkJoin, interval, map, of, switchMap } from 'rxjs';

import type { Customer, CustomerDetails, CustomerPurchase } from '../../customers/models/customer.models';
import { CustomersApiService } from '../../customers/services/customers-api.service';
import type { Product } from '../../products/models/product.models';
import { ProductsApiService } from '../../products/services/products-api.service';
import type { ConnectionPricingTier } from '../../sales/models/sales.models';
import type { SubscriptionOffer } from '../../subscriptions/models/subscription-offer.models';
import { SubscriptionOffersApiService } from '../../subscriptions/services/subscription-offers-api.service';
import { SalesApiService } from '../../sales/services/sales-api.service';
import { AppLoggerService } from '../../../core/services/app-logger.service';
import type { CafeSession } from '../models/session.models';
import { SessionsApiService } from '../services/sessions-api.service';

type SessionDetailEntry = {
  kind: 'purchase' | 'debt';
  saleIds: string[];
  debtIds: string[];
  sessionId?: string | null;
  label: string;
  rawLabel?: string;
  debtLabel?: string;
  occurredAt: string;
  amount: number;
  openDebt: boolean;
};

@Component({
  selector: 'app-sessions-page',
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="page">
      <header class="hero">
        <div>
          <p class="eyebrow">Flux comptoir</p>
          <h2>Sessions de connexion</h2>
        </div>
        <div class="hero-actions">
          <button type="button" class="ghost" (click)="restartSessionsDay()">Fin de journee</button>
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
            <div class="filters-grid collapsible" [class.is-collapsed]="!showCurrentFilters()">
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
                  <tr><th class="center-cell indicator-head"></th><th>Client</th><th class="center-cell">Type</th><th class="center-cell">Etat</th><th class="center-cell">Credit</th><th class="center-cell">A payer</th><th class="center-cell">Consomme</th><th class="center-cell">Debut</th><th class="actions-head" colspan="4"></th></tr>
                </thead>
                <tbody>
                  <ng-container *ngFor="let item of filteredCurrent()">
                    <tr [class.active-row]="sessionState(item) === 'En cours'" [class.paused-row]="sessionState(item) === 'Pause'" [class.awaiting-payment-row]="sessionState(item) === 'A payer'" [class.paid-row]="sessionState(item) === 'Terminee'">
                      <td class="center-cell indicator-cell">
                        <span *ngIf="hasOpenDebt(item); else standardUserIndicator" class="session-indicator debt-session-indicator" title="Client avec dette ouverte" aria-label="Client avec dette ouverte">D</span>
                        <ng-template #standardUserIndicator>
                          <span class="session-indicator ok-session-indicator" title="Client sans dette ouverte" aria-label="Client sans dette ouverte">
                            ✓
                          </span>
                        </ng-template>
                      </td>
                      <td class="client-cell">
                        <button type="button" class="link-button client-link" (click)="openCustomerModal(item.customerId)">
                          {{ item.customerName }}
                        </button>
                      </td>
                      <td class="center-cell"><span [class]="customerTypeChipClass(item.customerType)">{{ customerTypeLabel(item.customerType) }}</span></td>
                      <td class="center-cell"><span class="inline-state" [class.paused-chip]="sessionState(item) === 'Pause'" [class.active-chip]="sessionState(item) === 'En cours'" [class.pending-chip]="sessionState(item) === 'A payer'" [class.paid-chip]="sessionState(item) === 'Terminee'">{{ sessionState(item) }}</span></td>
                      <td class="center-cell">
                        <span *ngIf="item.customerType === 'SUBSCRIBER'; else noCredit">{{ formatRemaining(item) }}</span>
                        <ng-template #noCredit><span class="empty-chip">Aucun</span></ng-template>
                      </td>
                      <td class="center-cell" [class.pending-amount]="isAwaitingPayment(item)">{{ item.totalAmountDue | number:'1.2-2' }} EUR</td>
                      <td class="center-cell">{{ formatElapsed(item) }}</td>
                      <td class="center-cell">{{ item.startedAt | date:'dd/MM/yyyy HH:mm' }}</td>
                      <td class="action-cell"><button type="button" class="ghost" *ngIf="canPauseOrResume(item)" (click)="togglePause(item)">{{ item.paused ? 'Reprendre' : 'Pause' }}</button></td>
                      <td class="action-cell"><button type="button" *ngIf="canStop(item)" (click)="stopSession(item)">Arreter</button></td>
                      <td class="action-cell"><button type="button" *ngIf="canPay(item)" (click)="openPaymentDialog(item)">Payer</button></td>
                      <td class="action-cell"><button type="button" class="ghost" (click)="toggleDetails(item)">Detail</button></td>
                      <td class="action-cell"><button type="button" class="ghost" *ngIf="item.customerType === 'WALK_IN' && canStop(item)" (click)="selectWalkInSession(item)">Convertir</button></td>
                    </tr>
                    <tr class="detail-row" *ngIf="expandedSessionId() === item.sessionId">
                      <td colspan="12">
                        <div class="detail-card" *ngIf="sessionDetails()[item.customerId] as details; else detailLoading">
                          <div class="detail-grid">
                            <section class="stack">
                              <h4>Achats de la session et dettes</h4>
                              <div class="purchase-list" *ngIf="sessionEntries(item, details).length > 0; else noPurchases">
                                <article class="purchase-item compact session-entry" [class.debt-entry]="entry.kind === 'debt'" *ngFor="let entry of sessionEntries(item, details)">
                                  <strong>
                                    <span class="money-alert-icon debt-entry-icon" *ngIf="entry.openDebt" title="Dette ouverte"><span class="bill back"></span><span class="bill front"></span><span class="slash"></span></span>
                                    {{ formatEntryLabel(entry.label) }}
                                  </strong>
                                  <span class="entry-date">{{ entry.occurredAt | date:'dd/MM/yyyy HH:mm' }}</span>
                                  <span class="entry-amount">{{ entry.amount | number:'1.2-2' }} EUR</span>
                                  <span class="entry-actions-cell">
                                    <label class="entry-check" title="Cocher pour mettre en dette ou regler la dette">
                                      <input type="checkbox" [checked]="entry.openDebt" (change)="toggleEntryDebt(item, entry, $event)" />
                                    </label>
                                    <button type="button" class="ghost compact-remove icon-remove" *ngIf="canDeleteSubscriptionEntry(item, entry)" (click)="deleteSubscriptionEntry(item, entry)" aria-label="Supprimer l abonnement" title="Supprimer l abonnement">-</button>
                                  </span>
                                </article>
                              </div>
                            <ng-template #noPurchases><p class="muted">Aucun achat rattache a cette session ni dette ouverte.</p></ng-template>
                            </section>

                            <section class="stack">
                              <h4>Resume abonnement</h4>
                              <p class="muted"><strong>Type :</strong> <span [class]="customerTypeChipClass(details.type)">{{ customerTypeLabel(details.type) }}</span></p>
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
          </section>
        </article>
      </div>

      <div class="modal-backdrop" *ngIf="activePayTargetSession() as session" (click)="closePaymentDialog()">
        <div class="confirm-modal" (click)="$event.stopPropagation()">
          <div class="section-head">
            <div>
              <h3>Regler la session</h3>
              <p class="meta">{{ session.customerName }}</p>
            </div>
            <button type="button" class="ghost" (click)="closePaymentDialog()">Fermer</button>
          </div>
          <div class="stack confirm-body">
            <p class="muted">Le compteur est arrete. Encaisse le montant regle maintenant et finalise la session.</p>
            <div class="confirm-summary">
              <p class="muted"><strong>Cout connexion :</strong> {{ paymentDialogConnectionAmount(session) | number:'1.2-2' }} EUR</p>
              <p class="muted"><strong>Achats de la session :</strong> {{ paymentDialogPurchaseAmount(session) | number:'1.2-2' }} EUR</p>
              <p class="muted" *ngIf="paymentDialogOpenDebtAmount(session) > 0">
                <strong>Dettes ouvertes :</strong> {{ paymentDialogOpenDebtAmount(session) | number:'1.2-2' }} EUR
                <span class="meta">(affichees, non incluses dans ce reglement)</span>
              </p>
              <p class="confirm-total"><strong>A payer :</strong> {{ paymentDialogTotalAmount(session) | number:'1.2-2' }} EUR</p>
              <div *ngIf="paymentSelectedOffers().length > 0">
                <p><strong>Nouveaux abonnements ajoutes :</strong></p>
                <div class="purchase-list">
                  <article class="purchase-item compact session-entry" *ngFor="let offer of paymentSelectedOffers(); let index = index">
                    <strong>{{ offer.name }}</strong>
                    <span class="entry-date">{{ offer.includedMinutes }} min</span>
                    <span class="entry-amount">{{ offer.price | number:'1.2-2' }} EUR</span>
                    <button type="button" class="ghost compact-remove" (click)="removePaymentOffer(index)">Retirer</button>
                  </article>
                </div>
                <p class="muted" *ngIf="paymentResolutionForm.controls.createSubscriptionDebt.value">Ces nouveaux abonnements seront mis en dette.</p>
              </div>
            </div>
            <form class="stack" [formGroup]="paymentResolutionForm">
              <label class="field">
                <span>Montant regle par le client</span>
                <input type="number" min="0" step="0.01" formControlName="amountPaid" />
              </label>
              <p class="muted">Montant conseille : {{ paymentDialogTotalAmount(session) | number:'1.2-2' }} EUR</p>
              <ng-container *ngIf="canAddSubscriptionDuringPayment()">
                <label class="field">
                  <span>Ajouter un abonnement</span>
                  <select formControlName="offerToAddId">
                    <option value="">Choisir une offre</option>
                    <option *ngFor="let offer of offers()" [value]="offer.offerId">{{ offer.name }} - {{ offer.includedMinutes }} min - {{ offer.price | number:'1.2-2' }} EUR</option>
                  </select>
                </label>
                <div class="actions inline-actions">
                  <button type="button" class="ghost" (click)="addPaymentOffer()" [disabled]="!paymentResolutionForm.controls.offerToAddId.value">Ajouter cet abonnement</button>
                </div>
                <p class="muted">Ajoute un ou plusieurs abonnements pendant le reglement si besoin.</p>
                <label class="checkbox" *ngIf="paymentSelectedOffers().length > 0">
                  <input type="checkbox" formControlName="createSubscriptionDebt" />
                  <span>Mettre les nouveaux abonnements en dette</span>
                </label>
                <p class="muted" *ngIf="paymentSelectedOffers().length === 0 && paymentDialogRawOvertimeMinutes(session) === 0">
                  Aucun depassement en cours. Un abonnement ajoute sera simplement ajoute au montant du reglement ou mis en dette.
                </p>
                <p class="muted" *ngIf="paymentSelectedOffers().length > 0 && paymentDialogOvertimeMinutes(session) === 0">
                  Les nouveaux abonnements couvrent completement le depassement.
                </p>
                <p class="muted" *ngIf="paymentSelectedOffers().length > 0 && paymentDialogOvertimeMinutes(session) > 0">
                  Apres ces abonnements, il restera {{ paymentDialogOvertimeMinutes(session) }} min en trop a payer.
                </p>
                <p class="muted" *ngIf="paymentSelectedOffers().length === 0 && paymentDialogRawOvertimeMinutes(session) > 0">
                  Sans nouvel abonnement, le temps en trop restera facture comme depassement.
                </p>
              </ng-container>
            </form>
            <div class="actions">
              <button type="button" class="ghost" (click)="closePaymentDialog()">Annuler</button>
              <button type="button" class="ghost" (click)="confirmPaymentWithInvoice()">Valider et generer la facture PDF</button>
              <button type="button" (click)="confirmPayment()">Valider le paiement</button>
            </div>
          </div>
        </div>
      </div>

      <div class="modal-backdrop" *ngIf="activeCustomerModal() as customer" (click)="closeCustomerModal()">
        <div class="confirm-modal customer-modal" (click)="$event.stopPropagation()">
          <div class="section-head">
            <div>
              <h3>Fiche client</h3>
              <p class="meta">{{ customer.name }}</p>
            </div>
            <button type="button" class="ghost" (click)="closeCustomerModal()">Fermer</button>
          </div>

          <div class="stack confirm-body">
            <div class="facts customer-facts">
              <p><strong>Type :</strong> <span [class]="customerTypeChipClass(customer.type)">{{ customerTypeLabel(customer.type) }}</span></p>
              <p><strong>Abonnements :</strong> {{ customer.currentSubscriptionLabel ?? 'Aucun' }}</p>
              <p *ngIf="customer.type === 'SUBSCRIBER'" class="muted">Les abonnements se cumulent.</p>
              <p><strong>Credit disponible :</strong> {{ customer.remainingMinutes }} min</p>
              <p><strong>Total paye sur tous les achats :</strong> {{ totalPaidPurchases(customer) | number:'1.2-2' }} EUR</p>
              <p><strong>Total paye sur forfaits et produits :</strong> {{ totalPaidSales(customer) | number:'1.2-2' }} EUR</p>
              <p *ngIf="customer.debts.length > 0" class="debt-summary"><strong>Dettes :</strong> <span class="money-alert-icon debt-indicator" title="Dettes ouvertes"><span class="bill back"></span><span class="bill front"></span><span class="slash"></span></span></p>
            </div>

            <div class="stack">
              <h4>Achats du client</h4>
              <table class="table compact" *ngIf="customerPurchaseEntries(customer).length > 0; else noCustomerPurchases">
                <thead>
                  <tr><th>Quand</th><th>Type</th><th>Detail</th><th>Total</th></tr>
                </thead>
                <tbody>
                  <tr *ngFor="let entry of visibleCustomerPurchaseEntries(customer); let index = index" [class.debt-row]="entry.isDebt">
                    <td>{{ entry.occurredAt | date:'dd/MM/yyyy HH:mm' }}</td>
                    <td>{{ entry.type }}</td>
                    <td>{{ entry.detail }}</td>
                    <td class="customer-entry-total">
                      <strong>{{ entry.total | number:'1.2-2' }} EUR</strong>
                      <button
                        type="button"
                        class="ghost compact-remove entry-expand-toggle"
                        *ngIf="showMoreCustomerPurchasesButton(customer, index)"
                        (click)="showMoreCustomerPurchases()"
                        aria-label="Afficher 10 achats de plus"
                        title="Afficher 10 achats de plus"
                      >+</button>
                      <button
                        type="button"
                        class="ghost compact-remove entry-expand-toggle"
                        *ngIf="showLessCustomerPurchasesButton(customer, index)"
                        (click)="showLessCustomerPurchases()"
                        aria-label="Replier la liste des achats"
                        title="Replier la liste des achats"
                      >-</button>
                    </td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr>
                    <td colspan="3"><strong>Total paye</strong></td>
                    <td><strong>{{ totalPaidPurchases(customer) | number:'1.2-2' }} EUR</strong></td>
                  </tr>
                </tfoot>
              </table>
              <ng-template #noCustomerPurchases><p class="muted">Aucun achat ni dette ouverte.</p></ng-template>
            </div>
          </div>
        </div>
      </div>
      <div class="modal-backdrop" *ngIf="saleTargetSession() as session" (click)="closeSaleModal()">
        <div class="confirm-modal" (click)="$event.stopPropagation()">
          <div class="section-head">
            <div>
              <h3>{{ saleModalType() === 'subscription' ? 'Vendre un abonnement' : 'Vendre un produit' }}</h3>
              <p class="meta">{{ session.customerName }} - <span [class]="customerTypeChipClass(session.customerType)">{{ customerTypeLabel(session.customerType) }}</span></p>
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
  private readonly logger = inject(AppLoggerService);
  private readonly fb = inject(FormBuilder);

  readonly current = signal<CafeSession[]>([]);
  readonly day = signal<CafeSession[]>([]);
  readonly subscriberResults = signal<Customer[]>([]);
  readonly offers = signal<SubscriptionOffer[]>([]);
  readonly products = signal<Product[]>([]);
  readonly pricingTiers = signal<ConnectionPricingTier[]>([]);
  readonly selectedWalkInSession = signal<CafeSession | null>(null);
  readonly saleTargetSession = signal<CafeSession | null>(null);
  readonly saleModalType = signal<'product' | 'subscription' | null>(null);
  readonly payTargetSession = signal<CafeSession | null>(null);
  readonly customerModalId = signal<string | null>(null);
  readonly customerPurchasesVisibleCount = signal(10);
  readonly paymentOfferIds = signal<string[]>([]);
  readonly expandedSessionId = signal<string | null>(null);
  readonly expandedDaySessionId = signal<string | null>(null);
  readonly sessionDetails = signal<Record<string, CustomerDetails>>({});
  readonly sessionObservedAt = signal<Record<string, number>>({});
  readonly error = signal('');
  readonly notice = signal('');
  readonly openSection = signal<'walk-in' | 'subscriber-existing' | 'subscriber-new' | null>('walk-in');
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
  readonly paymentResolutionForm = this.fb.nonNullable.group({ offerToAddId: [''], createSubscriptionDebt: [false], amountPaid: [0, [Validators.required, Validators.min(0)]] });
  readonly currentFilters = this.fb.nonNullable.group({ term: [''], startedAfter: [''] });
  readonly dayFilters = this.fb.nonNullable.group({ term: [''], startedAfter: [''] });

  readonly filteredCurrent = computed(() => this.filterSessions(this.current(), this.currentTerm(), this.currentStartedAfter()));
  readonly filteredDay = computed(() => this.filterSessions(this.day().filter((session) => session.paid), this.dayTerm(), this.dayStartedAfter()));
  readonly activePayTargetSession = computed(() => {
    const target = this.payTargetSession();
    if (!target) {
      return null;
    }
    return this.current().find((session) => session.sessionId === target.sessionId) ?? target;
  });
  readonly activeCustomerModal = computed(() => {
    const customerId = this.customerModalId();
    if (!customerId) {
      return null;
    }
    return this.sessionDetails()[customerId] ?? null;
  });
  constructor() {
    this.logger.info('sessions-ui', 'Sessions page initialized');
    this.reload();
    this.loadOffers();
    this.salesApi.pricing().subscribe({
      next: (pricing) => this.pricingTiers.set(pricing.tiers),
      error: () => this.pricingTiers.set([]),
    });
    this.productsApi.search('', 'ACTIVE', '').subscribe({
      next: (value) => {
        this.products.set(value);
        this.logger.debug('sessions-ui', 'Products catalog loaded for session flows', { productCount: value.length });
      },
      error: (error: HttpErrorResponse) => {
        this.logger.warn('sessions-ui', 'Unable to preload products catalog', error);
      },
    });

    this.subscriberSearchForm.controls.term.valueChanges.pipe(
      map((value) => value.trim()),
      debounceTime(150),
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe((term) => {
      this.logger.debug('sessions-ui', 'Subscriber autocomplete input updated', { termLength: term.length });
      this.autocompleteSubscribers(term);
    });

    this.currentFilters.controls.term.valueChanges.pipe(
      map((value) => value.trim()),
      debounceTime(120),
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe((value) => {
      this.currentTerm.set(value);
      this.logger.debug('sessions-ui', 'Current sessions filters updated', { termLength: value.length, startedAfter: this.currentStartedAfter() || null });
    });

    this.currentFilters.controls.startedAfter.valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe((value) => {
      this.currentStartedAfter.set(value);
      this.logger.debug('sessions-ui', 'Current sessions date filter updated', { startedAfter: value || null, termLength: this.currentTerm().length });
    });

    this.dayFilters.controls.term.valueChanges.pipe(
      map((value) => value.trim()),
      debounceTime(120),
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe((value) => {
      this.dayTerm.set(value);
      this.dayPage.set(1);
      this.logger.debug('sessions-ui', 'Day sessions filters updated', { termLength: value.length, startedAfter: this.dayStartedAfter() || null });
    });

    this.dayFilters.controls.startedAfter.valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe((value) => {
      this.dayStartedAfter.set(value);
      this.dayPage.set(1);
      this.logger.debug('sessions-ui', 'Day sessions date filter updated', { startedAfter: value || null, termLength: this.dayTerm().length });
    });

    this.paymentResolutionForm.controls.createSubscriptionDebt.valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe(() => {
      this.syncPaymentDialogAmount();
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
      next: (offers) => {
        this.offers.set(offers);
        this.logger.debug('sessions-ui', 'Subscription offers loaded for session flows', { offerCount: offers.length });
      },
      error: (error: HttpErrorResponse) => {
        this.logger.warn('sessions-ui', 'Subscription offers loading failed', error);
        this.error.set(error.error?.message ?? 'Chargement des offres impossible');
      },
    });
  }

  reload(): void {
    this.logger.debug('sessions-ui', 'Reloading sessions page datasets');
    this.api.current().subscribe({
      next: (value) => {
        this.current.set(value.sessions);
        this.sessionObservedAt.set(Object.fromEntries(value.sessions.map((session) => [session.sessionId, Date.now()])));
        this.error.set('');
        this.logger.info('sessions-ui', 'Current sessions loaded', { count: value.sessions.length });
      },
      error: (error: HttpErrorResponse) => {
        this.logger.warn('sessions-ui', 'Current sessions loading failed', error);
        this.error.set(error.error?.message ?? 'Chargement des sessions en cours impossible');
      },
    });
  }

  restartSessionsDay(): void {
    this.api.restartDay().subscribe({
      next: (value) => {
        this.notice.set(
          value.archivedSessions > 0
            ? `${value.archivedSessions} session(s) ont ete cloturee(s) et passee(s) en dette de fin de journee.`
            : 'Aucune session a cloturer pour la fin de journee.'
        );
        this.reload();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, 'Redemarrage des sessions impossible')),
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
        this.logger.debug('sessions-ui', 'Subscriber autocomplete results received', {
          termLength: term.length,
          resultCount: customers.length,
          alreadyActiveCount: customers.filter((customer) => this.hasCurrentSession(customer.customerId)).length,
        });
      },
      error: (error: HttpErrorResponse) => {
        this.logger.warn('sessions-ui', 'Subscriber autocomplete failed', { termLength: term.length, error });
        this.error.set(error.error?.message ?? 'Recherche des abonnes impossible');
      },
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
    if (this.hasCurrentSession(customer.customerId)) {
      this.error.set('Cet abonne a deja une session en cours.');
      this.logger.warn('sessions-ui', 'Blocked duplicate subscriber session start', {
        customerId: customer.customerId,
        customerName: customer.name,
      });
      return;
    }
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
    const customerName = payload.name.trim();
    const subscriptionOfferId = payload.subscriptionOfferId;
    this.customersApi.create({ name: customerName, type: 'WALK_IN' }).pipe(
      switchMap((customer) =>
        this.api.start({ customerId: customer.customerId }).pipe(
          switchMap((session) =>
            this.customersApi.convert(customer.customerId, {
              subscriptionOfferId,
              deductCurrentSession: false,
              sessionId: session.sessionId,
            }).pipe(map(() => customer)),
          ),
        ),
      ),
    ).subscribe({
      next: () => {
        this.newSubscriberForm.reset({ name: '', subscriptionOfferId: '' });
        this.notice.set('');
        this.reload();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Creation de l abonne impossible'),
    });
  }

  openPaymentDialog(session: CafeSession): void {
    const suggestedAmount = Number(Number(session.totalAmountDue ?? 0).toFixed(2));
    this.payTargetSession.set(session);
    this.paymentOfferIds.set([]);
    this.paymentResolutionForm.reset({ offerToAddId: '', createSubscriptionDebt: false, amountPaid: suggestedAmount });
    this.loadCustomerDetails(session.customerId);
    this.error.set('');
    this.notice.set('');
  }

  closePaymentDialog(): void {
    this.payTargetSession.set(null);
    this.paymentOfferIds.set([]);
    this.paymentResolutionForm.reset({ offerToAddId: '', createSubscriptionDebt: false, amountPaid: 0 });
  }

  openCustomerModal(customerId: string): void {
    this.customerModalId.set(customerId);
    this.customerPurchasesVisibleCount.set(10);
    this.loadCustomerDetails(customerId);
    this.error.set('');
  }

  closeCustomerModal(): void {
    this.customerModalId.set(null);
    this.customerPurchasesVisibleCount.set(10);
  }

  stopSession(session: CafeSession): void {
    this.api.stop(session.sessionId).subscribe({
      next: (stoppedSession) => {
        this.notice.set(`Session arretee. Montant a payer : ${Number(stoppedSession.totalAmountDue ?? 0).toFixed(2)} EUR.`);
        this.current.update((sessions) => sessions.map((item) => item.sessionId === stoppedSession.sessionId ? stoppedSession : item));
        this.sessionObservedAt.update((observedAt) => ({ ...observedAt, [stoppedSession.sessionId]: Date.now() }));
        this.refreshCustomerDetails(session.customerId);
        this.reload();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, 'Arret impossible')),
    });
  }

  confirmPayment(): void {
    this.submitPayment(false);
  }

  confirmPaymentWithInvoice(): void {
    this.submitPayment(true);
  }

  private submitPayment(withInvoice: boolean): void {
    const session = this.activePayTargetSession();
    if (!session) {
      return;
    }
    if (this.paymentResolutionForm.invalid) {
      this.paymentResolutionForm.markAllAsTouched();
      return;
    }
    const selectedOffers = this.paymentSelectedOffers();
    const createSubscriptionDebt = this.paymentResolutionForm.controls.createSubscriptionDebt.value;
    const amountPaid = Number(this.paymentResolutionForm.controls.amountPaid.value ?? 0);
    const requiredAmount = this.paymentDialogTotalAmount(session);

    if (amountPaid + 0.001 < requiredAmount) {
      this.error.set(`Le montant regle ne couvre pas le total a payer (${requiredAmount.toFixed(2)} EUR).`);
      return;
    }

    const payload = {
      amountPaid,
      subscriptionOfferIds: selectedOffers.map((offer) => offer.offerId),
      createSubscriptionDebt,
    };

    if (withInvoice) {
      this.api.payWithInvoice(session.sessionId, payload).subscribe({
        next: (response: HttpResponse<Blob>) => {
          this.downloadPdf(response);
          this.onPaymentSuccess(session, selectedOffers, createSubscriptionDebt, true);
        },
        error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, 'Paiement impossible')),
      });
      return;
    }

    this.api.pay(session.sessionId, payload).subscribe({
      next: () => {
        this.onPaymentSuccess(session, selectedOffers, createSubscriptionDebt, false);
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, 'Paiement impossible')),
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
    this.salesApi.productSale({ customerId: session.customerId, sessionId: session.sessionId, lines: [{ productId, quantity }], createDebt }).subscribe({
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
    this.salesApi.subscriptionSale({ customerId: session.customerId, sessionId: session.sessionId, subscriptionOfferId, createDebt }).subscribe({
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

  changeDayPage(step: number): void {
    this.dayPage.set(Math.max(1, this.dayPage() + step));
  }

  todayPurchases(purchases: CustomerPurchase[]): CustomerPurchase[] {
    const today = new Date().toDateString();
    return purchases.filter((purchase) => new Date(purchase.soldAt).toDateString() === today);
  }

  sessionEntries(session: CafeSession, details: CustomerDetails): SessionDetailEntry[] {
    const sessionStart = new Date(session.startedAt).getTime();
    const sessionEnd = session.endedAt ? new Date(session.endedAt).getTime() : Number.POSITIVE_INFINITY;
    const uniquePurchases = Array.from(new Map(details.purchases.map((purchase) => [purchase.saleId, purchase] as const)).values());
    const allPurchases = uniquePurchases.map((purchase) => ({
      kind: 'purchase' as const,
      saleIds: [purchase.saleId],
      debtIds: [],
      sessionId: purchase.sessionId,
      label: purchase.label,
      debtLabel: purchase.debtLabel,
      occurredAt: purchase.soldAt,
      amount: purchase.totalAmount,
      openDebt: purchase.openDebt,
    }));
    const sessionPurchases = allPurchases
      .filter((purchase) => {
        if (purchase.sessionId) {
          return purchase.sessionId === session.sessionId;
        }
        const occurredAt = new Date(purchase.occurredAt).getTime();
        return occurredAt >= sessionStart && occurredAt <= sessionEnd;
      })
      .map((purchase) => ({
        kind: 'purchase' as const,
        saleIds: purchase.saleIds,
        debtIds: [],
        label: purchase.label,
        debtLabel: purchase.debtLabel,
        occurredAt: purchase.occurredAt,
        amount: purchase.amount,
        openDebt: purchase.openDebt,
      }));
    const openDebts = details.debts
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

    const mergedPurchases = sessionPurchases.map((purchase) => {
      const matchedDebt = openDebts.find((debt) => this.isDebtLinkedToPurchase(debt, purchase));
      return {
        ...purchase,
        kind: purchase.openDebt || matchedDebt ? 'debt' as const : 'purchase' as const,
        debtIds: matchedDebt?.debtIds ?? [],
        openDebt: purchase.openDebt || !!matchedDebt,
      };
    });
    const remainingDebts = openDebts.filter((debt) => !sessionPurchases.some((purchase) => this.isDebtLinkedToPurchase(debt, purchase)));
    const relabeledDebts = remainingDebts.map((debt) => {
      const matchedPurchase = allPurchases.find((purchase) => this.isDebtLinkedToPurchase(debt, purchase));
      return matchedPurchase
        ? { ...debt, label: matchedPurchase.label, saleIds: matchedPurchase.saleIds, debtLabel: matchedPurchase.debtLabel }
        : debt;
    });

    return [...mergedPurchases, ...relabeledDebts]
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
    return this.formatSeconds(this.liveDisplayRemainingSeconds(session));
  }

  sessionState(session: CafeSession): 'En cours' | 'Pause' | 'A payer' | 'Terminee' {
    if (session.paid) {
      return 'Terminee';
    }
    if (session.endedAt) {
      return 'A payer';
    }
    if (session.paused) {
      return 'Pause';
    }
    return 'En cours';
  }

  customerTypeLabel(type: string): string {
    return type === 'SUBSCRIBER' || type.toLowerCase().includes('abonn') ? 'Abonne' : 'Client';
  }

  customerTypeChipClass(type: string): string {
    return this.customerTypeLabel(type) === 'Abonne' ? 'type-chip subscriber-chip' : 'type-chip walk-in-chip';
  }

  canAddSubscriptionDuringPayment(): boolean {
    return this.offers().length > 0;
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

  customerPurchaseEntries(customer: CustomerDetails): Array<{ occurredAt: string; type: string; detail: string; total: number; isDebt: boolean }> {
    const uniquePurchases = Array.from(new Map(customer.purchases.map((purchase) => [purchase.saleId, purchase] as const)).values());
    const paidPurchases = uniquePurchases
      .filter((purchase) => !purchase.openDebt)
      .map((purchase) => ({
        occurredAt: purchase.soldAt,
        type: this.purchaseTypeLabel(purchase.type),
        detail: purchase.label,
        total: purchase.totalAmount,
        isDebt: false,
      }));
    const openDebts = customer.debts
      .filter((debt) => debt.status === 'OPEN')
      .map((debt) => ({
        occurredAt: debt.createdAt,
        type: this.debtTypeLabel(debt.label),
        detail: debt.label,
        total: debt.amount,
        isDebt: true,
      }));

    return [...paidPurchases, ...openDebts]
      .sort((left, right) => new Date(right.occurredAt).getTime() - new Date(left.occurredAt).getTime());
  }

  visibleCustomerPurchaseEntries(customer: CustomerDetails): Array<{ occurredAt: string; type: string; detail: string; total: number; isDebt: boolean }> {
    return this.customerPurchaseEntries(customer).slice(0, this.customerPurchasesVisibleCount());
  }

  showMoreCustomerPurchases(): void {
    this.customerPurchasesVisibleCount.update((count) => count + 10);
  }

  showLessCustomerPurchases(): void {
    this.customerPurchasesVisibleCount.set(10);
  }

  showMoreCustomerPurchasesButton(customer: CustomerDetails, index: number): boolean {
    const visibleCount = this.visibleCustomerPurchaseEntries(customer).length;
    return index === visibleCount - 1 && visibleCount < this.customerPurchaseEntries(customer).length;
  }

  showLessCustomerPurchasesButton(customer: CustomerDetails, index: number): boolean {
    const visibleCount = this.visibleCustomerPurchaseEntries(customer).length;
    return index === visibleCount - 1 && this.customerPurchasesVisibleCount() > 10;
  }

  private hasCurrentSession(customerId: string): boolean {
    return this.current().some((session) => session.customerId === customerId);
  }

  canPauseOrResume(session: CafeSession): boolean {
    return !session.endedAt;
  }

  canStop(session: CafeSession): boolean {
    return !session.endedAt;
  }

  canPay(session: CafeSession): boolean {
    return !!session.endedAt && !session.paid;
  }

  isAwaitingPayment(session: CafeSession): boolean {
    return this.sessionState(session) === 'A payer';
  }

  hasOpenDebt(session: CafeSession): boolean {
    return Number(session.openDebtAmount ?? 0) > 0.001;
  }

  addPaymentOffer(): void {
    const offerId = this.paymentResolutionForm.controls.offerToAddId.value;
    if (!offerId) {
      this.error.set('Choisis un abonnement a ajouter.');
      return;
    }
    this.paymentOfferIds.update((offerIds) => [...offerIds, offerId]);
    this.paymentResolutionForm.patchValue({ offerToAddId: '' });
    this.syncPaymentDialogAmount();
    this.error.set('');
  }

  removePaymentOffer(index: number): void {
    this.paymentOfferIds.update((offerIds) => offerIds.filter((_, currentIndex) => currentIndex !== index));
    this.syncPaymentDialogAmount();
  }

  paymentDialogPurchaseAmount(session: CafeSession): number {
    return Math.max(0, Number(session.totalAmountDue ?? 0) - this.paymentDialogBaseConnectionAmount(session));
  }

  paymentDialogOpenDebtAmount(session: CafeSession): number {
    return Math.max(0, Number(session.openDebtAmount ?? 0));
  }

  paymentSelectedOffers(): SubscriptionOffer[] {
    return this.paymentOfferIds()
      .map((offerId) => this.offers().find((offer) => offer.offerId === offerId) ?? null)
      .filter((offer): offer is SubscriptionOffer => offer != null);
  }

  paymentDialogRawOvertimeMinutes(session: CafeSession): number {
    return Math.max(0, Number(session.consumedMinutes ?? 0) - Number(session.remainingMinutes ?? 0));
  }

  paymentDialogOvertimeMinutes(session: CafeSession): number {
    const selectedOfferMinutes = this.paymentSelectedOffers().reduce((total, offer) => total + offer.includedMinutes, 0);
    return Math.max(0, Number(session.consumedMinutes ?? 0) - (Number(session.remainingMinutes ?? 0) + selectedOfferMinutes));
  }

  paymentDialogConnectionAmount(session: CafeSession): number {
    if (this.paymentSelectedOffers().length === 0 && this.customerTypeLabel(session.customerType) !== 'Abonne') {
      return this.paymentDialogBaseConnectionAmount(session);
    }
    return this.calculateConnectionPrice(this.paymentDialogOvertimeMinutes(session));
  }

  paymentDialogSubscriptionImmediateAmount(): number {
    const selectedOffers = this.paymentSelectedOffers();
    if (selectedOffers.length === 0 || this.paymentResolutionForm.controls.createSubscriptionDebt.value) {
      return 0;
    }
    return selectedOffers.reduce((total, offer) => total + Number(offer.price ?? 0), 0);
  }

  paymentDialogTotalAmount(session: CafeSession): number {
    return this.paymentDialogConnectionAmount(session) + this.paymentDialogPurchaseAmount(session) + this.paymentDialogSubscriptionImmediateAmount();
  }

  private paymentDialogBaseConnectionAmount(session: CafeSession): number {
    return Number(session.calculatedPrice ?? 0);
  }

  private syncPaymentDialogAmount(): void {
    const session = this.activePayTargetSession();
    if (!session) {
      return;
    }
    const nextAmount = Number(this.paymentDialogTotalAmount(session).toFixed(2));
    this.paymentResolutionForm.controls.amountPaid.setValue(nextAmount);
  }

  private downloadPdf(response: HttpResponse<Blob>): void {
    if (!response.body) {
      return;
    }
    const blobUrl = URL.createObjectURL(response.body);
    const link = document.createElement('a');
    link.href = blobUrl;
    link.download = this.api.fileName(response);
    link.click();
    URL.revokeObjectURL(blobUrl);
  }

  private onPaymentSuccess(session: CafeSession, selectedOffers: SubscriptionOffer[], createSubscriptionDebt: boolean, withInvoice: boolean): void {
    const connection = this.paymentDialogConnectionAmount(session);
    const purchases = this.paymentDialogPurchaseAmount(session);
    const subscriptionPaid = !createSubscriptionDebt
      ? selectedOffers.reduce((total, offer) => total + Number(offer.price ?? 0), 0)
      : 0;
    const dayTotal = connection + purchases + subscriptionPaid;
    const subscriptionNotice = createSubscriptionDebt && selectedOffers.length > 0
      ? selectedOffers.length > 1
        ? 'Paiement valide. Nouveaux abonnements ajoutes en dette.'
        : 'Paiement valide. Nouvel abonnement ajoute en dette.'
      : 'Paiement valide.';
    this.notice.set(
      `${subscriptionNotice}${withInvoice ? ' Facture PDF generee.' : ''} Total paye : ${dayTotal.toFixed(2)} EUR.`
    );
    this.closePaymentDialog();
    this.closeSaleModal();
    this.refreshCustomerDetails(session.customerId);
    this.reload();
  }

  toggleEntryDebt(session: CafeSession, entry: SessionDetailEntry, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const requests = checked
      ? entry.saleIds.map((saleId) => this.customersApi.createDebtFromSale(saleId))
      : entry.debtIds.map((debtId) => this.customersApi.reattachDebtToSession(debtId, session.sessionId));

    if (requests.length === 0) {
      (event.target as HTMLInputElement).checked = entry.openDebt;
      this.error.set('Action impossible sur cette ligne.');
      this.logger.warn('sessions-ui', 'Debt toggle ignored because no actionable request was produced', {
        label: entry.label,
        saleCount: entry.saleIds.length,
        debtCount: entry.debtIds.length,
      });
      return;
    }

    this.logger.info('sessions-ui', 'Toggling debt state from session detail', {
      action: checked ? 'create-debt' : 'reattach-to-session',
      sessionId: session.sessionId,
      label: entry.label,
      saleCount: entry.saleIds.length,
      debtCount: entry.debtIds.length,
    });
    forkJoin(requests).subscribe({
      next: () => {
        this.notice.set(checked ? 'Ligne passee en dette.' : 'Dette retransformee en achat de la session.');
        this.logger.info('sessions-ui', 'Debt state updated from session detail', {
          action: checked ? 'create-debt' : 'reattach-to-session',
          requestCount: requests.length,
        });
        this.refreshAfterDebtChange();
      },
      error: (error: HttpErrorResponse) => {
        (event.target as HTMLInputElement).checked = entry.openDebt;
        this.logger.warn('sessions-ui', 'Debt state update failed from session detail', {
          action: checked ? 'create-debt' : 'reattach-to-session',
          requestCount: requests.length,
          error,
        });
        this.error.set(this.resolveHttpError(error, checked ? 'Creation de dette impossible' : 'Reintegration de la dette impossible'));
      },
    });
  }

  canDeleteSubscriptionEntry(session: CafeSession, entry: SessionDetailEntry): boolean {
    return !!session.endedAt && !session.paid && entry.saleIds.length === 1 && !!entry.debtLabel && entry.debtLabel.startsWith('Vente abonnement du ');
  }

  deleteSubscriptionEntry(session: CafeSession, entry: SessionDetailEntry): void {
    const saleId = entry.saleIds[0];
    if (!saleId) {
      this.error.set('Suppression impossible pour cette ligne.');
      return;
    }
    this.salesApi.deleteSubscriptionSale(saleId).subscribe({
      next: () => {
        this.notice.set('Abonnement retire de la session.');
        this.refreshCustomerDetails(session.customerId);
        this.reload();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveHttpError(error, 'Suppression de l abonnement impossible')),
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

  private isDebtLinkedToPurchase(debt: SessionDetailEntry, purchase: SessionDetailEntry): boolean {
    if (debt.kind !== 'debt' || purchase.kind !== 'purchase') {
      return false;
    }
    return !!debt.rawLabel && !!purchase.debtLabel
      && debt.rawLabel === purchase.debtLabel
      && Math.abs(debt.amount - purchase.amount) <= 0.001;
  }

  private purchaseTypeLabel(type: CustomerPurchase['type']): string {
    switch (type) {
      case 'PRODUCTS':
        return 'Produit';
      case 'SUBSCRIPTION':
        return 'Abonnement';
      case 'CONNECTION_TIME':
        return 'Connexion';
      default:
        return type;
    }
  }

  private debtTypeLabel(label: string): string {
    const normalized = label.toLowerCase();
    if (normalized.startsWith('vente produits')) {
      return 'Produit';
    }
    if (normalized.startsWith('vente abonnements') || normalized.startsWith('vente abonnement')) {
      return 'Abonnement';
    }
    if (normalized.startsWith('session du') || normalized.startsWith('depassement abonnement du')) {
      return 'Connexion';
    }
    return 'Dette';
  }

  private liveConsumedSeconds(session: CafeSession): number {
    if (session.endedAt) {
      return Math.max(0, session.consumedSeconds);
    }
    const observedAt = this.sessionObservedAt()[session.sessionId] ?? this.now();
    const extraSeconds = session.paused ? 0 : Math.max(0, Math.floor((this.now() - observedAt) / 1000));
    return Math.max(0, session.consumedSeconds + extraSeconds);
  }

  private liveRemainingSeconds(session: CafeSession): number {
    if (session.endedAt) {
      return Math.max(0, session.remainingMinutes * 60 - Math.max(0, session.consumedSeconds));
    }
    return Math.max(0, session.remainingMinutes * 60 - this.liveConsumedSeconds(session));
  }

  private liveDisplayRemainingSeconds(session: CafeSession): number {
    const displayRemainingMinutes = Number(session.displayRemainingMinutes ?? session.remainingMinutes ?? 0);
    if (session.endedAt) {
      return Math.max(0, displayRemainingMinutes * 60);
    }
    return Math.max(0, displayRemainingMinutes * 60 - this.liveConsumedSeconds(session));
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

  private calculateConnectionPrice(minutes: number): number {
    if (minutes <= 0) {
      return 0;
    }
    const tiers = this.pricingTiers();
    if (tiers.length === 0) {
      return 0;
    }
    const sortedTiers = [...tiers].sort((left, right) => left.durationMinutes - right.durationMinutes);
    const maxDuration = sortedTiers[sortedTiers.length - 1].durationMinutes;
    const limit = minutes + maxDuration;
    const prices: Array<number | null> = Array.from({ length: limit + 1 }, () => null);
    prices[0] = 0;

    for (let coveredMinutes = 1; coveredMinutes <= limit; coveredMinutes += 1) {
      let bestPrice: number | null = null;
      for (const tier of sortedTiers) {
        const previousMinutes = coveredMinutes - tier.durationMinutes;
        if (previousMinutes < 0) {
          continue;
        }
        const previousPrice = prices[previousMinutes];
        if (previousPrice == null) {
          continue;
        }
        const candidate = previousPrice + Number(tier.price);
        if (bestPrice == null || candidate < bestPrice) {
          bestPrice = candidate;
        }
      }
      prices[coveredMinutes] = bestPrice;
    }

    let bestPrice: number | null = null;
    for (let coveredMinutes = minutes; coveredMinutes <= limit; coveredMinutes += 1) {
      const candidate = prices[coveredMinutes];
      if (candidate != null && (bestPrice == null || candidate < bestPrice)) {
        bestPrice = candidate;
      }
    }
    return bestPrice ?? 0;
  }

  private paymentDialogDetails(): CustomerDetails | null {
    const session = this.activePayTargetSession();
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
    if (currentSessionId) {
      const session = this.current().find((item) => item.sessionId === currentSessionId);
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
    this.logger.debug('sessions-ui', 'Loading customer detail for session drawer', { customerId });
    this.customersApi.get(customerId).subscribe({
      next: (details) => {
        this.sessionDetails.set({
          ...this.sessionDetails(),
          [customerId]: details,
        });
        const currentSessionId = this.expandedSessionId();
        const currentSession = currentSessionId ? this.current().find((item) => item.sessionId === currentSessionId) ?? null : null;
        const entries = currentSession ? this.sessionEntries(currentSession, details) : [];
        this.logger.debug('sessions-ui', 'Session detail normalized for session entries', {
          customerId,
          sessionId: currentSession?.sessionId,
          purchaseCount: details.purchases.length,
          debtCount: details.debts.length,
          normalizedEntryCount: entries.length,
          openDebtCount: entries.filter((entry) => entry.openDebt).length,
        });
      },
      error: (error: HttpErrorResponse) => {
        this.logger.warn('sessions-ui', 'Customer detail loading failed for session drawer', { customerId, error });
        this.error.set(this.resolveHttpError(error, 'Chargement du detail client impossible'));
      },
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
