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
  debt: boolean;
  soldAt: string;
}

export interface MonitoringSessionDetail {
  sessionLabel: string;
  startedAt: string;
  endedAt: string;
}

export interface MonitoringCustomerDetail {
  customerId: string;
  name: string;
  sales: MonitoringSaleDetail[];
  sessions: MonitoringSessionDetail[];
  totalCollected: number;
  totalDebtCreated: number;
}
