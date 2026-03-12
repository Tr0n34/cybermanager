export interface DebtItem {
  debtId: string;
  label: string;
  amount: number;
  status: 'OPEN' | 'SETTLED';
  createdAt: string;
  settledAt: string | null;
}

export interface CustomerDebtSummary {
  customerId: string;
  customerName: string;
  customerType: 'WALK_IN' | 'SUBSCRIBER';
  totalOpenDebt: number;
  debts: DebtItem[];
}
