import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnChanges, Output, inject, input } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import type { UserResponse } from '../models/user.models';
import { RoleSelectorComponent } from './role-selector.component';

@Component({
  selector: 'app-user-form',
  imports: [CommonModule, ReactiveFormsModule, RoleSelectorComponent],
  template: `
    <form class="form" [formGroup]="form" (ngSubmit)="submit()">
      <label>Email <input type="email" formControlName="email" /></label>
      <label>Prénom <input type="text" formControlName="firstName" /></label>
      <label>Nom <input type="text" formControlName="lastName" /></label>
      <label>Mot de passe <input type="password" formControlName="password" [placeholder]="mode() === 'create' ? 'obligatoire' : 'laisser vide pour conserver'" /></label>
      <div>
        <span>Rôles</span>
        <app-role-selector [selected]="roles" (selectedChange)="setRoles($event)" />
      </div>
      <button type="submit" [disabled]="form.invalid || roles.length === 0">{{ mode() === 'create' ? 'Créer' : 'Enregistrer' }}</button>
    </form>
  `,
  styles: `
    .form { display: grid; gap: 0.85rem; }
    label, div { display: grid; gap: 0.4rem; }
    input { border: 1px solid #cbd5e1; border-radius: 0.8rem; padding: 0.85rem 0.95rem; font: inherit; }
    button { border: 0; border-radius: 999px; padding: 0.9rem 1rem; background: #14213d; color: #fff; font-weight: 700; cursor: pointer; }
  `,
})
export class UserFormComponent implements OnChanges {
  private readonly fb = inject(FormBuilder);

  readonly mode = input<'create' | 'edit'>('create');
  readonly initialUser = input<UserResponse | null>(null);
  roles: string[] = ['EMPLOYEE'];

  @Output() readonly saved = new EventEmitter<{
    email: string;
    firstName: string;
    lastName: string;
    password?: string;
    roles: string[];
  }>();

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    password: [''],
  });

  ngOnChanges(): void {
    const user = this.initialUser();
    if (!user) {
      this.roles = ['EMPLOYEE'];
      this.form.reset({
        email: '',
        firstName: '',
        lastName: '',
        password: '',
      });
      return;
    }
    this.roles = [...user.roles];
    this.form.patchValue({
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      password: '',
    });
  }

  setRoles(roles: string[]): void {
    this.roles = roles;
  }

  submit(): void {
    if (this.form.invalid || this.roles.length === 0) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.saved.emit({ ...raw, roles: this.roles });
  }
}
