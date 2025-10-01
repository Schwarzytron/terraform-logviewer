import { LogEntry, SearchFilters, LogResponse } from '../types/LogEntry';

const API_BASE = 'http://localhost:8080/api/logs';

export const logApi = {
  uploadLog: async (file: File): Promise<LogResponse> => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${API_BASE}/upload`, {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`Upload failed: ${response.statusText}`);
    }

    return response.json();
  },

  advancedSearch: async (logFileId: string, filters: SearchFilters): Promise<{ content: LogEntry[] }> => {
    const response = await fetch(`${API_BASE}/search/advanced?logFileId=${logFileId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(filters),
    });

    if (!response.ok) {
      throw new Error(`Search failed: ${response.statusText}`);
    }

    return response.json();
  },

  getResourceTypes: async (): Promise<string[]> => {
    const response = await fetch(`${API_BASE}/resource-types`);
    if (!response.ok) throw new Error('Failed to fetch resource types');
    return response.json();
  },
};