export interface SubscriptionOffer {
  offerId: string;
  name: string;
  price: number;
  includedMinutes: number;
  status: 'ACTIVE' | 'INACTIVE';
}
