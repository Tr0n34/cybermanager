import { Component, input } from '@angular/core';

@Component({
  selector: 'app-user-status-badge',
  template: `
    <span class="status-badge" [class.active]="status() === 'ACTIVE'" [class.disabled]="status() === 'DISABLED'">
      {{ status() === 'ACTIVE' ? 'Active' : 'Disabled' }}
    </span>
  `,
  styles: `
    .status-badge { display: inline-flex; align-items: center; padding: 0.34rem 0.76rem; border-radius: 999px; font-size: 0.78rem; font-weight: 800; letter-spacing: 0.01em; }
    .status-badge.active { background: linear-gradient(135deg, #dcfce7, #bbf7d0); color: #166534; box-shadow: inset 0 0 0 1px rgba(22, 101, 52, 0.08); }
    .status-badge.disabled { background: linear-gradient(135deg, #fee2e2, #fecaca); color: #991b1b; box-shadow: inset 0 0 0 1px rgba(153, 27, 27, 0.08); }
  `,
})
export class UserStatusBadgeComponent {
  readonly status = input<'ACTIVE' | 'DISABLED'>('ACTIVE');
}
