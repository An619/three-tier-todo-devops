import React, { createContext, useState, useContext, ReactNode, useEffect, useCallback } from 'react';
import { Task, Priority, Status, Category, TaskResponse } from '../types/task';
import { toast } from 'sonner';
import { taskApi } from '@/services/api';

interface TaskContextType {
  tasks: Task[];
  loading: boolean;
  error: string | null;
  addTask: (task: Omit<Task, 'id' | 'createdAt'>) => Promise<void>;
  updateTask: (task: Task) => Promise<void>;
  deleteTask: (id: string) => Promise<void>;
  getTask: (id: string) => Task | undefined;
  filterTasks: (status?: Status, priority?: Priority, category?: Category, searchTerm?: string) => Task[];
  refreshTasks: () => Promise<void>;
}
const TaskContext = createContext<TaskContextType | undefined>(undefined);
const convertResponseToTask = (response: TaskResponse): Task => ({
  id: response.id,
  title: response.title,
  description: response.description,
  priority: response.priority as Priority,
  status: response.status as Status,
  category: response.category as Category,
  createdAt: new Date(response.createdAt),
  dueDate: response.dueDate ? new Date(response.dueDate) : undefined,
});

export const TaskProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refreshTasks = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await taskApi.getAllTasks();
      setTasks(response.map(convertResponseToTask));
    } catch (err) {
      console.error('Failed to fetch tasks from task-service:', err);
      setError('Unable to load tasks from the task service. Check service health and sign in again if your session expired.');
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refreshTasks().catch(() => undefined);
  }, [refreshTasks]);

  const addTask = async (task: Omit<Task, 'id' | 'createdAt'>) => {
    setLoading(true);
    setError(null);
    try {
      const dueDate = task.dueDate ? task.dueDate.toISOString() : undefined;
      const response = await taskApi.createTask({ ...task, dueDate });
      setTasks(prev => [convertResponseToTask(response), ...prev]);
      toast.success('Task created successfully');
    } catch (err) {
      setError('Task could not be saved by the task service.');
      toast.error('Failed to create task');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const updateTask = async (updatedTask: Task) => {
    setLoading(true);
    setError(null);
    try {
      const dueDate = updatedTask.dueDate ? updatedTask.dueDate.toISOString() : undefined;
      const response = await taskApi.updateTask({
        id: updatedTask.id,
        title: updatedTask.title,
        description: updatedTask.description,
        priority: updatedTask.priority,
        status: updatedTask.status,
        category: updatedTask.category,
        dueDate,
      });
      const converted = convertResponseToTask(response);
      setTasks(prev => prev.map(task => task.id === converted.id ? converted : task));
      toast.success('Task updated successfully');
    } catch (err) {
      setError('Task could not be updated by the task service.');
      toast.error('Failed to update task');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const deleteTask = async (id: string) => {
    setLoading(true);
    setError(null);
    try {
      await taskApi.deleteTask(id);
      setTasks(prev => prev.filter(task => task.id !== id));
      toast.success('Task deleted successfully');
    } catch (err) {
      setError('Task could not be deleted by the task service.');
      toast.error('Failed to delete task');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const getTask = (id: string) => tasks.find(task => task.id === id);
  const filterTasks = (status?: Status, priority?: Priority, category?: Category, searchTerm?: string) =>
    tasks.filter(task => {
      const matchesStatus = !status || task.status === status;
      const matchesPriority = !priority || task.priority === priority;
      const matchesCategory = !category || task.category === category;
      const matchesSearch = !searchTerm ||
        task.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (task.description && task.description.toLowerCase().includes(searchTerm.toLowerCase()));
      return matchesStatus && matchesPriority && matchesCategory && matchesSearch;
    });

  return (
    <TaskContext.Provider value={{ tasks, loading, error, addTask, updateTask, deleteTask, getTask, filterTasks, refreshTasks }}>
      {children}
    </TaskContext.Provider>
  );
};

export const useTaskContext = (): TaskContextType => {
  const context = useContext(TaskContext);
  if (context === undefined) throw new Error('useTaskContext must be used within a TaskProvider');
  return context;
};
