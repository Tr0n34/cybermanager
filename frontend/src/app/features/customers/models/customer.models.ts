export interface Customer {
  customerId: string;
  name: string;
  type: 'WALK_IN' | 'SUBSCRIBER';
  status: 'ACTIVE' | 'INACTIVE';
  remainingMinutes: number;
  openDebtAmount: number;
}

export interface CustomerPurchase {
  saleId: string;
  sessionId: string | null;
  type: string;
  label: string;
  debtLabel: string;
  soldAt: string;
  totalAmount: number;
  openDebt: boolean;
}

export interface CustomerDetails extends Customer {
  currentSubscriptionLabel: string | null;
  purchases: CustomerPurchase[];
  debts: { debtId: string; label: string; amount: number; status: 'OPEN' | 'SETTLED'; createdAt: string; settledAt: string | null }[];
}
