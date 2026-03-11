import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, map } from 'rxjs';

import type { Customer } from '../../customers/models/customer.models';
import { CustomersApiService } from '../../customers/services/customers-api.service';
import type { SubscriptionOffer } from '../../subscriptions/models/subscription-offer.models';
import { SubscriptionOffersApiService } from '../../subscriptions/services/subscription-offers-api.service';
import type { CafeSession } from '../models/session.models';
import { SessionDisplaySettingsService } from '../services/session-display-settings.service';
import { SessionsApiService } from '../services/sessions-api.service';

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

      <div class="grid">
        <article class="panel panel-left stack">
          <section class="mode-card" [class.open]="openSection() === 'walk-in'">
            <button type="button" class="mode-toggle" (click)="setSection('walk-in')">
              <span>Client journalier</span>
              <small>{{ openSection() === 'walk-in' ? 'Refermer' : 'Ouvrir' }}</small>
            </button>
            <div class="mode-body">
              <form [formGroup]="walkInForm" (ngSubmit)="startWalkIn()" class="stack">
                <label class="field">
                  <span>Nom du client journalier</span>
                  <input formControlName="customerName" placeholder="Ex. Samir" />
                </label>
                <button type="submit">Demarrer la session</button>
              </form>
            </div>
          </section>

          <section class="mode-card" [class.open]="openSection() === 'subscriber-existing'">
            <button type="button" class="mode-toggle" (click)="setSection('subscriber-existing')">
              <span>Client abonne existant</span>
              <small>{{ openSection() === 'subscriber-existing' ? 'Refermer' : 'Ouvrir' }}</small>
            </button>
            <div class="mode-body stack">
              <label class="field">
                <span>Recherche d'un abonne</span>
                <input [formControl]="subscriberSearchForm.controls.term" placeholder="Nom du client abonne" />
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
              <span>Nouveau client abonne</span>
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
        </article>

        <article class="panel panel-right stack">
          <section class="stack">
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
              <tr><th>Client</th><th>Type</th><th>Credit</th><th>Debut</th><th></th><th></th></tr>
              <tr *ngFor="let item of pagedCurrent()">
                <td>{{ item.customerName }}</td>
                <td>{{ item.customerType === 'SUBSCRIBER' ? 'Abonne' : 'Journalier' }}</td>
                <td>{{ item.customerType === 'SUBSCRIBER' ? (item.remainingMinutes + ' min') : '-' }}</td>
                <td>{{ item.startedAt | date:'short' }}</td>
                <td><button type="button" (click)="stop(item.sessionId)">Arreter</button></td>
                <td><button type="button" class="ghost" *ngIf="item.customerType === 'WALK_IN'" (click)="selectWalkInSession(item)">Convertir</button></td>
              </tr>
            </table>
            <div class="pager" *ngIf="currentPageCount() > 1">
              <button type="button" class="ghost" (click)="changeCurrentPage(-1)" [disabled]="currentPage() === 1">Precedent</button>
              <span>Page {{ currentPage() }} / {{ currentPageCount() }}</span>
              <button type="button" class="ghost" (click)="changeCurrentPage(1)" [disabled]="currentPage() === currentPageCount()">Suivant</button>
            </div>
          </section>

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
              <tr><th>Client</th><th>Type</th><th>Duree</th><th>Montant</th><th>Debut</th></tr>
              <tr *ngFor="let item of pagedDay()">
                <td>{{ item.customerName }}</td>
                <td>{{ item.customerType === 'SUBSCRIBER' ? 'Abonne' : 'Journalier' }}</td>
                <td>{{ item.consumedMinutes }} min</td>
                <td>{{ item.calculatedPrice | number:'1.2-2' }} EUR</td>
                <td>{{ item.startedAt | date:'short' }}</td>
              </tr>
            </table>
            <div class="pager" *ngIf="dayPageCount() > 1">
              <button type="button" class="ghost" (click)="changeDayPage(-1)" [disabled]="dayPage() === 1">Precedent</button>
              <span>Page {{ dayPage() }} / {{ dayPageCount() }}</span>
              <button type="button" class="ghost" (click)="changeDayPage(1)" [disabled]="dayPage() === dayPageCount()">Suivant</button>
            </div>
          </section>
        </article>
      </div>
    </section>
  `,
  styles: `.page,.stack{display:grid;gap:1rem}.hero{display:grid;gap:.75rem}.hero-actions{display:flex;justify-content:space-between;gap:1rem;align-items:flex-start;flex-wrap:wrap}.eyebrow{margin:0;color:#0f766e;text-transform:uppercase;letter-spacing:.12em;font-size:.72rem}.lede{margin:0;max-width:56rem;color:#475569}.ghost-link{text-decoration:none;background:#fff;color:#334155;border:1px solid #cbd5e1;padding:.8rem 1rem;border-radius:.9rem}.grid{display:grid;grid-template-columns:minmax(320px, 430px) minmax(0, 1fr);gap:1.25rem;align-items:start}.panel{background:linear-gradient(180deg,#f8fafc,#ffffff);padding:1.25rem;border-radius:1.35rem;box-shadow:0 18px 40px rgba(15,23,42,.08)}.panel-left{align-self:start;position:sticky;top:2rem;height:fit-content}.panel-right{align-self:start;min-height:0}.mode-card{border:1px solid #e2e8f0;border-radius:1rem;background:#fff;overflow:hidden;transition:max-height .28s ease,transform .28s ease,box-shadow .28s ease,opacity .24s ease;max-height:4.2rem;opacity:.9}.mode-card.open{max-height:30rem;opacity:1;transform:translateY(0);box-shadow:0 14px 30px rgba(15,23,42,.06)}.mode-card.accent{border-color:#fdba74;background:#fff7ed;max-height:24rem}.mode-toggle{width:100%;display:flex;justify-content:space-between;align-items:center;padding:1rem 1.1rem;background:transparent;color:#0f172a;border:0;font-weight:800;cursor:pointer}.mode-toggle small{color:#64748b;font-weight:600}.mode-toggle.static{cursor:default}.mode-body{padding:0 1.1rem;opacity:0;transform:translateY(-8px);transition:opacity .24s ease,transform .24s ease,padding .24s ease;pointer-events:none}.mode-card.open .mode-body,.mode-body.visible{padding:0 1.1rem 1.1rem;opacity:1;transform:translateY(0);pointer-events:auto}.field{display:grid;gap:.45rem}.field span{font-size:.86rem;font-weight:700;color:#334155}.filters-grid{display:grid;grid-template-columns:1fr 1fr;gap:.9rem}.suggestions{display:grid;gap:.5rem}.suggestion{display:flex;justify-content:space-between;align-items:center;padding:.9rem 1rem;border-radius:.9rem;background:#fff7ed;color:#111827;border:1px solid #fed7aa}.suggestion span{color:#9a3412}.section-head{display:flex;justify-content:space-between;gap:1rem;align-items:center;flex-wrap:wrap}.meta{margin:0;color:#64748b}.table{width:100%;border-collapse:collapse}.table th,.table td{padding:.8rem;border-bottom:1px solid #e2e8f0;text-align:left}.checkbox{display:flex;gap:.65rem;align-items:center}.actions,.pager{display:flex;gap:.75rem;align-items:center;flex-wrap:wrap}.muted{margin:0;color:#64748b}.error{margin:0;color:#991b1b;font-weight:700}input,select,button{padding:.85rem .95rem;border-radius:.85rem;border:1px solid #cbd5e1;font:inherit}button{background:#0f172a;color:#fff;border:0;font-weight:700;cursor:pointer}.ghost{background:#fff;color:#334155;border:1px solid #cbd5e1}@media(max-width:1100px){.grid{grid-template-columns:1fr}.panel-left{position:static}.filters-grid{grid-template-columns:1fr}}`,
})
export class SessionsPageComponent {
  private readonly customersApi = inject(CustomersApiService);
  private readonly offersApi = inject(SubscriptionOffersApiService);
  private readonly api = inject(SessionsApiService);
  private readonly settingsService = inject(SessionDisplaySettingsService);
  private readonly fb = inject(FormBuilder);

  readonly current = signal<CafeSession[]>([]);
  readonly day = signal<CafeSession[]>([]);
  readonly subscriberResults = signal<Customer[]>([]);
  readonly offers = signal<SubscriptionOffer[]>([]);
  readonly selectedWalkInSession = signal<CafeSession | null>(null);
  readonly error = signal('');
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
      this.error.set('Le nom du client journalier est requis');
      return;
    }
    this.api.start({ customerName }).subscribe({
      next: () => {
        this.walkInForm.reset({ customerName: '' });
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
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Creation du client abonne impossible'),
    });
  }

  stop(sessionId: string): void {
    this.api.stop(sessionId).subscribe({
      next: () => this.reload(),
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Arret impossible'),
    });
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
}
