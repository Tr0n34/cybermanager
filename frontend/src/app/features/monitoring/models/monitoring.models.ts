export interface DayCustomer {
  customerId: string;
  name: string;
  type: string;
  remainingMinutes: number;
  consumedMinutes: number;
  purchasesTotal: number;
  activeSession: boolean;
}
