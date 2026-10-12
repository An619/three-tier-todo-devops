import { CreateTaskRequest, TaskResponse, UpdateTaskRequest } from '@/types/task';

// Use a relative URL so the browser calls the same origin in Compose and Kubernetes.
// Nginx routes /api requests to the internal backend service.
const API_BASE_URL = '/api';

const handleResponse = async (response: Response) => {
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'API request failed');
  }
  if (response.status === 204) return undefined;
  return response.json();
};

export const taskApi = {
  getAllTasks: async (): Promise<TaskResponse[]> => {
    const response = await fetch(`${API_BASE_URL}/tasks`);
    return handleResponse(response);
  },
  getTaskById: async (id: string): Promise<TaskResponse> => {
    const response = await fetch(`${API_BASE_URL}/tasks/${id}`);
    return handleResponse(response);
  },
  createTask: async (task: CreateTaskRequest): Promise<TaskResponse> => {
    const response = await fetch(`${API_BASE_URL}/tasks`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(task),
    });
    return handleResponse(response);
  },
  updateTask: async (task: UpdateTaskRequest): Promise<TaskResponse> => {
    const response = await fetch(`${API_BASE_URL}/tasks/${task.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(task),
    });
    return handleResponse(response);
  },
  deleteTask: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/tasks/${id}`, { method: 'DELETE' });
    await handleResponse(response);
  },
};
