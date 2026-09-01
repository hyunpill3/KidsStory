"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { PhotoUpload } from "@/components/upload/photo-upload";
import { StoryInput } from "@/components/story/story-input";
import { OptionsForm } from "@/components/options/options-form";
import { GenerationProgress } from "@/components/progress/generation-progress";
import { VideoPlayer } from "@/components/video/video-player";
import { StepIndicator } from "@/components/create/step-indicator";
import { AdSlot } from "@/components/ads/ad-slot";
import { useCreateWizardStore } from "@/hooks/use-create-wizard-store";
import {
  useCreateProject,
  useGenerateVideo,
  useProjectStatus,
  useUploadImages,
} from "@/hooks/use-projects";

function formatExpiresIn(expiresAt: string | null): string | null {
  if (!expiresAt) return null;
  const hours = Math.max(1, Math.round((new Date(expiresAt).getTime() - Date.now()) / 3_600_000));
  return `This video will be available for about ${hours} more hour${hours === 1 ? "" : "s"}, then it's automatically deleted.`;
}

export function CreateWizard() {
  const {
    step,
    photos,
    storyPrompt,
    options,
    projectId,
    setStep,
    setPhotos,
    setStoryPrompt,
    setOptions,
    setProjectId,
    reset,
  } = useCreateWizardStore();

  const [isSubmitting, setIsSubmitting] = useState(false);

  const createProject = useCreateProject();
  const uploadImages = useUploadImages();
  const generateVideo = useGenerateVideo();
  const { data: statusData } = useProjectStatus(
    projectId ?? undefined,
    step === "review",
  );

  const handleSubmit = async () => {
    if (photos.length === 0) {
      toast.error("Please upload a photo.");
      setStep("upload");
      return;
    }

    setIsSubmitting(true);
    try {
      const project = await createProject.mutateAsync({ storyPrompt, options });
      setProjectId(project.id);
      await uploadImages.mutateAsync({ projectId: project.id, files: photos });
      await generateVideo.mutateAsync(project.id);
      setStep("review");
    } catch (error) {
      const message = error instanceof Error ? error.message : "An error occurred while processing your request.";
      toast.error(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="container mx-auto max-w-2xl px-4 py-12">
      <StepIndicator current={step} />

      {step === "upload" && <PhotoUpload photos={photos} onChange={setPhotos} />}
      {step === "story" && <StoryInput value={storyPrompt} onChange={setStoryPrompt} />}
      {step === "options" && <OptionsForm options={options} onChange={setOptions} />}
      {step === "review" && statusData && (
        <>
          <GenerationProgress
            status={statusData.status}
            progress={statusData.progress}
            errorMessage={statusData.errorMessage}
          />
          {statusData.status === "completed" && statusData.videoUrl && (
            <div className="mt-8 space-y-4">
              <VideoPlayer videoUrl={statusData.videoUrl} />
              {formatExpiresIn(statusData.expiresAt) && (
                <p className="text-center text-sm text-muted-foreground">
                  {formatExpiresIn(statusData.expiresAt)}
                </p>
              )}
              <AdSlot />
              <Button className="w-full" onClick={reset}>
                Create Another Video
              </Button>
            </div>
          )}
        </>
      )}

      {step !== "review" && (
        <div className="mt-8 flex justify-between">
          <Button
            variant="outline"
            disabled={step === "upload"}
            onClick={() => {
              if (step === "story") setStep("upload");
              if (step === "options") setStep("story");
            }}
          >
            Previous
          </Button>

          {step === "options" ? (
            <Button onClick={handleSubmit} disabled={isSubmitting}>
              {isSubmitting ? "Generating..." : "Create My Story"}
            </Button>
          ) : (
            <Button
              onClick={() => {
                if (step === "upload") {
                  if (photos.length === 0) {
                    toast.error("Please upload a photo.");
                    return;
                  }
                  setStep("story");
                } else if (step === "story") {
                  setStep("options");
                }
              }}
            >
              Next
            </Button>
          )}
        </div>
      )}

      <AdSlot className="mt-10" />
    </div>
  );
}
