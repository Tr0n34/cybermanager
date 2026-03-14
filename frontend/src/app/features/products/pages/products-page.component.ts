import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import type { Product } from '../models/product.models';
import { ProductsApiService } from '../services/products-api.service';

@Component({
  selector: 'app-products-page',
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="page">
      <header class="hero">
        <div>
          <p class="eyebrow">Bounded context product</p>
          <h2>Catalogue produits</h2>
        </div>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>

      <div class="toolbar">
        <div class="section-title-group">
          <button type="button" class="ghost filter-toggle" (click)="showFilters.set(!showFilters())" [attr.aria-expanded]="showFilters()">
            <span class="filter-icon" aria-hidden="true"></span>
            <span>Filtres</span>
          </button>
          <button type="button" (click)="openCreatePanel()">Creer un nouveau produit</button>
        </div>

        <p class="summary">
          {{ filteredProducts().length }} produit{{ filteredProducts().length > 1 ? 's' : '' }}
          <span *ngIf="filteredProducts().length !== products().length">sur {{ products().length }}</span>
        </p>

        <div class="pager" *ngIf="totalPages() > 1">
          <button type="button" class="ghost" (click)="previousPage()" [disabled]="currentPage() === 1">Precedent</button>
          <span>Page {{ currentPage() }} / {{ totalPages() }}</span>
          <button type="button" class="ghost" (click)="nextPage()" [disabled]="currentPage() === totalPages()">Suivant</button>
        </div>
      </div>

      <div class="filters-grid collapsible" [class.is-collapsed]="!showFilters()" [formGroup]="filters">
        <label class="field">
          <span>Recherche</span>
          <input formControlName="term" placeholder="Nom ou description" />
        </label>

        <label class="field">
          <span>Categorie</span>
          <input formControlName="category" list="product-category-options" placeholder="Categorie" />
        </label>

        <label class="field">
          <span>Statut</span>
          <select formControlName="status">
            <option value="">Tous les statuts</option>
            <option value="ACTIVE">Actif</option>
            <option value="INACTIVE">Inactif</option>
          </select>
        </label>

        <label class="field">
          <span>Taille de page</span>
          <select formControlName="pageSize">
            <option *ngFor="let size of pageSizeOptions" [value]="size">{{ size }} / page</option>
          </select>
        </label>
      </div>

      <datalist id="product-category-options">
        <option *ngFor="let category of categoryOptions()" [value]="category"></option>
      </datalist>

      <div class="grid" [class.panel-open]="isPanelOpen()">
        <article class="panel list-panel">
          <div class="panel-header">
            <h3>Liste</h3>
          </div>

          <table class="table">
            <thead>
              <tr>
                <th>Nom</th>
                <th>Description</th>
                <th>Categorie</th>
                <th>Prix</th>
                <th>Statut</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of paginatedProducts()" [class.active]="selected()?.productId === item.productId">
                <td><strong>{{ item.name }}</strong></td>
                <td class="description-cell">{{ item.description || 'Aucune description' }}</td>
                <td class="muted">{{ item.category || 'Non renseignee' }}</td>
                <td>{{ item.price | number:'1.2-2' }} EUR</td>
                <td>
                  <span class="status-badge" [class.active]="item.status === 'ACTIVE'" [class.inactive]="item.status === 'INACTIVE'">
                    {{ statusLabel(item.status) }}
                  </span>
                </td>
                <td><button type="button" (click)="edit(item)">Modifier</button></td>
              </tr>

              <tr *ngIf="paginatedProducts().length === 0">
                <td colspan="6" class="empty">Aucun produit pour ce filtre.</td>
              </tr>
            </tbody>
          </table>
        </article>

        <form class="panel side-panel form" [class.open]="isPanelOpen()" [formGroup]="form" (ngSubmit)="save()" [attr.aria-hidden]="!isPanelOpen()">
          <div class="side-panel-header">
            <h3>{{ selected() ? 'Edition produit' : 'Creation produit' }}</h3>
            <button type="button" class="icon-button" (click)="closePanel()">Fermer</button>
          </div>

          <label>
            Nom
            <input formControlName="name" placeholder="Nom du produit" />
          </label>

          <label>
            Description
            <textarea formControlName="description" rows="3" placeholder="Description courte du produit"></textarea>
          </label>

          <label>
            Categorie
            <input formControlName="category" list="product-category-options" placeholder="Categorie" />
          </label>

          <label>
            Prix
            <input formControlName="price" type="number" step="0.01" min="0.01" placeholder="Prix" />
          </label>

          <div class="actions">
            <button type="submit" [disabled]="form.invalid">Enregistrer</button>
            <button *ngIf="selected()" type="button" class="secondary" (click)="toggleStatus()">
              {{ selected()?.status === 'ACTIVE' ? 'Desactiver' : 'Activer' }}
            </button>
            <button *ngIf="selected()" type="button" class="danger" (click)="deleteProduct()">Supprimer</button>
          </div>
        </form>
      </div>
    </section>
  `,
  styles: `
    .page, .form { display: grid; gap: 1rem; }
    .hero { display: grid; gap: 1rem; }
    .eyebrow { margin: 0; color: #ff7b00; text-transform: uppercase; letter-spacing: 0.15em; font-size: 0.72rem; }
    input, select, textarea { border: 1px solid #cbd5e1; border-radius: 0.85rem; padding: 0.7rem 0.82rem; font: inherit; background: #fff; }
    textarea { resize: vertical; min-height: 5.2rem; }
    .toolbar { display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap; }
    .summary { margin: 0; color: #334155; font-weight: 600; }
    .pager { display: inline-flex; align-items: center; gap: 0.75rem; color: #475569; }
    .section-title-group { display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
    .filters-grid { grid-template-columns: repeat(4, max-content); justify-content: start; }
    .filters-grid .field input, .filters-grid .field select { width: auto; min-width: 10.5rem; max-width: 13rem; border-radius: 999px; background: #fff; padding: 0.62rem 0.8rem !important; }
    .grid { display: grid; grid-template-columns: minmax(0, 1fr) 0fr; gap: 1.25rem; align-items: start; transition: grid-template-columns 220ms ease-out; }
    .grid.panel-open { grid-template-columns: minmax(0, 1.5fr) minmax(24rem, 0.95fr); }
    .panel { background: rgba(255,255,255,0.84); border-radius: 1.2rem; padding: 1.1rem; }
    .list-panel { overflow: hidden; }
    .panel-header, .side-panel-header { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
    .panel-header h3, .side-panel-header h3 { margin: 0; }
    .table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 1rem; overflow: hidden; table-layout: fixed; }
    th, td { padding: 0.5rem 0.58rem; border-bottom: 1px solid #e2e8f0; text-align: left; vertical-align: middle; line-height: 1.15; }
    th { font-size: 0.72rem; letter-spacing: 0.08em; text-transform: uppercase; color: #64748b; }
    tbody tr { transition: background 160ms ease; }
    tbody tr:hover, tbody tr.active { background: #fff7ed; }
    strong { display: block; line-height: 1.15; }
    .description-cell { color: #475569; font-size: 0.84rem; }
    .muted { color: #64748b; }
    .empty { text-align: center; color: #64748b; padding: 1rem; }
    .side-panel {
      overflow: hidden;
      opacity: 0;
      transform: translateX(14px) scale(0.985);
      transform-origin: right center;
      pointer-events: none;
      max-width: 0;
      padding-inline: 0;
      transition:
        opacity 170ms ease-out,
        transform 220ms ease-out,
        max-width 220ms ease-out,
        padding-inline 220ms ease-out;
    }
    .side-panel.open {
      opacity: 1;
      transform: translateX(0) scale(1);
      pointer-events: auto;
      max-width: 100%;
      padding-inline: 1.1rem;
    }
    label { display: grid; gap: 0.35rem; color: #1e293b; font-weight: 600; }
    .actions { display: flex; gap: 0.75rem; flex-wrap: wrap; }
    .error { margin: 0; color: #991b1b; font-weight: 700; }
    .status-badge { display: inline-flex; align-items: center; padding: 0.22rem 0.56rem; border-radius: 999px; font-size: 0.7rem; font-weight: 800; letter-spacing: 0.01em; }
    .status-badge.active { background: linear-gradient(135deg, #dcfce7, #bbf7d0); color: #166534; box-shadow: inset 0 0 0 1px rgba(22, 101, 52, 0.08); }
    .status-badge.inactive { background: linear-gradient(135deg, #fee2e2, #fecaca); color: #991b1b; box-shadow: inset 0 0 0 1px rgba(153, 27, 27, 0.08); }

    @media (max-width: 1000px) {
      .filters-grid { grid-template-columns: 1fr; }
      .filters-grid .field input, .filters-grid .field select { width: 100%; min-width: 0; max-width: none; }
      .grid, .grid.panel-open { grid-template-columns: 1fr; }
      .side-panel, .side-panel.open { max-width: none; padding-inline: 1.1rem; opacity: 1; transform: none; }
      .side-panel:not(.open) { display: none; }
    }
  `,
})
export class ProductsPageComponent {
  private readonly api = inject(ProductsApiService);
  private readonly fb = inject(FormBuilder);

  readonly pageSizeOptions = [5, 10, 20, 50];
  readonly products = signal<Product[]>([]);
  readonly selected = signal<Product | null>(null);
  readonly isPanelOpen = signal(false);
  readonly showFilters = signal(false);
  readonly page = signal(1);
  readonly error = signal('');
  readonly filters = this.fb.nonNullable.group({ term: [''], status: [''], category: [''], pageSize: [10] });
  readonly filterState = signal(this.filters.getRawValue());
  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    description: [''],
    category: [''],
    price: [0, [Validators.required, Validators.min(0.01)]],
  });

  readonly categoryOptions = computed(() =>
    [...new Set(this.products().map((item) => item.category).filter((value) => value.trim().length > 0))]
      .sort((left, right) => left.localeCompare(right)),
  );

  readonly filteredProducts = computed(() => {
    const { term, status, category } = this.filterState();
    const normalizedTerm = term.trim().toLocaleLowerCase();
    const normalizedCategory = category.trim().toLocaleLowerCase();

    return this.products().filter((item) => {
      const matchesTerm = normalizedTerm.length === 0
        || item.name.toLocaleLowerCase().includes(normalizedTerm)
        || item.description.toLocaleLowerCase().includes(normalizedTerm);
      const matchesCategory = normalizedCategory.length === 0 || item.category.toLocaleLowerCase().includes(normalizedCategory);
      const matchesStatus = !status || item.status === status;
      return matchesTerm && matchesCategory && matchesStatus;
    });
  });

  readonly totalPages = computed(() => {
    const pageSize = Number(this.filterState().pageSize) || 10;
    return Math.max(1, Math.ceil(this.filteredProducts().length / pageSize));
  });

  readonly currentPage = computed(() => Math.min(this.page(), this.totalPages()));

  readonly paginatedProducts = computed(() => {
    const pageSize = Number(this.filterState().pageSize) || 10;
    const start = (this.currentPage() - 1) * pageSize;
    return this.filteredProducts().slice(start, start + pageSize);
  });

  constructor() {
    this.filters.valueChanges.subscribe((value) => {
      this.filterState.set({
        term: value.term ?? '',
        status: value.status ?? '',
        category: value.category ?? '',
        pageSize: Number(value.pageSize ?? 10),
      });
      this.page.set(1);
    });

    this.load();
  }

  load(): void {
    this.api.search('', '', '').subscribe({
      next: (items) => {
        this.products.set(items);
        this.error.set('');
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveError(error, 'Chargement impossible')),
    });
  }

  edit(item: Product): void {
    this.selected.set(item);
    this.form.patchValue({ name: item.name, description: item.description, category: item.category, price: item.price });
    this.isPanelOpen.set(true);
  }

  openCreatePanel(): void {
    this.selected.set(null);
    this.form.reset({ name: '', description: '', category: '', price: 0 });
    this.error.set('');
    this.isPanelOpen.set(true);
  }

  closePanel(): void {
    this.selected.set(null);
    this.isPanelOpen.set(false);
    this.error.set('');
  }

  previousPage(): void {
    this.page.update((page) => Math.max(1, page - 1));
  }

  nextPage(): void {
    this.page.update((page) => Math.min(this.totalPages(), page + 1));
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = this.form.getRawValue();
    const current = this.selected();
    const request = current ? this.api.update(current.productId, payload) : this.api.create(payload);
    request.subscribe({
      next: () => {
        this.closePanel();
        this.load();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveError(error, 'Sauvegarde impossible')),
    });
  }

  toggleStatus(): void {
    const current = this.selected();
    if (!current) {
      return;
    }

    const request = current.status === 'ACTIVE'
      ? this.api.deactivate(current.productId)
      : this.api.activate(current.productId);

    request.subscribe({
      next: (updated) => {
        this.selected.set(updated);
        this.load();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveError(error, 'Mise a jour du statut impossible')),
    });
  }

  deleteProduct(): void {
    const current = this.selected();
    if (!current) {
      return;
    }
    if (!confirm(`Supprimer le produit ${current.name} ?`)) {
      return;
    }

    this.api.delete(current.productId).subscribe({
      next: () => {
        this.closePanel();
        this.load();
      },
      error: (error: HttpErrorResponse) => this.error.set(this.resolveError(error, 'Suppression impossible')),
    });
  }

  statusLabel(status: Product['status']): string {
    return status === 'ACTIVE' ? 'Actif' : 'Inactif';
  }

  private resolveError(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error?.message === 'string' && error.error.message.trim().length > 0) {
      return error.error.message;
    }
    if (typeof error.error === 'string' && error.error.trim().length > 0) {
      return error.error;
    }
    if (error.status === 0) {
      return 'Serveur inaccessible ou non redemarre.';
    }
    if (error.status === 404) {
      return 'Endpoint introuvable. Redemarre probablement le backend pour charger la suppression.';
    }
    if (error.status === 403) {
      return 'Action reservee a un administrateur.';
    }
    return fallback;
  }
}
