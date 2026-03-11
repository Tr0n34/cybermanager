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
          <th>Rôles</th>
          <th>Statut</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let user of users()">
          <td>
            <strong>{{ user.firstName }} {{ user.lastName }}</strong>
            <div>{{ user.email }}</div>
          </td>
          <td>{{ user.roles.join(', ') }}</td>
          <td><app-user-status-badge [status]="user.status" /></td>
          <td><button type="button" (click)="selected.emit(user)">Modifier</button></td>
        </tr>
      </tbody>
    </table>
  `,
  styles: `
    .table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 1rem; overflow: hidden; }
    th, td { padding: 1rem; border-bottom: 1px solid #e2e8f0; text-align: left; }
    button { border: 0; border-radius: 999px; padding: 0.6rem 0.9rem; background: #fca311; color: #14213d; font-weight: 700; cursor: pointer; }
  `,
})
export class UsersTableComponent {
  readonly users = input<UserResponse[]>([]);

  @Output() readonly selected = new EventEmitter<UserResponse>();
}
