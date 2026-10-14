import { CreateTaskRequest, TaskResponse, UpdateTaskRequest } from '@/types/task';
const API_BASE_URL = '/api';
export interface AuthUser { id: string; username: string; email: string; }
export interface AuthResponse { token: string; tokenType: 'Bearer'; expiresIn: number; user: AuthUser; }
const handleResponse = async <T>(response: Response): Promise<T> => {
  if (!response.ok) { const errorData = await response.json().catch(() => ({})); throw new Error(errorData.message || 'API request failed'); }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
};
const authHeaders = (): Record<string, string> => {
  const token = localStorage.getItem('access_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
};
export const authApi = {
  register: async (username: string, email: string, password: string): Promise<AuthResponse> =>
    handleResponse<AuthResponse>(await fetch(`${API_BASE_URL}/auth/register`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username, email, password }) })),
  login: async (email: string, password: string): Promise<AuthResponse> =>
    handleResponse<AuthResponse>(await fetch(`${API_BASE_URL}/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }) })),
  me: async (): Promise<AuthUser> => handleResponse<AuthUser>(await fetch(`${API_BASE_URL}/auth/me`, { headers: authHeaders() })),
};
export const taskApi = {
  getAllTasks: async (): Promise<TaskResponse[]> => handleResponse<TaskResponse[]>(await fetch(`${API_BASE_URL}/tasks`, { headers: authHeaders() })),
  getTaskById: async (id: string): Promise<TaskResponse> => handleResponse<TaskResponse>(await fetch(`${API_BASE_URL}/tasks/${id}`, { headers: authHeaders() })),
  createTask: async (task: CreateTaskRequest): Promise<TaskResponse> => handleResponse<TaskResponse>(await fetch(`${API_BASE_URL}/tasks`, { method: 'POST', headers: { 'Content-Type': 'application/json', ...authHeaders() }, body: JSON.stringify(task) })),
  updateTask: async (task: UpdateTaskRequest): Promise<TaskResponse> => handleResponse<TaskResponse>(await fetch(`${API_BASE_URL}/tasks/${task.id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json', ...authHeaders() }, body: JSON.stringify(task) })),
  deleteTask: async (id: string): Promise<void> => handleResponse<void>(await fetch(`${API_BASE_URL}/tasks/${id}`, { method: 'DELETE', headers: authHeaders() })),
};
