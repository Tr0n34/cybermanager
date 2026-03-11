import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, input } from '@angular/core';

@Component({
  selector: 'app-role-selector',
  imports: [CommonModule],
  template: `
    <div class="roles">
      <label *ngFor="let role of rolesList">
        <input type="checkbox" [checked]="selected().includes(role)" (change)="toggle(role, $event)" />
        <span>{{ role }}</span>
      </label>
    </div>
  `,
  styles: `.roles { display: flex; gap: 1rem; flex-wrap: wrap; } label { display: inline-flex; gap: 0.45rem; align-items: center; }`,
})
export class RoleSelectorComponent {
  readonly selected = input<string[]>([]);
  readonly rolesList = ['ADMIN', 'EMPLOYEE'];

  @Output() readonly selectedChange = new EventEmitter<string[]>();

  toggle(role: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const next = checked
      ? [...this.selected(), role]
      : this.selected().filter((item) => item !== role);
    this.selectedChange.emit(next);
  }
}
