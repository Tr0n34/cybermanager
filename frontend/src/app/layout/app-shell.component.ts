import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
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
        </div>
        <nav>
          <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
          <a routerLink="/sessions" routerLinkActive="active">Sessions</a>
          <div class="nav-group" [class.open]="openMenus().products">
            <button type="button" class="nav-toggle" (click)="toggleMenu('products')" [attr.aria-expanded]="openMenus().products">
              <span>Gestion Produit</span>
              <span class="toggle-mark" aria-hidden="true">{{ openMenus().products ? '-' : '+' }}</span>
            </button>
            <div class="nav-children">
              <a routerLink="/products" routerLinkActive="active">Produits</a>
              <a routerLink="/subscription-offers" routerLinkActive="active">Abonnements</a>
              <a routerLink="/sales" routerLinkActive="active">Ventes</a>
              <a routerLink="/sales/pricing" routerLinkActive="active">Tarifs Temps</a>
            </div>
          </div>
          <a routerLink="/debts" routerLinkActive="active">Dettes</a>
          <div class="nav-group" [class.open]="openMenus().functions">
            <button type="button" class="nav-toggle" (click)="toggleMenu('functions')" [attr.aria-expanded]="openMenus().functions">
              <span>Fonctions</span>
              <span class="toggle-mark" aria-hidden="true">{{ openMenus().functions ? '-' : '+' }}</span>
            </button>
            <div class="nav-children">
              <a routerLink="/customers" routerLinkActive="active">Clients</a>
              <a routerLink="/users" routerLinkActive="active">Utilisateurs</a>
              <a routerLink="/company" routerLinkActive="active">Entreprise</a>
              <a routerLink="/invoices" routerLinkActive="active">Factures</a>
              <a routerLink="/archiving" routerLinkActive="active">Archivage</a>
              <a routerLink="/reporting" routerLinkActive="active">Historique</a>
            </div>
          </div>
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
    .shell { display: grid; grid-template-columns: clamp(220px, 16vw, 280px) 1fr; height: var(--cm-app-height); min-height: var(--cm-app-height); overflow: hidden; background: linear-gradient(135deg, #f5efe6, #d8e2dc); color: #1f2933; }
    .sidebar { padding: var(--cm-page-padding); background: #14213d; color: #fff; display: flex; flex-direction: column; gap: clamp(0.9rem, 1.8dvh, 2rem); min-height: var(--cm-app-height); max-height: var(--cm-app-height); overflow: auto; }
    .eyebrow { text-transform: uppercase; letter-spacing: 0.08em; font-size: 0.96rem; color: #fca311; margin: 0; font-family: var(--cm-title-font); font-weight: 800; }
    nav { display: grid; gap: clamp(0.35rem, 0.8dvh, 0.75rem); }
    a { color: rgba(255,255,255,0.72); text-decoration: none; font-weight: 600; }
    a.active, a:hover { color: #fff; }
    .nav-group { display: grid; gap: 0; }
    .nav-toggle { width: 100%; margin-top: 0; justify-content: space-between; background: transparent !important; box-shadow: none !important; color: rgba(255,255,255,0.78) !important; padding: 0.15rem 0 !important; border-radius: 0 !important; font-weight: 700; }
    .nav-toggle:hover { transform: none; filter: none; color: #fff !important; box-shadow: none !important; }
    .toggle-mark { display: inline-flex; width: 1rem; justify-content: center; font-weight: 900; color: #fca311; }
    .nav-children { display: grid; gap: 0.42rem; padding-left: 0.9rem; max-height: 0; opacity: 0; overflow: hidden; transform: translateY(-4px); transition: max-height 170ms ease-out, opacity 140ms ease-out, transform 170ms ease-out, padding-top 170ms ease-out; }
    .nav-group.open .nav-children { max-height: 12rem; opacity: 1; transform: translateY(0); padding-top: 0.2rem; }
    .nav-children a { font-size: 0.95rem; }
    .profile { margin-top: auto; display: grid; gap: 0.35rem; background: rgba(255,255,255,0.08); border-radius: 1rem; padding: clamp(0.75rem, 1dvh, 1rem); }
    button { margin-top: 0.75rem; border: 0; border-radius: 999px; padding: 0.8rem 1rem; font-weight: 700; cursor: pointer; background: #fca311; color: #14213d; }
    .content { padding: var(--cm-page-padding); height: var(--cm-app-height); min-height: var(--cm-app-height); max-height: var(--cm-app-height); overflow: auto; }
    @media (max-width: 900px) { .shell { grid-template-columns: 1fr; } .sidebar { gap: 1rem; min-height: auto; max-height: none; } .content { min-height: auto; max-height: none; } }
  `,
})
export class AppShellComponent {
  readonly session = inject(SessionService);
  private readonly router = inject(Router);
  readonly openMenus = signal(this.initialMenus());

  toggleMenu(menu: 'products' | 'functions'): void {
    this.openMenus.update((state) => ({ ...state, [menu]: !state[menu] }));
  }

  logout(): void {
    this.session.clear();
    void this.router.navigate(['/login']);
  }

  private initialMenus(): { products: boolean; functions: boolean } {
    const url = this.router.url;
      return {
      products: url.startsWith('/products') || url.startsWith('/subscription-offers') || url.startsWith('/sales'),
      functions: url.startsWith('/customers') || url.startsWith('/users') || url.startsWith('/company') || url.startsWith('/invoices') || url.startsWith('/archiving') || url.startsWith('/reporting'),
    };
  }
}
