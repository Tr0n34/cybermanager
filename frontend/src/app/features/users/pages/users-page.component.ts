import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import type { UserResponse } from '../models/user.models';
import { UserFormComponent } from '../components/user-form.component';
import { UsersTableComponent } from '../components/users-table.component';
import { UsersApiService } from '../services/users-api.service';

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
          <input type="search" formControlName="term" placeholder="Filtrer par nom" />

          <select formControlName="role">
            <option value="">Tous les roles</option>
            <option *ngFor="let role of roleOptions" [value]="role">{{ role }}</option>
          </select>

          <select formControlName="status">
            <option value="">Tous les statuts</option>
            <option value="ACTIVE">Actifs</option>
            <option value="DISABLED">Desactives</option>
          </select>

          <select formControlName="pageSize">
            <option *ngFor="let size of pageSizeOptions" [value]="size">{{ size }} / page</option>
          </select>

          <button *ngIf="!isPanelOpen()" type="button" class="secondary" (click)="openCreatePanel()">Nouvel utilisateur</button>
        </div>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>

      <div class="toolbar">
        <p class="summary">
          {{ filteredUsers().length }} utilisateur{{ filteredUsers().length > 1 ? 's' : '' }}
          <span *ngIf="filteredUsers().length !== users().length">sur {{ users().length }}</span>
        </p>

        <div class="pager" *ngIf="totalPages() > 1">
          <button type="button" class="ghost" (click)="previousPage()" [disabled]="currentPage() === 1">Precedent</button>
          <span>Page {{ currentPage() }} / {{ totalPages() }}</span>
          <button type="button" class="ghost" (click)="nextPage()" [disabled]="currentPage() === totalPages()">Suivant</button>
        </div>
      </div>

      <div class="grid" [class.panel-open]="isPanelOpen()">
        <article class="panel list-panel">
          <div class="panel-header">
            <h3>Liste</h3>
          </div>

          <app-users-table
            [users]="paginatedUsers()"
            [selectedUserId]="selectedUser()?.userId ?? null"
            (selected)="selectUser($event)"
          />
        </article>

        <article class="panel side-panel" [class.open]="isPanelOpen()" [attr.aria-hidden]="!isPanelOpen()">
          <div class="side-panel-header">
            <h3>{{ panelMode() === 'edit' ? 'Edition utilisateur' : 'Creation utilisateur' }}</h3>
            <button type="button" class="icon-button" (click)="closePanel()">Fermer</button>
          </div>

          <app-user-form
            [mode]="panelMode()"
            [initialUser]="selectedUser()"
            (saved)="saveUser($event)"
          />

          <div class="actions" *ngIf="selectedUser() as user">
            <button type="button" (click)="toggleStatus(user)">{{ user.status === 'ACTIVE' ? 'Desactiver' : 'Activer' }}</button>
            <button type="button" class="danger" (click)="deleteUser(user)">Supprimer</button>
            <button type="button" class="secondary" (click)="openCreatePanel()">Nouveau</button>
          </div>
        </article>
      </div>
    </section>
  `,
  styles: `
    .page, .panel { display: grid; gap: 1rem; }
    .hero { display: grid; gap: 1rem; }
    .eyebrow { margin: 0; color: #ff7b00; text-transform: uppercase; letter-spacing: 0.15em; font-size: 0.72rem; }
    .filters { display: flex; gap: 0.75rem; flex-wrap: wrap; align-items: center; }
    input, select { border: 1px solid #cbd5e1; border-radius: 0.85rem; padding: 0.8rem 0.9rem; font: inherit; background: #fff; }
    .toolbar { display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap; }
    .summary { margin: 0; color: #334155; font-weight: 600; }
    .pager { display: inline-flex; align-items: center; gap: 0.75rem; color: #475569; }
    .grid { display: grid; grid-template-columns: minmax(0, 1fr) 0fr; gap: 1.5rem; align-items: start; transition: grid-template-columns 280ms ease; }
    .grid.panel-open { grid-template-columns: minmax(0, 1.35fr) minmax(22rem, 0.9fr); }
    .panel { background: rgba(255,255,255,0.84); border-radius: 1.2rem; padding: 1.1rem; }
    .list-panel { overflow: hidden; }
    .panel-header, .side-panel-header { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
    .panel-header h3, .side-panel-header h3 { margin: 0; }
    .side-panel {
      overflow: hidden;
      opacity: 0;
      transform: translateX(18px) scale(0.98);
      transform-origin: right center;
      pointer-events: none;
      max-width: 0;
      padding-inline: 0;
      transition:
        opacity 220ms ease,
        transform 280ms ease,
        max-width 280ms ease,
        padding-inline 280ms ease;
    }
    .side-panel.open {
      opacity: 1;
      transform: translateX(0) scale(1);
      pointer-events: auto;
      max-width: 100%;
      padding-inline: 1.1rem;
    }
    .actions { display: flex; gap: 0.75rem; }
    button { border: 0; border-radius: 999px; padding: 0.8rem 1rem; background: #14213d; color: #fff; font-weight: 700; cursor: pointer; }
    .secondary { background: #64748b; }
    .ghost, .icon-button { background: #e2e8f0; color: #0f172a; }
    button:disabled { opacity: 0.45; cursor: default; }
    .error { margin: 0; color: #991b1b; font-weight: 700; }

    @media (max-width: 1000px) {
      .grid, .grid.panel-open { grid-template-columns: 1fr; }
      .side-panel, .side-panel.open { max-width: none; padding-inline: 1.1rem; opacity: 1; transform: none; }
      .side-panel:not(.open) { display: none; }
    }
  `,
})
export class UsersPageComponent {
  private readonly api = inject(UsersApiService);
  private readonly fb = inject(FormBuilder);

  readonly roleOptions = ['ADMIN', 'EMPLOYEE'];
  readonly pageSizeOptions = [5, 10, 20, 50];
  readonly filters = this.fb.nonNullable.group({
    term: [''],
    role: [''],
    status: [''],
    pageSize: [10],
  });
  readonly users = signal<UserResponse[]>([]);
  readonly selectedUser = signal<UserResponse | null>(null);
  readonly panelMode = signal<'create' | 'edit'>('create');
  readonly isPanelOpen = signal(false);
  readonly page = signal(1);
  readonly error = signal('');
  readonly filterState = signal(this.filters.getRawValue());

  readonly filteredUsers = computed(() => {
    const { term, role, status } = this.filterState();
    const normalizedTerm = term.trim().toLocaleLowerCase();

    return this.users().filter((user) => {
      const fullName = `${user.firstName} ${user.lastName}`.toLocaleLowerCase();
      const matchesName = normalizedTerm.length === 0 || fullName.includes(normalizedTerm);
      const matchesRole = !role || user.roles.includes(role);
      const matchesStatus = !status || user.status === status;
      return matchesName && matchesRole && matchesStatus;
    });
  });

  readonly totalPages = computed(() => {
    const pageSize = Number(this.filterState().pageSize) || 10;
    return Math.max(1, Math.ceil(this.filteredUsers().length / pageSize));
  });

  readonly currentPage = computed(() => Math.min(this.page(), this.totalPages()));

  readonly paginatedUsers = computed(() => {
    const pageSize = Number(this.filterState().pageSize) || 10;
    const page = this.currentPage();
    const start = (page - 1) * pageSize;
    return this.filteredUsers().slice(start, start + pageSize);
  });

  constructor() {
    this.filters.valueChanges.subscribe((value) => {
      this.filterState.set({
        term: value.term ?? '',
        role: value.role ?? '',
        status: value.status ?? '',
        pageSize: Number(value.pageSize ?? 10),
      });
      this.page.set(1);
    });

    this.loadUsers();
  }

  loadUsers(): void {
    this.api.search('', '').subscribe({
      next: (users) => {
        this.users.set(users);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveError(error, 'Chargement impossible')),
    });
  }

  selectUser(user: UserResponse): void {
    this.selectedUser.set(user);
    this.panelMode.set('edit');
    this.isPanelOpen.set(true);
  }

  openCreatePanel(): void {
    this.panelMode.set('create');
    this.selectedUser.set(null);
    this.error.set('');
    this.isPanelOpen.set(true);
  }

  closePanel(): void {
    this.selectedUser.set(null);
    this.isPanelOpen.set(false);
    this.error.set('');
  }

  previousPage(): void {
    this.page.update((page) => Math.max(1, page - 1));
  }

  nextPage(): void {
    this.page.update((page) => Math.min(this.totalPages(), page + 1));
  }

  saveUser(payload: { email: string; firstName: string; lastName: string; password?: string; roles: string[] }): void {
    const current = this.selectedUser();
    const request = current
      ? this.api.update(current.userId, payload)
      : this.api.create({ ...payload, password: payload.password ?? '' });

    request.subscribe({
      next: () => {
        this.closePanel();
        this.loadUsers();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveError(error, 'Sauvegarde impossible')),
    });
  }

  toggleStatus(user: UserResponse): void {
    const request = user.status === 'ACTIVE' ? this.api.disable(user.userId) : this.api.enable(user.userId);
    request.subscribe({
      next: (updated) => {
        this.selectedUser.set(updated);
        this.loadUsers();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveError(error, 'Mise a jour du statut impossible')),
    });
  }

  deleteUser(user: UserResponse): void {
    if (!confirm(`Supprimer l'utilisateur ${user.firstName} ${user.lastName} ?`)) {
      return;
    }

    this.api.delete(user.userId).subscribe({
      next: () => {
        this.closePanel();
        this.loadUsers();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveError(error, 'Suppression impossible')),
    });
  }

  private resolveError(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error?.message === 'string' && error.error.message.trim().length > 0) {
      return error.error.message;
    }
    if (typeof error.error === 'string' && error.error.trim().length > 0) {
      return error.error;
    }
    if (error.status === 0) {
      return 'Serveur inaccessible ou non redemarre.';
    }
    if (error.status === 404) {
      return 'Endpoint introuvable. Redemarre probablement le backend pour charger la suppression.';
    }
    if (error.status === 403) {
      return 'Action reservee a un administrateur.';
    }
    return fallback;
  }
}
