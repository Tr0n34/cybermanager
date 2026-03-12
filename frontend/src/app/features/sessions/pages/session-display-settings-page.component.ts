import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { SessionDisplaySettingsService } from '../services/session-display-settings.service';

@Component({
  selector: 'app-session-display-settings-page',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="page">
      <header class="hero">
        <div>
          <p class="eyebrow">Reglages sessions</p>
          <h2>Affichage des listes</h2>
        </div>
        <p class="lede">Definis combien de lignes afficher par page pour les sessions en cours et les sessions du jour.</p>
      </header>

      <article class="panel">
        <form [formGroup]="form" (ngSubmit)="save()" class="stack">
          <p class="success" *ngIf="notice()">{{ notice() }}</p>
          <label class="field">
            <span>Nombre de sessions en cours par page</span>
            <input type="number" min="1" formControlName="currentPageSize" />
          </label>
          <label class="field">
            <span>Nombre de sessions du jour par page</span>
            <input type="number" min="1" formControlName="dayPageSize" />
          </label>
          <div class="actions">
            <button type="submit" [disabled]="form.invalid">Enregistrer</button>
            <a routerLink="/sessions" class="ghost">Retour aux sessions</a>
          </div>
        </form>
      </article>
    </section>
  `,
  styles: `.page,.stack{display:grid;gap:1rem}.hero{display:grid;gap:.5rem}.eyebrow{margin:0;color:#0f766e;text-transform:uppercase;letter-spacing:.12em;font-size:.72rem}.lede{margin:0;max-width:42rem;color:#475569}.panel{max-width:36rem;background:#fff;padding:1.25rem;border-radius:1.25rem;box-shadow:0 18px 40px rgba(15,23,42,.08)}.field{display:grid;gap:.45rem}.field span{font-size:.86rem;font-weight:700;color:#334155}.actions{display:flex;gap:.75rem;flex-wrap:wrap}.success{margin:0;color:#166534;font-weight:700}input,button,a{padding:.85rem .95rem;border-radius:.85rem;border:1px solid #cbd5e1;font:inherit;text-decoration:none}button{background:#0f172a;color:#fff;border:0;font-weight:700;cursor:pointer}.ghost{background:#fff;color:#334155}`,
})
export class SessionDisplaySettingsPageComponent {
  private readonly settingsService = inject(SessionDisplaySettingsService);
  private readonly fb = inject(FormBuilder);
  readonly notice = signal('');

  readonly form = this.fb.nonNullable.group({
    currentPageSize: [this.settingsService.settings().currentPageSize, [Validators.required, Validators.min(1)]],
    dayPageSize: [this.settingsService.settings().dayPageSize, [Validators.required, Validators.min(1)]],
  });

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const saved = this.settingsService.update({
      currentPageSize: Number(this.form.getRawValue().currentPageSize),
      dayPageSize: Number(this.form.getRawValue().dayPageSize),
    });
    this.form.setValue(saved);
    this.notice.set('Configuration enregistree.');
  }
}
