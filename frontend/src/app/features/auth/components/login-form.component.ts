import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, inject, input } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import type { LoginPayload } from '../models/auth.models';

@Component({
  selector: 'app-login-form',
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <form class="form" [formGroup]="form" (ngSubmit)="submit()">
      <label>
        Email
        <input type="email" formControlName="email" placeholder="admin@cybermanager.local" />
      </label>
      <label>
        Mot de passe
        <input type="password" formControlName="password" placeholder="admin123" />
      </label>
      <button type="submit" [disabled]="loading() || form.invalid">
        {{ loading() ? 'Connexion...' : 'Se connecter' }}
      </button>
    </form>
  `,
  styles: `
    .form { display: grid; gap: 1rem; }
    label { display: grid; gap: 0.45rem; font-weight: 700; color: #14213d; }
    input { border: 1px solid #cbd5e1; border-radius: 0.9rem; padding: 0.95rem 1rem; font: inherit; }
    button { border: 0; border-radius: 999px; padding: 1rem 1.2rem; background: #14213d; color: #fff; font-weight: 700; cursor: pointer; }
    button:disabled { opacity: 0.7; cursor: default; }
  `,
})
export class LoginFormComponent {
  private readonly fb = inject(FormBuilder);

  readonly loading = input(false);

  @Output() readonly loginRequested = new EventEmitter<LoginPayload>();

  readonly form = this.fb.nonNullable.group({
    email: ['admin@cybermanager.local', [Validators.required, Validators.email]],
    password: ['admin123', [Validators.required]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loginRequested.emit(this.form.getRawValue());
  }
}
