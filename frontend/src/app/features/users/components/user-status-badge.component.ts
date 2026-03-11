import { Component, input } from '@angular/core';

@Component({
  selector: 'app-user-status-badge',
  template: `<span class="badge" [class.disabled]="status() === 'DISABLED'">{{ status() }}</span>`,
  styles: `
    .badge { display: inline-flex; padding: 0.3rem 0.75rem; border-radius: 999px; background: #dcfce7; color: #166534; font-weight: 700; font-size: 0.78rem; }
    .badge.disabled { background: #fee2e2; color: #991b1b; }
  `,
})
export class UserStatusBadgeComponent {
  readonly status = input<'ACTIVE' | 'DISABLED'>('ACTIVE');
}
