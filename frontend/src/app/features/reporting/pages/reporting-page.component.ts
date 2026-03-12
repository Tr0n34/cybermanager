import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import type { DayCustomerHistory } from '../models/reporting.models';
import { ReportingApiService } from '../services/reporting-api.service';

@Component({
  selector: 'app-reporting-page',
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="page">
      <h2>Historique journalier</h2>
      <form class="toolbar" [formGroup]="form">
        <input type="date" formControlName="date" />
        <button type="button" (click)="load()">Charger</button>
      </form>
      <div class="grid">
        <div class="panel">
          <table class="table">
            <tr><th>Client</th><th>Type</th><th>Temps</th><th>Montant</th><th></th></tr>
            <tr *ngFor="let item of customers()">
              <td>{{ item.name }}</td><td>{{ item.type }}</td><td>{{ item.totalMinutes }} min</td><td>{{ item.salesTotal | number:'1.2-2' }} €</td>
              <td><button type="button" (click)="detail(item.customerId)">Détail</button></td>
            </tr>
          </table>
        </div>
        <div class="panel" *ngIf="detailState() as detail">
          <h3>{{ detail.name }}</h3>
          <h4>Ventes</h4><ul><li *ngFor="let item of detail.sales">{{ item }}</li></ul>
          <h4>Sessions</h4><ul><li *ngFor="let item of detail.sessions">{{ item }}</li></ul>
        </div>
      </div>
    </section>
  `,
  styles: `.page{display:grid;gap:1rem}.toolbar{display:flex;gap:.75rem;flex-wrap:wrap}.grid{display:grid;grid-template-columns:1.2fr .8fr;gap:1rem}.panel{background:#fff;padding:1rem;border-radius:1rem}.table{width:100%;border-collapse:collapse}.table th,.table td{padding:.75rem;border-bottom:1px solid #e2e8f0;text-align:left}input,button{padding:.75rem;border-radius:.75rem;border:1px solid #cbd5e1}button{background:#14213d;color:#fff;border:0}@media(max-width:1000px){.grid{grid-template-columns:1fr}}`,
})
export class ReportingPageComponent {
  private readonly api = inject(ReportingApiService);
  private readonly fb = inject(FormBuilder);
  readonly customers = signal<DayCustomerHistory[]>([]);
  readonly detailState = signal<{ customerId: string; name: string; sales: string[]; sessions: string[] } | null>(null);
  readonly form = this.fb.nonNullable.group({ date: [new Date().toISOString().slice(0, 10)] });
  constructor() { this.load(); }
  load(): void { this.api.day(this.form.getRawValue().date).subscribe((v) => this.customers.set(v.customers)); }
  detail(customerId: string): void { const date = this.form.getRawValue().date; this.api.customer(date, customerId).subscribe((v) => this.detailState.set(v)); }
}
