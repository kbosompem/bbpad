// API configuration for BBPad
const API_BASE_URL = import.meta.env.PROD ? '' : '/api';

export const apiConfig = {
  baseUrl: API_BASE_URL,
  endpoints: {
    execute: `${API_BASE_URL}/execute`,
    scripts: {
      save: `${API_BASE_URL}/scripts/save`,
      get: (id: string) => `${API_BASE_URL}/scripts/get/${id}`,
      list: `${API_BASE_URL}/scripts/list`,
    },
    connections: {
      list: `${API_BASE_URL}/connections`,
      test: `${API_BASE_URL}/connections/test`,
      create: `${API_BASE_URL}/connections`,
      update: `${API_BASE_URL}/connections/update`,
      remove: `${API_BASE_URL}/connections/remove`,
      schema: `${API_BASE_URL}/schema`,
    },
    tabs: {
      load: `${API_BASE_URL}/tabs/load`,
      save: `${API_BASE_URL}/tabs/save`,
    },
  },
};