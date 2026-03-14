export interface CompanyProfile {
  id: string;
  legalName: string;
  siret: string | null;
  phone: string | null;
  email: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  postalCode: string | null;
  city: string | null;
  country: string | null;
  updatedAt: string;
}

export interface CompanyProfilePayload {
  legalName: string;
  siret: string;
  phone: string;
  email: string;
  addressLine1: string;
  addressLine2: string;
  postalCode: string;
  city: string;
  country: string;
}
