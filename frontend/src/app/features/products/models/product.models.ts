export interface Product {
  productId: string;
  name: string;
  price: number;
  category: string;
  description: string;
  status: 'ACTIVE' | 'INACTIVE';
}
