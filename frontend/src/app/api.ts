import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

export type Role = 'CLAIMANT' | 'OFFICER' | 'MANAGER';

export interface Claim {
  id: string; reference: string; claimantId: string; market: string; productType: string;
  status: string; incidentDate: string; description: string;
  claimedAmount: number; approvedAmount: number | null; settledAmount: number | null;
  currency: string; assignedOfficer: string | null; createdAt: string; updatedAt: string;
}
export interface HistoryEntry {
  fromStatus: string | null; toStatus: string; actorId: string; actorRole: Role; note: string | null; createdAt: string;
}
export interface AppNotification { id: number; message: string; createdAt: string; }
export interface OfficerStats {
  officerId: string; underReview: number; infoRequested: number; awaitingSettlement: number;
  open: number; closed: number; avgDaysToClose: number | null;
}
export interface Workload { unassigned: number; officers: OfficerStats[]; }
export interface Exposure { market: string; currency: string; openClaims: number; exposure: number; }
export interface NewClaim {
  market: string; productType: string; incidentDate: string; description: string; claimedAmount: number | null;
}

/** Thin wrapper over the REST API. The user is sent as headers (mock auth, see README). */
@Injectable({ providedIn: 'root' })
export class Api {
  private http = inject(HttpClient);
  user: { id: string; role: Role } = { id: 'alice', role: 'CLAIMANT' };

  private get opts() {
    return { headers: new HttpHeaders({ 'X-User-Id': this.user.id, 'X-User-Role': this.user.role }) };
  }

  myClaims() { return this.http.get<Claim[]>('/api/claims/mine', this.opts); }
  queue() { return this.http.get<Claim[]>('/api/claims/queue', this.opts); }
  assigned() { return this.http.get<Claim[]>('/api/claims/assigned', this.opts); }
  get(id: string) { return this.http.get<Claim>(`/api/claims/${id}`, this.opts); }
  history(id: string) { return this.http.get<HistoryEntry[]>(`/api/claims/${id}/history`, this.opts); }
  notifications() { return this.http.get<AppNotification[]>('/api/notifications', this.opts); }
  workload() { return this.http.get<Workload>('/api/dashboard/workload', this.opts); }
  exposure() { return this.http.get<Exposure[]>('/api/dashboard/exposure', this.opts); }

  submit(body: NewClaim) { return this.http.post<Claim>('/api/claims', body, this.opts); }
  provideInfo(id: string, message: string) { return this.http.post<Claim>(`/api/claims/${id}/info`, { message }, this.opts); }
  pickUp(id: string) { return this.http.post<Claim>(`/api/claims/${id}/pick-up`, {}, this.opts); }
  requestInfo(id: string, message: string) { return this.http.post<Claim>(`/api/claims/${id}/request-info`, { message }, this.opts); }
  approve(id: string, amount: number, note: string) { return this.http.post<Claim>(`/api/claims/${id}/approve`, { amount, note }, this.opts); }
  reject(id: string, message: string) { return this.http.post<Claim>(`/api/claims/${id}/reject`, { message }, this.opts); }
  settle(id: string, amount: number) { return this.http.post<Claim>(`/api/claims/${id}/settle`, { amount }, this.opts); }
}
