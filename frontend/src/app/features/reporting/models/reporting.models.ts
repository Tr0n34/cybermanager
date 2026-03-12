export interface DayCustomerHistory {
  customerId: string;
  name: string;
  type: string;
  totalMinutes: number;
  salesTotal: number;
  debtTotal: number;
  collectedTotal: number;
  state: string;
}

export interface ReportingSaleDetail {
  label: string;
  quantity: number;
  totalPrice: number;
}

export interface ReportingCustomerDetail {
  customerId: string;
  name: string;
  sales: ReportingSaleDetail[];
  sessions: string[];
  totalCollected: number;
  totalDebtCreated: number;
}

export interface DayHistoryResponse {
  startDate: string;
  endDate: string;
  customers: DayCustomerHistory[];
  totalCollected: number;
  totalDebtCreated: number;
}
