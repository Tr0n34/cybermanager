import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import type { DayCustomer } from '../models/monitoring.models';
import { MonitoringApiService } from '../services/monitoring-api.service';

@Component({
  selector: 'app-monitoring-page',
  imports: [CommonModule],
  template: `
    <section class="page">
      <h2>Monitoring temps réel</h2>
      <div class="grid">
        <div class="panel">
          <table class="table">
            <tr><th>Client</th><th>Type</th><th>Temps</th><th>Achats</th><th>État</th><th></th></tr>
            <tr *ngFor="let item of customers()">
              <td>{{ item.name }}</td><td>{{ item.type }}</td><td>{{ item.consumedMinutes }} min</td><td>{{ item.purchasesTotal | number:'1.2-2' }} €</td><td>{{ item.activeSession ? 'En cours' : 'Terminé' }}</td>
              <td><button type="button" (click)="loadDetail(item.customerId)">Détail</button></td>
            </tr>
          </table>
        </div>
        <div class="panel" *ngIf="detail() as detail">
          <h3>{{ detail.name }}</h3>
          <h4>Ventes</h4>
          <ul><li *ngFor="let item of detail.sales">{{ item }}</li></ul>
          <h4>Sessions</h4>
          <ul><li *ngFor="let item of detail.sessions">{{ item }}</li></ul>
        </div>
      </div>
    </section>
  `,
  styles: `.page{display:grid;gap:1rem}.grid{display:grid;grid-template-columns:1.2fr .8fr;gap:1rem}.panel{background:#fff;padding:1rem;border-radius:1rem}.table{width:100%;border-collapse:collapse}.table th,.table td{padding:.75rem;border-bottom:1px solid #e2e8f0;text-align:left}button{padding:.6rem .8rem;border-radius:.75rem;border:0;background:#14213d;color:#fff}@media(max-width:1000px){.grid{grid-template-columns:1fr}}`,
})
export class MonitoringPageComponent {
  private readonly api = inject(MonitoringApiService);
  readonly customers = signal<DayCustomer[]>([]);
  readonly detail = signal<{ customerId: string; name: string; sales: string[]; sessions: string[] } | null>(null);
  constructor() { this.api.customers().subscribe((v) => this.customers.set(v)); }
  loadDetail(customerId: string): void { this.api.customer(customerId).subscribe((v) => this.detail.set(v)); }
}
