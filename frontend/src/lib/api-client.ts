import axios from "axios";
import { API_BASE_URL } from "@/lib/constants";

// No accounts in the MVP: identity is the `ks_anon_id` httponly cookie set by
// the backend, so requests must include credentials for it to be sent/received.
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30_000,
  withCredentials: true,
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error?.response?.data?.detail ?? error?.message ?? "An unknown error occurred.";
    return Promise.reject(new Error(message));
  },
);
