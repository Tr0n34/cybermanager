import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { SessionService } from '../../../core/services/session.service';
import { AuthenticationErrorBannerComponent } from '../components/authentication-error-banner.component';
import { LoginFormComponent } from '../components/login-form.component';
import { AuthApiService } from '../services/auth-api.service';

@Component({
  selector: 'app-login-page',
  imports: [CommonModule, LoginFormComponent, AuthenticationErrorBannerComponent],
  template: `
    <section class="page">
      <div class="card">
        <p class="eyebrow">Cyber cafe operations</p>
        <h1>Connexion sécurisée</h1>
        <p class="lead">Authentification des employés et administrateurs CyberManager.</p>
        <app-authentication-error-banner [message]="error()" />
        <app-login-form [loading]="loading()" (loginRequested)="login($event)" />
      </div>
    </section>
  `,
  styles: `
    .page { min-height: 100vh; display: grid; place-items: center; padding: 2rem; background:
      radial-gradient(circle at top left, rgba(252,163,17,.35), transparent 32%),
      radial-gradient(circle at bottom right, rgba(20,33,61,.35), transparent 30%),
      #e0fbfc; }
    .card { width: min(440px, 100%); display: grid; gap: 1rem; background: rgba(255,255,255,0.92); border-radius: 1.5rem; padding: 2rem; box-shadow: 0 30px 60px rgba(20,33,61,.18); }
    .eyebrow { margin: 0; color: #ff7b00; text-transform: uppercase; letter-spacing: 0.18em; font-size: 0.72rem; }
    h1 { margin: 0; color: #14213d; }
    .lead { margin: 0; color: #475569; }
  `,
})
export class LoginPageComponent {
  private readonly authApi = inject(AuthApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal('');

  login(payload: { email: string; password: string }): void {
    this.loading.set(true);
    this.error.set('');
    this.authApi.login(payload).subscribe({
      next: (response) => {
        this.session.setSession(response.token, response);
        void this.router.navigate(['/dashboard']);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message ?? 'Authentification impossible');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false),
    });
  }
}
