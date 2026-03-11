export interface CafeSession {
  sessionId: string;
  customerId: string;
  customerName: string;
  customerType: 'WALK_IN' | 'SUBSCRIBER';
  remainingMinutes: number;
  startedAt: string;
  endedAt: string | null;
  consumedMinutes: number;
  calculatedPrice: number;
}
