import { Injectable, computed, signal } from '@angular/core';

import type { SessionUser } from '../models/session-user.model';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly tokenState = signal<string | null>(localStorage.getItem('cm_token'));
  private readonly userState = signal<SessionUser | null>(this.readStoredUser());

  readonly token = computed(() => this.tokenState());
  readonly user = computed(() => this.userState());
  readonly isAuthenticated = computed(() => !!this.tokenState());

  setSession(token: string, user: SessionUser): void {
    localStorage.setItem('cm_token', token);
    localStorage.setItem('cm_user', JSON.stringify(user));
    this.tokenState.set(token);
    this.userState.set(user);
  }

  clear(): void {
    localStorage.removeItem('cm_token');
    localStorage.removeItem('cm_user');
    this.tokenState.set(null);
    this.userState.set(null);
  }

  updateUser(user: SessionUser): void {
    localStorage.setItem('cm_user', JSON.stringify(user));
    this.userState.set(user);
  }

  private readStoredUser(): SessionUser | null {
    const raw = localStorage.getItem('cm_user');
    return raw ? (JSON.parse(raw) as SessionUser) : null;
  }
}
