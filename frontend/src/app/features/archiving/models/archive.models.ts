export interface ArchiveCandidate {
  customerId: string;
  name: string;
  type: 'WALK_IN' | 'SUBSCRIBER';
  status: 'ACTIVE' | 'INACTIVE';
  remainingMinutes: number;
  latestActivityAt: string;
  sessionCount: number;
  saleCount: number;
  debtCount: number;
  salesTotal: number;
  debtTotal: number;
}

export interface GeneratedArchiveFile {
  id: string;
  fileName: string;
  fileType: string;
  generatedBy: string;
  generatedAt: string;
}

export interface ArchiveRequest {
  startDate: string;
  endDate: string;
  type: '' | 'WALK_IN';
  format?: 'csv' | 'xlsx';
}
