import axios from "axios";

import { API_BASE_PATH } from "./config";

export const http = axios.create({ baseURL: API_BASE_PATH });

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
