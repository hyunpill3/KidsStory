export type Plan = "free" | "basic" | "premium";

export type AgeGroup = "3-5" | "6-8" | "9-12";

export type VideoLength = 10;

export type VisualStyle = "3d_cute" | "storybook" | "watercolor" | "cartoon";

export type VoiceType = "male" | "female" | "calm_bedtime";

export type Language = "ko" | "en";

export type ProjectStatus =
  | "draft"
  | "queued"
  | "analyzing_photos"
  | "generating_story"
  | "generating_scenes"
  | "generating_narration"
  | "composing_audio"
  | "rendering_video"
  | "completed"
  | "failed";

export interface ProjectOptions {
  ageGroup: AgeGroup;
  videoLength: VideoLength;
  style: VisualStyle;
  voice: VoiceType;
  language: Language;
}

export interface ProjectImage {
  id: string;
  url: string;
  thumbnailUrl: string;
  order: number;
}

export interface ProjectScene {
  id: string;
  order: number;
  narration: string;
  visualDescription: string | null;
  imageUrl: string | null;
  videoUrl: string | null;
  narrationAudioUrl: string | null;
  mixedAudioUrl: string | null;
}

export interface Project {
  id: string;
  title: string;
  storyPrompt: string | null;
  generatedStory: string | null;
  plan: Plan;
  options: ProjectOptions;
  status: ProjectStatus;
  progress: number;
  images: ProjectImage[];
  scenes: ProjectScene[];
  videoUrl: string | null;
  expiresAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectPayload {
  storyPrompt: string;
  options: ProjectOptions;
  captchaToken?: string | null;
}

export interface ProjectStatusResponse {
  status: ProjectStatus;
  progress: number;
  currentStep: string;
  videoUrl: string | null;
  expiresAt: string | null;
  errorMessage: string | null;
}
