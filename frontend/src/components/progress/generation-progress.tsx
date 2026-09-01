"use client";

import { CheckCircle2, Loader2, XCircle } from "lucide-react";
import { Progress } from "@/components/ui/progress";
import { STATUS_LABELS } from "@/lib/constants";
import type { ProjectStatus } from "@/types";

const PIPELINE_STEPS: ProjectStatus[] = [
  "analyzing_photos",
  "generating_story",
  "generating_scenes",
  "generating_narration",
  "composing_audio",
  "rendering_video",
];

interface GenerationProgressProps {
  status: ProjectStatus;
  progress: number;
  errorMessage?: string | null;
}

export function GenerationProgress({
  status,
  progress,
  errorMessage,
}: GenerationProgressProps) {
  const isFailed = status === "failed";
  const isCompleted = status === "completed";

  return (
    <div className="space-y-6">
      <div className="text-center space-y-2">
        {isFailed ? (
          <XCircle className="h-10 w-10 text-destructive mx-auto" />
        ) : isCompleted ? (
          <CheckCircle2 className="h-10 w-10 text-primary mx-auto" />
        ) : (
          <Loader2 className="h-10 w-10 text-primary mx-auto animate-spin" />
        )}
        <h2 className="text-xl font-semibold">
          {isFailed ? "Generation failed" : STATUS_LABELS[status] ?? "Generating"}
        </h2>
        {isFailed && errorMessage && (
          <p className="text-sm text-destructive">{errorMessage}</p>
        )}
      </div>

      <Progress value={progress} />

      <ol className="space-y-2">
        {PIPELINE_STEPS.map((step) => {
          const stepIndex = PIPELINE_STEPS.indexOf(step);
          const currentIndex = PIPELINE_STEPS.indexOf(status);
          const done = isCompleted || (currentIndex !== -1 && stepIndex < currentIndex);
          const active = step === status;

          return (
            <li
              key={step}
              className={`flex items-center gap-2 text-sm rounded-md p-2 ${
                active ? "bg-muted font-medium" : ""
              }`}
            >
              {done ? (
                <CheckCircle2 className="h-4 w-4 text-primary" />
              ) : active ? (
                <Loader2 className="h-4 w-4 animate-spin text-primary" />
              ) : (
                <span className="h-4 w-4 rounded-full border" />
              )}
              {STATUS_LABELS[step]}
            </li>
          );
        })}
      </ol>
    </div>
  );
}
