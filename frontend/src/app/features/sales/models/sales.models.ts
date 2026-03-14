export interface Sale {
  saleId: string;
  customerId: string;
  type: string;
  soldAt: string;
  totalAmount: number;
  lines: { label: string; quantity: number; unitPrice: number; totalPrice: number }[];
}

export interface ConnectionPricingTier {
  id: number | null;
  hours: number;
  minutes: number;
  durationMinutes: number;
  price: number;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface ConnectionPricing {
  tiers: ConnectionPricingTier[];
}
