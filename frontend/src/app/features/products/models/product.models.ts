export interface Product {
  productId: string;
  name: string;
  price: number;
  category: string;
  status: 'ACTIVE' | 'INACTIVE';
}
