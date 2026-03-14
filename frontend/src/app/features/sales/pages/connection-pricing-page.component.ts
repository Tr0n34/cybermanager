import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import type { ConnectionPricingTier } from '../models/sales.models';
import { SalesApiService } from '../services/sales-api.service';

@Component({
  selector: 'app-connection-pricing-page',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="page">
      <header class="hero">
        <div>
          <p class="eyebrow">Configuration</p>
          <h2>Tarifs d'usage dans le temps</h2>
          <p class="feedback warning" *ngIf="hasPendingChanges()">
            La grille a ete modifiee mais n'est pas encore validee. Utilise "Valider la grille" pour appliquer les changements.
          </p>
          <div class="hero-actions">
            <button type="button" class="secondary" (click)="openCalculator()">Calculer un tarif de connexion</button>
            <button type="button" *ngIf="!showAddTierPanel()" (click)="showAddTierPanel.set(true)">Ajouter une plage</button>
            <a *ngIf="canReturnToSales()" routerLink="/sales" class="link-button secondary">Retour aux ventes</a>
          </div>
        </div>
      </header>

      <div class="grid">
        <section class="panel">
          <div class="panel-header">
            <div>
              <h3>Grille active</h3>
            </div>
            <button type="button" class="secondary" (click)="loadPricing()">Recharger</button>
          </div>

          <table class="table" *ngIf="tiers().length > 0; else emptyState">
            <thead>
              <tr>
                <th>Duree</th>
                <th>Prix</th>
                <th>Cree le</th>
                <th>Modifie le</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let tier of tiers(); trackBy: trackTier">
                <td>
                  <ng-container *ngIf="editingTierKey() === tierKey(tier); else tierDurationView">
                    <div class="inline-edit" [formGroup]="editTierForm">
                      <input type="number" min="0" formControlName="hours" />
                      <span>h</span>
                      <input type="number" min="0" max="59" formControlName="minutes" />
                      <span>min</span>
                    </div>
                  </ng-container>
                  <ng-template #tierDurationView>{{ formatDuration(tier) }}</ng-template>
                </td>
                <td>
                  <ng-container *ngIf="editingTierKey() === tierKey(tier); else tierPriceView">
                    <div [formGroup]="editTierForm">
                      <input class="price-input" type="number" min="0" step="0.01" formControlName="price" />
                    </div>
                  </ng-container>
                  <ng-template #tierPriceView>{{ tier.price | number:'1.2-2' }} EUR</ng-template>
                </td>
                <td>{{ formatDateTime(tier.createdAt) }}</td>
                <td>{{ formatDateTime(tier.updatedAt) }}</td>
                <td class="row-actions">
                  <ng-container *ngIf="editingTierKey() === tierKey(tier); else rowActionsView">
                    <button type="button" class="secondary" (click)="cancelTierEdit()">Annuler</button>
                    <button type="button" (click)="saveTierEdit()">Valider</button>
                  </ng-container>
                  <ng-template #rowActionsView>
                    <button type="button" class="secondary" (click)="startTierEdit(tier)">Modifier</button>
                    <button type="button" class="danger" (click)="removeTier(tierKey(tier))">Retirer</button>
                  </ng-template>
                </td>
              </tr>
            </tbody>
          </table>
          <ng-template #emptyState>
            <p class="empty">Aucune plage definie pour le moment.</p>
          </ng-template>

          <div class="actions">
            <button type="button" (click)="savePricing()">Valider la grille</button>
          </div>
        </section>

        <section class="panel add-tier-panel" [class.is-collapsed]="!showAddTierPanel()">
          <div class="add-tier-body">
            <div class="panel-header">
              <div>
                <h3>Ajouter une plage</h3>
              </div>
              <button type="button" class="secondary" (click)="showAddTierPanel.set(false)">Fermer</button>
            </div>
            <form [formGroup]="tierForm" (ngSubmit)="addTier()">
              <label>
                Heures
                <input type="number" min="0" formControlName="hours" />
              </label>
              <label>
                Minutes
                <input type="number" min="0" max="59" formControlName="minutes" />
              </label>
              <label>
                Prix
                <input type="number" min="0" step="0.01" formControlName="price" />
              </label>
              <button type="submit">Ajouter la plage</button>
            </form>
            <p class="feedback error" *ngIf="errorMessage()">{{ errorMessage() }}</p>
            <p class="feedback success" *ngIf="successMessage()">{{ successMessage() }}</p>
          </div>
        </section>
      </div>

      <div class="modal-backdrop" *ngIf="isCalculatorOpen()" (click)="closeCalculator()">
        <section class="confirm-modal calculator-modal" (click)="$event.stopPropagation()">
          <div class="panel-header">
            <div>
              <h3>Calculer un tarif de connexion</h3>
            </div>
            <button type="button" class="icon-button" (click)="closeCalculator()">Fermer</button>
          </div>

          <form class="calculator-form" [formGroup]="calculatorForm">
            <label>
              Heures
              <input type="number" min="0" formControlName="hours" />
            </label>
            <label>
              Minutes
              <input type="number" min="0" max="59" formControlName="minutes" />
            </label>
          </form>

          <div class="confirm-summary">
            <p><strong>Duree saisie :</strong> {{ calculatorDurationLabel() }}</p>
            <p *ngIf="calculatorError(); else calculatorResult" class="feedback error">{{ calculatorError() }}</p>
            <ng-template #calculatorResult>
              <p><strong>Tarif calcule :</strong> {{ calculatorPriceLabel() }}</p>
            </ng-template>
          </div>
        </section>
      </div>
    </section>
  `,
  styles: `
    .page { display: grid; gap: 1.5rem; }
    .hero { display: flex; justify-content: space-between; gap: 1rem; align-items: start; }
    .eyebrow { margin: 0; text-transform: uppercase; letter-spacing: 0.16em; font-size: 0.72rem; color: #9a3412; }
    .hero h2 { margin: 0.35rem 0 0.5rem; }
    .hero-actions { display: flex; gap: 0.75rem; flex-wrap: wrap; margin-top: 0.9rem; align-items: center; }
    .grid { display: grid; grid-template-columns: minmax(0, 1fr) 340px; gap: 1rem; align-items: start; }
    .panel { background: #fff; padding: 1.25rem; border-radius: 1.25rem; box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08); display: grid; gap: 1rem; }
    .add-tier-panel { align-self: start; overflow: hidden; transition: opacity 220ms ease, transform 240ms ease, max-height 260ms ease, padding 220ms ease; transform-origin: top; max-height: 28rem; opacity: 1; transform: translateY(0) scale(1); }
    .add-tier-panel.is-collapsed { max-height: 0; opacity: 0; transform: translateY(-10px) scale(0.98); padding-top: 0; padding-bottom: 0; pointer-events: none; }
    .add-tier-body { display: grid; gap: 1rem; }
    form { display: grid; gap: 0.85rem; }
    label { display: grid; gap: 0.45rem; font-weight: 600; color: #1e293b; }
    input, button, .link-button { padding: 0.72rem 0.88rem; border-radius: 0.9rem; border: 1px solid #cbd5e1; font: inherit; }
    button, .link-button { background: #14213d; color: #fff; border: 0; text-decoration: none; font-weight: 700; cursor: pointer; }
    .secondary { background: #e2e8f0; color: #0f172a; }
    .danger { background: #fee2e2; color: #991b1b; }
    .empty, .panel-header p { margin: 0; color: #475569; }
    .feedback { margin: 0; padding: 0.8rem 0.95rem; border-radius: 0.9rem; }
    .feedback.error { background: #fef2f2; color: #991b1b; }
    .feedback.success { background: #ecfdf5; color: #166534; }
    .feedback.warning { background: #fff7ed; color: #9a3412; }
    .panel-header { display: flex; justify-content: space-between; gap: 1rem; align-items: start; }
    .table { width: 100%; border-collapse: collapse; }
    .table th, .table td { padding: 0.58rem 0.6rem; border-bottom: 1px solid #e2e8f0; text-align: left; vertical-align: middle; }
    .inline-edit { display: inline-flex; align-items: center; gap: 0.4rem; }
    .inline-edit input, .price-input { width: 4.4rem; padding: 0.48rem 0.58rem; border-radius: 0.75rem; }
    .price-input { width: 5.6rem; }
    .row-actions { display: flex; gap: 0.45rem; justify-content: flex-end; flex-wrap: wrap; }
    .actions { display: flex; justify-content: flex-end; gap: 0.75rem; }
    .calculator-modal { width: min(32rem, 100%); display: grid; gap: 1rem; }
    .calculator-form { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    @media (max-width: 960px) {
      .hero, .panel-header, .actions { flex-direction: column; }
      .grid { grid-template-columns: 1fr; }
      .calculator-form { grid-template-columns: 1fr; }
    }
  `,
})
export class ConnectionPricingPageComponent {
  private readonly salesApi = inject(SalesApiService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  readonly tiers = signal<ConnectionPricingTier[]>([]);
  readonly loadedTiers = signal<ConnectionPricingTier[]>([]);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly showAddTierPanel = signal(false);
  readonly isCalculatorOpen = signal(false);
  readonly editingTierKey = signal<string | null>(null);
  readonly canReturnToSales = signal(false);
  readonly tierForm = this.fb.nonNullable.group({
    hours: [0],
    minutes: [30],
    price: [1.5],
  });
  readonly editTierForm = this.fb.nonNullable.group({
    hours: [0],
    minutes: [30],
    price: [1.5],
  });
  readonly calculatorForm = this.fb.nonNullable.group({
    hours: [0],
    minutes: [30],
  });
  readonly calculatorState = signal(this.calculatorForm.getRawValue());
  readonly calculatorDuration = computed(() => {
    const { hours, minutes } = this.calculatorState();
    return Number(hours) * 60 + Number(minutes);
  });
  readonly calculatorError = computed(() => {
    if (this.calculatorDuration() <= 0) {
      return 'La duree doit etre superieure a zero.';
    }
    if (this.tiers().length === 0) {
      return 'Ajoute au moins une plage pour effectuer le calcul.';
    }
    return '';
  });
  readonly calculatorPrice = computed(() => {
    if (this.calculatorError()) {
      return null;
    }
    return this.calculateConnectionPrice(this.calculatorDuration(), this.tiers());
  });
  readonly hasPendingChanges = computed(() =>
    this.normalizeTiers(this.tiers()) !== this.normalizeTiers(this.loadedTiers()),
  );

  constructor() {
    this.canReturnToSales.set(Boolean(this.router.getCurrentNavigation()?.extras.state?.['fromSales'] ?? window.history.state?.fromSales));
    this.calculatorForm.valueChanges.pipe(takeUntilDestroyed()).subscribe((value) => {
      this.calculatorState.set({
        hours: Number(value.hours ?? 0),
        minutes: Number(value.minutes ?? 0),
      });
    });
    this.loadPricing();
  }

  loadPricing(): void {
    this.salesApi.pricing().subscribe((pricing) => {
      this.loadedTiers.set(pricing.tiers);
      this.tiers.set(pricing.tiers);
      this.errorMessage.set('');
      this.successMessage.set('');
    });
  }

  addTier(): void {
    const { hours, minutes, price } = this.tierForm.getRawValue();
    const durationMinutes = hours * 60 + minutes;
    if (durationMinutes <= 0) {
      this.errorMessage.set('La duree doit etre superieure a zero.');
      this.successMessage.set('');
      return;
    }
    if (price <= 0) {
      this.errorMessage.set('Le prix doit etre superieur a zero.');
      this.successMessage.set('');
      return;
    }

    const nextTier: ConnectionPricingTier = {
      id: null,
      hours,
      minutes,
      durationMinutes,
      price,
      createdAt: null,
      updatedAt: null,
    };
    const filtered = this.tiers().filter((tier) => this.tierKey(tier) !== this.tierKey(nextTier) && tier.durationMinutes !== durationMinutes);
    filtered.push(nextTier);
    this.tiers.set(this.sortTiers(filtered));
    this.tierForm.reset({ hours: 0, minutes: 30, price: 1.5 });
    this.errorMessage.set('');
    this.successMessage.set('La nouvelle plage a ete preparee. Valide la grille pour la prendre en compte.');
  }

  startTierEdit(tier: ConnectionPricingTier): void {
    this.editingTierKey.set(this.tierKey(tier));
    this.editTierForm.reset({
      hours: tier.hours,
      minutes: tier.minutes,
      price: tier.price,
    });
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  cancelTierEdit(): void {
    this.editingTierKey.set(null);
    this.editTierForm.reset({ hours: 0, minutes: 30, price: 1.5 });
    this.errorMessage.set('');
  }

  saveTierEdit(): void {
    const key = this.editingTierKey();
    if (!key) {
      return;
    }

    const currentTier = this.tiers().find((tier) => this.tierKey(tier) === key);
    if (!currentTier) {
      this.cancelTierEdit();
      return;
    }

    const { hours, minutes, price } = this.editTierForm.getRawValue();
    const safeHours = Math.max(0, Number(hours ?? 0));
    const safeMinutes = Math.max(0, Math.min(59, Number(minutes ?? 0)));
    const safePrice = Number(price ?? 0);
    const durationMinutes = safeHours * 60 + safeMinutes;

    if (durationMinutes <= 0) {
      this.errorMessage.set('La duree doit etre superieure a zero.');
      this.successMessage.set('');
      return;
    }
    if (safePrice <= 0) {
      this.errorMessage.set('Le prix doit etre superieur a zero.');
      this.successMessage.set('');
      return;
    }
    if (this.tiers().some((tier) => this.tierKey(tier) !== key && tier.durationMinutes === durationMinutes)) {
      this.errorMessage.set('Une plage avec cette duree existe deja.');
      this.successMessage.set('');
      return;
    }

    this.tiers.set(this.sortTiers(this.tiers().map((tier) => this.tierKey(tier) === key
      ? { ...tier, hours: safeHours, minutes: safeMinutes, durationMinutes, price: safePrice }
      : tier)));
    this.editingTierKey.set(null);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  removeTier(key: string): void {
    this.tiers.set(this.tiers().filter((tier) => this.tierKey(tier) !== key));
    if (this.editingTierKey() === key) {
      this.cancelTierEdit();
      return;
    }
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  savePricing(): void {
    if (this.tiers().length === 0) {
      this.errorMessage.set('Ajoute au moins une plage avant de valider.');
      this.successMessage.set('');
      return;
    }
    if (this.tiers().some((tier) => tier.durationMinutes <= 0 || tier.price <= 0)) {
      this.errorMessage.set('Chaque plage doit avoir une duree et un prix valides.');
      this.successMessage.set('');
      return;
    }

    this.salesApi.updatePricing({
      tiers: this.tiers().map((tier) => ({ hours: tier.hours, minutes: tier.minutes, price: tier.price })),
    }).subscribe((pricing) => {
      this.loadedTiers.set(pricing.tiers);
      this.tiers.set(pricing.tiers);
      this.tierForm.reset({ hours: 0, minutes: 30, price: 1.5 });
      this.editingTierKey.set(null);
      this.errorMessage.set('');
      this.successMessage.set('La nouvelle grille tarifaire a bien ete prise en compte.');
      this.showAddTierPanel.set(false);
    });
  }

  openCalculator(): void {
    this.isCalculatorOpen.set(true);
  }

  closeCalculator(): void {
    this.isCalculatorOpen.set(false);
  }

  calculatorDurationLabel(): string {
    const { hours, minutes } = this.calculatorState();
    if (!hours && !minutes) {
      return '0 min';
    }

    const parts: string[] = [];
    if (hours > 0) {
      parts.push(`${hours} h`);
    }
    if (minutes > 0) {
      parts.push(`${minutes} min`);
    }
    return parts.join(' ');
  }

  calculatorPriceLabel(): string {
    const price = this.calculatorPrice();
    if (price == null) {
      return '-';
    }
    return `${price.toFixed(2)} EUR`;
  }

  trackTier = (_index: number, tier: ConnectionPricingTier): string => this.tierKey(tier);

  tierKey(tier: ConnectionPricingTier): string {
    return tier.id != null ? `id-${tier.id}` : `duration-${tier.durationMinutes}`;
  }

  formatDateTime(value: string | null): string {
    if (!value) {
      return '-';
    }
    return new Date(value).toLocaleString('fr-FR');
  }

  formatDuration(tier: ConnectionPricingTier): string {
    if (tier.hours && tier.minutes) {
      return `${tier.hours} h ${tier.minutes} min`;
    }
    if (tier.hours) {
      return `${tier.hours} h`;
    }
    return `${tier.minutes} min`;
  }

  private sortTiers(tiers: ConnectionPricingTier[]): ConnectionPricingTier[] {
    return [...tiers].sort((left, right) => left.durationMinutes - right.durationMinutes);
  }

  private normalizeTiers(tiers: ConnectionPricingTier[]): string {
    return JSON.stringify(
      this.sortTiers(tiers).map((tier) => ({
        durationMinutes: tier.durationMinutes,
        price: Number(tier.price),
      })),
    );
  }

  private calculateConnectionPrice(minutes: number, tiers: ConnectionPricingTier[]): number {
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

    if (bestPrice == null) {
      throw new Error('Unable to calculate pricing for duration');
    }

    return bestPrice;
  }
}
