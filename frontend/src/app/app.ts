import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe, LowerCasePipe } from '@angular/common';
import { Observable } from 'rxjs';
import { Api, AppNotification, Claim, Exposure, HistoryEntry, NewClaim, Role, Workload } from './api';

const today = () => new Date().toISOString().slice(0, 10);

@Component({
  selector: 'app-root',
  imports: [FormsModule, DatePipe, DecimalPipe, LowerCasePipe],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private api = inject(Api);

  readonly roles: Role[] = ['CLAIMANT', 'OFFICER', 'MANAGER'];
  readonly markets = ['MY', 'SG', 'HK', 'TH', 'ID', 'PH'];
  readonly today = today();

  // who we're acting as
  userId = 'alice';
  role: Role = 'CLAIMANT';
  activeRole = signal<Role>('CLAIMANT');
  activeUser = signal('alice');

  // data from the API
  claims = signal<Claim[]>([]);
  queue = signal<Claim[]>([]);
  selected = signal<Claim | null>(null);
  history = signal<HistoryEntry[]>([]);
  notifications = signal<AppNotification[]>([]);
  workload = signal<Workload | null>(null);
  exposure = signal<Exposure[]>([]);
  error = signal<string | null>(null);
  flash = signal<string | null>(null);

  // form fields
  form: NewClaim = this.emptyForm();
  text = '';
  amount: number | null = null;

  constructor() {
    this.switchUser();
  }

  switchUser() {
    const id = this.userId.trim() || 'anonymous';
    this.api.user = { id, role: this.role };
    this.activeUser.set(id);
    this.activeRole.set(this.role);
    this.selected.set(null);
    this.claims.set([]);
    this.queue.set([]);
    this.refresh();
  }

  refresh() {
    this.error.set(null);
    switch (this.activeRole()) {
      case 'CLAIMANT':
        this.run(this.api.myClaims(), c => this.claims.set(c));
        this.run(this.api.notifications(), n => this.notifications.set(n));
        break;
      case 'OFFICER':
        this.run(this.api.queue(), q => this.queue.set(q));
        this.run(this.api.assigned(), c => this.claims.set(c));
        this.run(this.api.workload(), w => this.workload.set(w));
        break;
      case 'MANAGER':
        this.run(this.api.queue(), q => this.queue.set(q));
        this.run(this.api.workload(), w => this.workload.set(w));
        this.run(this.api.exposure(), e => this.exposure.set(e));
        break;
    }
    const s = this.selected();
    if (s) this.select(s);
  }

  select(claim: Claim) {
    this.text = '';
    this.amount = claim.approvedAmount ?? claim.claimedAmount;
    this.run(this.api.get(claim.id), c => this.selected.set(c));
    this.run(this.api.history(claim.id), h => this.history.set(h));
  }

  submit() {
    this.run(this.api.submit(this.form), c => {
      this.form = this.emptyForm();
      this.done(`Claim ${c.reference} submitted`, c);
    });
  }

  act(kind: 'provideInfo' | 'pickUp' | 'requestInfo' | 'approve' | 'reject' | 'settle') {
    const c = this.selected();
    if (!c) return;
    const calls: Record<typeof kind, () => Observable<Claim>> = {
      provideInfo: () => this.api.provideInfo(c.id, this.text),
      pickUp: () => this.api.pickUp(c.id),
      requestInfo: () => this.api.requestInfo(c.id, this.text),
      approve: () => this.api.approve(c.id, this.amount ?? 0, this.text),
      reject: () => this.api.reject(c.id, this.text),
      settle: () => this.api.settle(c.id, this.amount ?? 0),
    };
    this.run(calls[kind](), updated => this.done(`${updated.reference} is now ${this.label(updated.status)}`, updated));
  }

  isMine(c: Claim) {
    return this.activeRole() === 'OFFICER' && c.assignedOfficer === this.activeUser();
  }

  label(status: string) {
    return status.replace('_', ' ').toLowerCase();
  }

  private done(message: string, claim: Claim) {
    this.flash.set(message);
    setTimeout(() => this.flash.set(null), 3000);
    this.selected.set(claim);
    this.refresh();
  }

  private run<T>(obs: Observable<T>, ok: (v: T) => void) {
    obs.subscribe({
      next: ok,
      error: e => this.error.set(e.error?.detail ?? e.error?.title ?? e.message),
    });
  }

  private emptyForm(): NewClaim {
    return { market: 'MY', productType: 'MOTOR', incidentDate: today(), description: '', claimedAmount: null };
  }
}
