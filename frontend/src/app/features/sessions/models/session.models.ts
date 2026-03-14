export interface CafeSession {
  sessionId: string;
  customerId: string;
  customerName: string;
  customerType: 'WALK_IN' | 'SUBSCRIBER';
  remainingMinutes: number;
  displayRemainingMinutes: number;
  startedAt: string;
  endedAt: string | null;
  paused: boolean;
  paid: boolean;
  consumedSeconds: number;
  consumedMinutes: number;
  calculatedPrice: number;
  purchasesAmount: number;
  openDebtAmount: number;
  totalAmountDue: number;
  totalPaidAmount: number;
}
