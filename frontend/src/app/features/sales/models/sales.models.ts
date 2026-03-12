export interface Sale {
  saleId: string;
  customerId: string;
  type: string;
  soldAt: string;
  totalAmount: number;
  lines: { label: string; quantity: number; unitPrice: number; totalPrice: number }[];
}

export interface ConnectionPricingTier {
  hours: number;
  minutes: number;
  durationMinutes: number;
  price: number;
}

export interface ConnectionPricing {
  tiers: ConnectionPricingTier[];
}
