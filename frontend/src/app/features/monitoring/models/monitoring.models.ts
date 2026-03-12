export interface DayCustomer {
  customerId: string;
  name: string;
  type: string;
  remainingMinutes: number;
  consumedMinutes: number;
  purchasesTotal: number;
  debtTotal: number;
  collectedTotal: number;
  state: string;
}

export interface MonitoringSaleDetail {
  label: string;
  quantity: number;
  totalPrice: number;
}

export interface MonitoringCustomerDetail {
  customerId: string;
  name: string;
  sales: MonitoringSaleDetail[];
  sessions: string[];
  totalCollected: number;
  totalDebtCreated: number;
}
