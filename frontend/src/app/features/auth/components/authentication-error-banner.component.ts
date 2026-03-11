import { CommonModule } from '@angular/common';
import { Component, input } from '@angular/core';

@Component({
  selector: 'app-authentication-error-banner',
  imports: [CommonModule],
  template: `<p *ngIf="message()" class="banner">{{ message() }}</p>`,
  styles: `.banner { margin: 0; border-radius: 0.9rem; background: #ffe3e3; color: #7f1d1d; padding: 0.85rem 1rem; font-weight: 600; }`,
})
export class AuthenticationErrorBannerComponent {
  readonly message = input<string>('');
}
