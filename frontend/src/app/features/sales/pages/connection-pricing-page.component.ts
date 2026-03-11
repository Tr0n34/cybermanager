import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
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
          <p>Definis des plages de duree en heures et minutes, avec le prix applique aux clients journaliers.</p>
        </div>
        <a routerLink="/sales" class="link-button">Retour aux ventes</a>
      </header>

      <div class="grid">
        <section class="panel">
          <h3>Ajouter une plage</h3>
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
          <p class="hint">Exemple : 0 h 30 min = 1.50 EUR, 1 h 0 min = 2.50 EUR.</p>
          <p class="feedback error" *ngIf="errorMessage()">{{ errorMessage() }}</p>
          <p class="feedback success" *ngIf="successMessage()">{{ successMessage() }}</p>
        </section>

        <section class="panel">
          <div class="panel-header">
            <div>
              <h3>Grille active</h3>
              <p>Cette grille est utilisee pour la vente de temps et a l'arret d'une session journaliere.</p>
            </div>
            <button type="button" class="secondary" (click)="loadPricing()">Recharger</button>
          </div>

          <table class="table" *ngIf="tiers().length > 0; else emptyState">
            <thead>
              <tr>
                <th>Duree</th>
                <th>Prix</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let tier of tiers()">
                <td>{{ formatDuration(tier) }}</td>
                <td>{{ tier.price | number:'1.2-2' }} EUR</td>
                <td><button type="button" class="danger" (click)="removeTier(tier.durationMinutes)">Retirer</button></td>
              </tr>
            </tbody>
          </table>
          <ng-template #emptyState>
            <p class="empty">Aucune plage definie pour le moment.</p>
          </ng-template>

          <div class="actions">
            <button type="button" class="secondary" (click)="resetDraft()">Revenir a la grille chargee</button>
            <button type="button" (click)="savePricing()">Enregistrer la grille</button>
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
    .hero p { margin: 0; color: #475569; max-width: 52rem; }
    .grid { display: grid; grid-template-columns: 340px 1fr; gap: 1rem; align-items: start; }
    .panel { background: #fff; padding: 1.25rem; border-radius: 1.25rem; box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08); display: grid; gap: 1rem; }
    form { display: grid; gap: 0.85rem; }
    label { display: grid; gap: 0.45rem; font-weight: 600; color: #1e293b; }
    input, button, .link-button { padding: 0.8rem 0.95rem; border-radius: 0.9rem; border: 1px solid #cbd5e1; font: inherit; }
    button, .link-button { background: #14213d; color: #fff; border: 0; text-decoration: none; font-weight: 700; cursor: pointer; }
    .secondary { background: #e2e8f0; color: #0f172a; }
    .danger { background: #fee2e2; color: #991b1b; }
    .hint, .empty, .panel-header p { margin: 0; color: #475569; }
    .feedback { margin: 0; padding: 0.8rem 0.95rem; border-radius: 0.9rem; }
    .feedback.error { background: #fef2f2; color: #991b1b; }
    .feedback.success { background: #ecfdf5; color: #166534; }
    .panel-header { display: flex; justify-content: space-between; gap: 1rem; align-items: start; }
    .table { width: 100%; border-collapse: collapse; }
    .table th, .table td { padding: 0.9rem 0.75rem; border-bottom: 1px solid #e2e8f0; text-align: left; }
    .actions { display: flex; justify-content: flex-end; gap: 0.75rem; }
    @media (max-width: 960px) {
      .hero, .panel-header, .actions { flex-direction: column; }
      .grid { grid-template-columns: 1fr; }
    }
  `,
})
export class ConnectionPricingPageComponent {
  private readonly salesApi = inject(SalesApiService);
  private readonly fb = inject(FormBuilder);

  readonly tiers = signal<ConnectionPricingTier[]>([]);
  readonly loadedTiers = signal<ConnectionPricingTier[]>([]);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly tierForm = this.fb.nonNullable.group({
    hours: [0],
    minutes: [30],
    price: [1.5],
  });

  constructor() {
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
      this.errorMessage.set("La duree doit etre superieure a zero.");
      this.successMessage.set('');
      return;
    }
    if (price <= 0) {
      this.errorMessage.set("Le prix doit etre superieur a zero.");
      this.successMessage.set('');
      return;
    }

    const nextTier: ConnectionPricingTier = { hours, minutes, durationMinutes, price };
    const filtered = this.tiers().filter((tier) => tier.durationMinutes !== durationMinutes);
    filtered.push(nextTier);
    filtered.sort((left, right) => left.durationMinutes - right.durationMinutes);
    this.tiers.set(filtered);
    this.tierForm.reset({ hours: 0, minutes: 30, price: 1.5 });
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  removeTier(durationMinutes: number): void {
    this.tiers.set(this.tiers().filter((tier) => tier.durationMinutes !== durationMinutes));
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  resetDraft(): void {
    this.tiers.set(this.loadedTiers());
    this.tierForm.reset({ hours: 0, minutes: 30, price: 1.5 });
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  savePricing(): void {
    if (this.tiers().length === 0) {
      this.errorMessage.set("Ajoute au moins une plage avant d'enregistrer.");
      this.successMessage.set('');
      return;
    }

    this.salesApi.updatePricing({
      tiers: this.tiers().map((tier) => ({ hours: tier.hours, minutes: tier.minutes, price: tier.price })),
    }).subscribe((pricing) => {
      this.loadedTiers.set(pricing.tiers);
      this.tiers.set(pricing.tiers);
      this.tierForm.reset({ hours: 0, minutes: 30, price: 1.5 });
      this.errorMessage.set('');
      this.successMessage.set('La grille tarifaire a ete mise a jour.');
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
}
