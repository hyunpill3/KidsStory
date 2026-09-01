import { apiClient } from "@/lib/api-client";
import type { CreateProjectPayload, Project, ProjectStatusResponse } from "@/types";

export async function createProject(payload: CreateProjectPayload): Promise<Project> {
  const { data } = await apiClient.post<Project>("/projects/", payload);
  return data;
}

export async function uploadProjectImages(
  projectId: string,
  files: File[],
): Promise<Project> {
  const formData = new FormData();
  files.forEach((file) => formData.append("files", file));

  const { data } = await apiClient.post<Project>(
    `/projects/${projectId}/upload/`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data;
}

export async function generateVideo(projectId: string): Promise<Project> {
  const { data } = await apiClient.post<Project>(`/projects/${projectId}/generate/`);
  return data;
}

export async function getProjectStatus(
  projectId: string,
): Promise<ProjectStatusResponse> {
  const { data } = await apiClient.get<ProjectStatusResponse>(
    `/projects/${projectId}/status/`,
  );
  return data;
}
