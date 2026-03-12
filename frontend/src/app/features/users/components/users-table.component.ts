import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, input } from '@angular/core';

import type { UserResponse } from '../models/user.models';
import { UserStatusBadgeComponent } from './user-status-badge.component';

@Component({
  selector: 'app-users-table',
  imports: [CommonModule, UserStatusBadgeComponent],
  template: `
    <table class="table">
      <thead>
        <tr>
          <th>Utilisateur</th>
          <th>Roles</th>
          <th>Statut</th>
          <th></th>
        </tr>
      </thead>

      <tbody>
        <tr *ngFor="let user of users()" [class.active]="selectedUserId() === user.userId">
          <td>
            <strong>{{ user.firstName }} {{ user.lastName }}</strong>
            <div class="email">{{ user.email }}</div>
          </td>
          <td class="roles">{{ user.roles.join(', ') }}</td>
          <td><app-user-status-badge [status]="user.status" /></td>
          <td><button type="button" (click)="selected.emit(user)">Modifier</button></td>
        </tr>

        <tr *ngIf="users().length === 0">
          <td colspan="4" class="empty">Aucun utilisateur pour ce filtre.</td>
        </tr>
      </tbody>
    </table>
  `,
  styles: `
    .table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 1rem; overflow: hidden; table-layout: fixed; }
    th, td { padding: 0.72rem 0.85rem; border-bottom: 1px solid #e2e8f0; text-align: left; vertical-align: middle; }
    th { font-size: 0.78rem; letter-spacing: 0.08em; text-transform: uppercase; color: #64748b; }
    tbody tr { transition: background 160ms ease; }
    tbody tr:hover, tbody tr.active { background: #fff7ed; }
    strong { display: block; line-height: 1.2; }
    .email { color: #64748b; font-size: 0.85rem; margin-top: 0.12rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .roles { color: #334155; font-size: 0.9rem; }
    .empty { text-align: center; color: #64748b; padding: 1rem; }
    button { border: 0; border-radius: 999px; padding: 0.45rem 0.8rem; background: #fca311; color: #14213d; font-weight: 700; cursor: pointer; }
  `,
})
export class UsersTableComponent {
  readonly users = input<UserResponse[]>([]);
  readonly selectedUserId = input<string | null>(null);

  @Output() readonly selected = new EventEmitter<UserResponse>();
}
