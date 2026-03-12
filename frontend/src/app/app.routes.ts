import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { AppShellComponent } from './layout/app-shell.component';
import { LoginPageComponent } from './features/auth/pages/login-page.component';
import { DashboardPageComponent } from './features/dashboard/pages/dashboard-page.component';
import { UsersPageComponent } from './features/users/pages/users-page.component';
import { ProductsPageComponent } from './features/products/pages/products-page.component';
import { SubscriptionOffersPageComponent } from './features/subscriptions/pages/subscription-offers-page.component';
import { CustomersPageComponent } from './features/customers/pages/customers-page.component';
import { SalesPageComponent } from './features/sales/pages/sales-page.component';
import { ConnectionPricingPageComponent } from './features/sales/pages/connection-pricing-page.component';
import { SessionsPageComponent } from './features/sessions/pages/sessions-page.component';
import { SessionDisplaySettingsPageComponent } from './features/sessions/pages/session-display-settings-page.component';
import { MonitoringPageComponent } from './features/monitoring/pages/monitoring-page.component';
import { ReportingPageComponent } from './features/reporting/pages/reporting-page.component';
import { DebtsPageComponent } from './features/debts/pages/debts-page.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginPageComponent,
    canActivate: [guestGuard],
  },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: DashboardPageComponent },
      { path: 'users', component: UsersPageComponent },
      { path: 'products', component: ProductsPageComponent },
      { path: 'subscription-offers', component: SubscriptionOffersPageComponent },
      { path: 'customers', component: CustomersPageComponent },
      { path: 'sales', component: SalesPageComponent },
      { path: 'sales/pricing', component: ConnectionPricingPageComponent },
      { path: 'sessions', component: SessionsPageComponent },
      { path: 'sessions/settings', component: SessionDisplaySettingsPageComponent },
      { path: 'debts', component: DebtsPageComponent },
      { path: 'monitoring', component: MonitoringPageComponent },
      { path: 'reporting', component: ReportingPageComponent },
    ],
  },
  { path: '**', redirectTo: '' },
];
