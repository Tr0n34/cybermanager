import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { SessionService } from '../core/services/session.service';

@Component({
  selector: 'app-shell',
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <div>
          <p class="eyebrow">CyberManager</p>
          <h1>Administration</h1>
        </div>
        <nav>
          <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
          <a routerLink="/users" routerLinkActive="active">Utilisateurs</a>
          <a routerLink="/products" routerLinkActive="active">Produits</a>
          <a routerLink="/subscription-offers" routerLinkActive="active">Abonnements</a>
          <a routerLink="/customers" routerLinkActive="active">Clients</a>
          <a routerLink="/sales" routerLinkActive="active">Ventes</a>
          <a routerLink="/sales/pricing" routerLinkActive="active">Tarifs temps</a>
          <a routerLink="/sessions" routerLinkActive="active">Sessions</a>
          <a routerLink="/debts" routerLinkActive="active">Dettes</a>
          <a routerLink="/monitoring" routerLinkActive="active">Monitoring</a>
          <a routerLink="/reporting" routerLinkActive="active">Historique</a>
        </nav>
        <section class="profile" *ngIf="session.user() as user">
          <p>{{ user.firstName }} {{ user.lastName }}</p>
          <small>{{ user.email }}</small>
          <button type="button" (click)="logout()">Se déconnecter</button>
        </section>
      </aside>
      <main class="content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: `
    .shell { display: grid; grid-template-columns: 280px 1fr; min-height: 100vh; background: linear-gradient(135deg, #f5efe6, #d8e2dc); color: #1f2933; }
    .sidebar { padding: 2rem; background: #14213d; color: #fff; display: flex; flex-direction: column; gap: 2rem; }
    .eyebrow { text-transform: uppercase; letter-spacing: 0.18em; font-size: 0.7rem; color: #fca311; margin: 0; }
    h1 { margin: 0.4rem 0 0; font-size: 1.8rem; }
    nav { display: grid; gap: 0.75rem; }
    a { color: rgba(255,255,255,0.72); text-decoration: none; font-weight: 600; }
    a.active, a:hover { color: #fff; }
    .profile { margin-top: auto; display: grid; gap: 0.35rem; background: rgba(255,255,255,0.08); border-radius: 1rem; padding: 1rem; }
    button { margin-top: 0.75rem; border: 0; border-radius: 999px; padding: 0.8rem 1rem; font-weight: 700; cursor: pointer; background: #fca311; color: #14213d; }
    .content { padding: 2rem; }
    @media (max-width: 900px) { .shell { grid-template-columns: 1fr; } .sidebar { gap: 1rem; } }
  `,
})
export class AppShellComponent {
  readonly session = inject(SessionService);
  private readonly router = inject(Router);

  logout(): void {
    this.session.clear();
    void this.router.navigate(['/login']);
  }
}
