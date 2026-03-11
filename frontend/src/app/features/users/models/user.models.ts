export interface UserResponse {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  status: 'ACTIVE' | 'DISABLED';
}

export interface CreateUserPayload {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
  roles: string[];
}

export interface UpdateUserPayload {
  email: string;
  firstName: string;
  lastName: string;
  password?: string;
  roles: string[];
}
