export type InvoiceStatus = 'DRAFT' | 'ISSUED' | 'CANCELLED';

export interface InvoiceSummary {
  invoiceId: string;
  saleId: string;
  invoiceNumber: string;
  customerName: string;
  issuedAt: string;
  status: InvoiceStatus;
  totalAmount: number;
}

export interface InvoiceLine {
  label: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface InvoiceDetail {
  invoiceId: string;
  saleId: string;
  customerId: string | null;
  invoiceNumber: string;
  customerName: string;
  issuedAt: string;
  status: InvoiceStatus;
  totalAmount: number;
  lines: InvoiceLine[];
}

export interface InvoiceSearchRequest {
  invoiceNumber?: string;
  customerName?: string;
  status?: '' | InvoiceStatus;
  startDate?: string;
  endDate?: string;
}

export interface ManualInvoiceLinePayload {
  label: string;
  quantity: number;
  unitPrice: number;
}

export interface CreateInvoicePayload {
  customerId?: string | null;
  customerName: string;
  lines: ManualInvoiceLinePayload[];
}
