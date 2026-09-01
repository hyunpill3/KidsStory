"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import {
  createProject,
  generateVideo,
  getProjectStatus,
  uploadProjectImages,
} from "@/lib/projects-api";
import type { CreateProjectPayload } from "@/types";

const PROJECT_STATUS_POLL_INTERVAL_MS = 3000;

export function useCreateProject() {
  return useMutation({
    mutationFn: (payload: CreateProjectPayload) => createProject(payload),
  });
}

export function useUploadImages() {
  return useMutation({
    mutationFn: ({ projectId, files }: { projectId: string; files: File[] }) =>
      uploadProjectImages(projectId, files),
  });
}

export function useGenerateVideo() {
  return useMutation({
    mutationFn: (projectId: string) => generateVideo(projectId),
  });
}

export function useProjectStatus(projectId: string | undefined, enabled: boolean) {
  return useQuery({
    queryKey: ["projects", projectId, "status"],
    queryFn: () => getProjectStatus(projectId as string),
    enabled: Boolean(projectId) && enabled,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === "completed" || status === "failed") {
        return false;
      }
      return PROJECT_STATUS_POLL_INTERVAL_MS;
    },
  });
}
