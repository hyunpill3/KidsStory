import { cn } from "@/lib/utils";
import type { WizardStep } from "@/hooks/use-create-wizard-store";

const STEPS: { id: WizardStep; label: string }[] = [
  { id: "upload", label: "Upload Photos" },
  { id: "story", label: "Write Story" },
  { id: "options", label: "Choose Options" },
  { id: "review", label: "Review" },
];

export function StepIndicator({ current }: { current: WizardStep }) {
  const currentIndex = STEPS.findIndex((step) => step.id === current);

  return (
    <ol className="flex items-center justify-between mb-8">
      {STEPS.map((step, index) => (
        <li key={step.id} className="flex-1 flex items-center">
          <div className="flex flex-col items-center gap-1 flex-1">
            <div
              className={cn(
                "h-8 w-8 rounded-full flex items-center justify-center text-sm font-semibold border",
                index <= currentIndex
                  ? "bg-primary text-primary-foreground border-primary"
                  : "text-muted-foreground",
              )}
            >
              {index + 1}
            </div>
            <span
              className={cn(
                "text-xs",
                index <= currentIndex ? "text-foreground" : "text-muted-foreground",
              )}
            >
              {step.label}
            </span>
          </div>
          {index < STEPS.length - 1 && (
            <div
              className={cn(
                "h-px flex-1 mx-1 mb-5",
                index < currentIndex ? "bg-primary" : "bg-border",
              )}
            />
          )}
        </li>
      ))}
    </ol>
  );
}
