"use client";

import { create } from "zustand";
import type { ProjectOptions } from "@/types";

export type WizardStep = "upload" | "story" | "options" | "review";

interface CreateWizardState {
  step: WizardStep;
  photos: File[];
  storyPrompt: string;
  options: ProjectOptions;
  projectId: string | null;
  setStep: (step: WizardStep) => void;
  setPhotos: (photos: File[]) => void;
  setStoryPrompt: (prompt: string) => void;
  setOptions: (options: Partial<ProjectOptions>) => void;
  setProjectId: (id: string | null) => void;
  reset: () => void;
}

const defaultOptions: ProjectOptions = {
  ageGroup: "3-5",
  videoLength: 10,
  style: "3d_cute",
  voice: "calm_bedtime",
  language: "ko",
};

export const useCreateWizardStore = create<CreateWizardState>((set) => ({
  step: "upload",
  photos: [],
  storyPrompt: "",
  options: defaultOptions,
  projectId: null,
  setStep: (step) => set({ step }),
  setPhotos: (photos) => set({ photos }),
  setStoryPrompt: (storyPrompt) => set({ storyPrompt }),
  setOptions: (options) =>
    set((state) => ({ options: { ...state.options, ...options } })),
  setProjectId: (projectId) => set({ projectId }),
  reset: () =>
    set({
      step: "upload",
      photos: [],
      storyPrompt: "",
      options: defaultOptions,
      projectId: null,
    }),
}));
