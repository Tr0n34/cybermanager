import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';

import { SessionService } from '../../../core/services/session.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [CommonModule],
  template: `
    <section class="dashboard">
      <header>
        <p class="eyebrow">Vue opérationnelle</p>
        <h2>Bienvenue {{ fullName() }}</h2>
        <p>Cette base couvre l’authentification et la gestion des utilisateurs.</p>
      </header>
      <article class="card">
        <h3>Profil courant</h3>
        <dl *ngIf="session.user() as user">
          <div><dt>Email</dt><dd>{{ user.email }}</dd></div>
          <div><dt>Rôles</dt><dd>{{ user.roles.join(', ') }}</dd></div>
        </dl>
      </article>
    </section>
  `,
  styles: `
    .dashboard { display: grid; gap: 1.5rem; }
    .eyebrow { margin: 0; color: #ff7b00; text-transform: uppercase; letter-spacing: 0.15em; font-size: 0.72rem; }
    header, .card { background: rgba(255,255,255,0.8); border-radius: 1.2rem; padding: 1.5rem; }
    dl { display: grid; gap: 1rem; margin: 0; }
    div { display: grid; gap: 0.2rem; }
    dt { font-weight: 700; color: #334155; }
    dd { margin: 0; }
  `,
})
export class DashboardPageComponent {
  readonly session = inject(SessionService);
  readonly fullName = computed(() => {
    const user = this.session.user();
    return user ? `${user.firstName} ${user.lastName}` : '';
  });
}
