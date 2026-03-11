import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import type { UserResponse } from '../models/user.models';
import { UsersApiService } from '../services/users-api.service';
import { UserFormComponent } from '../components/user-form.component';
import { UsersTableComponent } from '../components/users-table.component';

@Component({
  selector: 'app-users-page',
  imports: [CommonModule, ReactiveFormsModule, UsersTableComponent, UserFormComponent],
  template: `
    <section class="page">
      <header class="hero">
        <div>
          <p class="eyebrow">Bounded context users</p>
          <h2>Gestion des utilisateurs</h2>
        </div>
        <div class="filters" [formGroup]="filters">
          <input type="search" formControlName="term" placeholder="Rechercher un utilisateur" />
          <select formControlName="status">
            <option value="">Tous les statuts</option>
            <option value="ACTIVE">Actifs</option>
            <option value="DISABLED">Désactivés</option>
          </select>
          <button type="button" (click)="loadUsers()">Filtrer</button>
          <button type="button" class="secondary" (click)="clearSelection()">Nouvel utilisateur</button>
        </div>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>

      <div class="grid">
        <article class="panel">
          <h3>Liste</h3>
          <app-users-table [users]="users()" (selected)="selectUser($event)" />
        </article>

        <article class="panel">
          <h3>{{ selectedUser() ? 'Édition utilisateur' : 'Création utilisateur' }}</h3>
          <app-user-form
            [mode]="selectedUser() ? 'edit' : 'create'"
            [initialUser]="selectedUser()"
            (saved)="saveUser($event)"
          />
          <div class="actions" *ngIf="selectedUser() as user">
            <button type="button" (click)="toggleStatus(user)">{{ user.status === 'ACTIVE' ? 'Désactiver' : 'Activer' }}</button>
            <button type="button" class="secondary" (click)="clearSelection()">Nouveau</button>
          </div>
        </article>
      </div>
    </section>
  `,
  styles: `
    .page, .panel { display: grid; gap: 1rem; }
    .hero { display: grid; gap: 1rem; }
    .eyebrow { margin: 0; color: #ff7b00; text-transform: uppercase; letter-spacing: .15em; font-size: 0.72rem; }
    .filters { display: flex; gap: 0.75rem; flex-wrap: wrap; }
    input, select { border: 1px solid #cbd5e1; border-radius: 0.85rem; padding: 0.8rem 0.9rem; font: inherit; }
    .grid { display: grid; grid-template-columns: 1.3fr .9fr; gap: 1.5rem; }
    .panel { background: rgba(255,255,255,0.84); border-radius: 1.2rem; padding: 1.25rem; }
    .actions { display: flex; gap: 0.75rem; }
    button { border: 0; border-radius: 999px; padding: 0.8rem 1rem; background: #14213d; color: #fff; font-weight: 700; cursor: pointer; }
    .secondary { background: #64748b; }
    .error { margin: 0; color: #991b1b; font-weight: 700; }
    @media (max-width: 1000px) { .grid { grid-template-columns: 1fr; } }
  `,
})
export class UsersPageComponent {
  private readonly api = inject(UsersApiService);
  private readonly fb = inject(FormBuilder);

  readonly filters = this.fb.nonNullable.group({
    term: [''],
    status: [''],
  });
  readonly users = signal<UserResponse[]>([]);
  readonly selectedUser = signal<UserResponse | null>(null);
  readonly error = signal('');

  constructor() {
    this.loadUsers();
  }

  loadUsers(): void {
    const { term, status } = this.filters.getRawValue();
    this.api.search(term, status).subscribe({
      next: (users) => {
        this.users.set(users);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Chargement impossible'),
    });
  }

  selectUser(user: UserResponse): void {
    this.selectedUser.set(user);
  }

  clearSelection(): void {
    this.selectedUser.set(null);
    this.error.set('');
  }

  saveUser(payload: { email: string; firstName: string; lastName: string; password?: string; roles: string[] }): void {
    const current = this.selectedUser();
    const request = current
      ? this.api.update(current.userId, payload)
      : this.api.create({ ...payload, password: payload.password ?? '' });
    request.subscribe({
      next: () => {
        this.clearSelection();
        this.loadUsers();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Sauvegarde impossible'),
    });
  }

  toggleStatus(user: UserResponse): void {
    const request = user.status === 'ACTIVE' ? this.api.disable(user.userId) : this.api.enable(user.userId);
    request.subscribe({
      next: (updated) => {
        this.selectedUser.set(updated);
        this.loadUsers();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message ?? 'Mise à jour du statut impossible'),
    });
  }
}
