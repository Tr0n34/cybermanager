export interface AuthenticationResponse {
  token: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
}

export interface LoginPayload {
  email: string;
  password: string;
}
