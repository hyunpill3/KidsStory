import type { AgeGroup, Language, VisualStyle, VoiceType } from "@/types";

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8000/api/v1";

// No signup, no paid tiers for the MVP: one free video, capped to keep
// AI generation cost bounded. Matches the backend's free-tier settings.
export const FREE_TIER = {
  maxPhotos: 1,
  videoLengthSeconds: 10,
  dailyLimit: 1,
  expiresAfterHours: 24,
  hasWatermark: true,
};

export const AGE_GROUPS: { value: AgeGroup; label: string }[] = [
  { value: "3-5", label: "3–5 years" },
  { value: "6-8", label: "6–8 years" },
  { value: "9-12", label: "9–12 years" },
];

export const VISUAL_STYLES: { value: VisualStyle; label: string; description: string }[] = [
  { value: "3d_cute", label: "Cute 3D Animation", description: "Soft and cuddly 3D character style" },
  { value: "storybook", label: "Storybook", description: "Warm hand-drawn storybook style" },
  { value: "watercolor", label: "Watercolor", description: "Gentle watercolor illustration style" },
  { value: "cartoon", label: "Cartoon", description: "Playful cartoon style" },
];

export const VOICE_TYPES: { value: VoiceType; label: string }[] = [
  { value: "male", label: "Male" },
  { value: "female", label: "Female" },
  { value: "calm_bedtime", label: "Calm bedtime voice" },
];

export const LANGUAGES: { value: Language; label: string }[] = [
  { value: "ko", label: "Korean" },
  { value: "en", label: "English" },
];

export const STATUS_LABELS: Record<string, string> = {
  draft: "Draft",
  queued: "Queued",
  analyzing_photos: "Analyzing photos",
  generating_story: "Generating story",
  generating_scenes: "Generating scenes",
  generating_narration: "Generating narration",
  composing_audio: "Adding audio",
  rendering_video: "Rendering video",
  completed: "Completed",
  failed: "Failed",
};

