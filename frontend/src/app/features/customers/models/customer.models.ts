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
  type: string;
  label: string;
  soldAt: string;
  totalAmount: number;
}

export interface CustomerDetails extends Customer {
  currentSubscriptionLabel: string | null;
  purchases: CustomerPurchase[];
  debts: { debtId: string; label: string; amount: number; status: 'OPEN' | 'SETTLED'; createdAt: string; settledAt: string | null }[];
}
