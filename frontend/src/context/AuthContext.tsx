
import React, { createContext, useState, useContext, useEffect } from 'react';

interface User {
  id: string;
  username: string;
  email: string;
}

interface StoredUser extends User {
  password: string;
}

const readStoredUsers = (): StoredUser[] => {
  try {
    const parsed: unknown = JSON.parse(localStorage.getItem('users') || '[]');
    if (!Array.isArray(parsed)) return [];
    return parsed.filter((item): item is StoredUser =>
      typeof item === 'object' &&
      item !== null &&
      'id' in item && typeof item.id === 'string' &&
      'username' in item && typeof item.username === 'string' &&
      'email' in item && typeof item.email === 'string' &&
      'password' in item && typeof item.password === 'string'
    );
  } catch {
    return [];
  }
};

interface AuthContextType {
  user: User | null;
  login: (email: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  loading: boolean;
  error: string | null;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Check for stored user on component mount
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
    setLoading(false);
  }, []);

  const register = async (username: string, email: string, password: string) => {
    setLoading(true);
    setError(null);
    try {
      // In a real app, this would be an API call to your backend
      // For now, we'll simulate a successful registration with dummy data
      if (username && email && password) {
        // Check if user already exists
        const users = readStoredUsers();
        const userExists = users.find((storedUser) => storedUser.email === email);
        
        if (userExists) {
          throw new Error('User with this email already exists');
        }
        
        // Create a new user
        const newUser = {
          id: Date.now().toString(),
          username,
          email,
          password // In a real app, you'd hash this password
        };
        
        // Store user in "database"
        users.push(newUser);
        localStorage.setItem('users', JSON.stringify(users));
        
        // Create user session without password
        const userSession = {
          id: newUser.id,
          username: newUser.username,
          email: newUser.email
        };
        
        localStorage.setItem('user', JSON.stringify(userSession));
        setUser(userSession);
      } else {
        throw new Error('All fields are required');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred during registration');
      throw err; // Re-throw to handle in component
    } finally {
      setLoading(false);
    }
  };

  const login = async (email: string, password: string) => {
    setLoading(true);
    setError(null);
    try {
      // In a real app, this would be an API call to your backend
      // For now, we'll check against our local storage "database"
      const users = readStoredUsers();
      const user = users.find((storedUser) => storedUser.email === email && storedUser.password === password);
      
      if (user) {
        // Create user session without password
        const userSession = {
          id: user.id,
          username: user.username,
          email: user.email
        };
        
        localStorage.setItem('user', JSON.stringify(userSession));
        setUser(userSession);
      } else {
        throw new Error('Invalid email or password');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred during login');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, register, logout, loading, error }}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthContext;
