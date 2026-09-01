"use client";

import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { AGE_GROUPS, FREE_TIER, LANGUAGES, VISUAL_STYLES, VOICE_TYPES } from "@/lib/constants";
import type { ProjectOptions } from "@/types";

interface OptionsFormProps {
  options: ProjectOptions;
  onChange: (options: Partial<ProjectOptions>) => void;
}

export function OptionsForm({ options, onChange }: OptionsFormProps) {
  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-xl font-semibold">Choose Options</h2>
        <p className="text-muted-foreground text-sm mt-1">
          Pick a style and length that fits your child best.
        </p>
      </div>

      <div className="grid gap-6 sm:grid-cols-2">
        <div className="space-y-2">
          <Label>Child Age</Label>
          <Select
            value={options.ageGroup}
            onValueChange={(value) => onChange({ ageGroup: value as ProjectOptions["ageGroup"] })}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {AGE_GROUPS.map((item) => (
                <SelectItem key={item.value} value={item.value}>
                  {item.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-2">
          <Label>Video Length</Label>
          <p className="flex h-9 items-center rounded-md border bg-muted px-3 text-sm text-muted-foreground">
            {FREE_TIER.videoLengthSeconds} seconds (free plan)
          </p>
        </div>

        <div className="space-y-2">
          <Label>Voice</Label>
          <Select
            value={options.voice}
            onValueChange={(value) => onChange({ voice: value as ProjectOptions["voice"] })}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {VOICE_TYPES.map((item) => (
                <SelectItem key={item.value} value={item.value}>
                  {item.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-2">
          <Label>Language</Label>
          <Select
            value={options.language}
            onValueChange={(value) => onChange({ language: value as ProjectOptions["language"] })}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {LANGUAGES.map((item) => (
                <SelectItem key={item.value} value={item.value}>
                  {item.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="space-y-3">
        <Label>Style</Label>
        <RadioGroup
          value={options.style}
          onValueChange={(value) => onChange({ style: value as ProjectOptions["style"] })}
          className="grid gap-3 sm:grid-cols-2"
        >
          {VISUAL_STYLES.map((item) => (
            <Label
              key={item.value}
              htmlFor={item.value}
              className="flex items-start gap-3 rounded-md border p-3 cursor-pointer has-[[data-state=checked]]:border-primary"
            >
              <RadioGroupItem value={item.value} id={item.value} className="mt-1" />
              <span>
                <span className="block font-medium">{item.label}</span>
                <span className="block text-sm text-muted-foreground">
                  {item.description}
                </span>
              </span>
            </Label>
          ))}
        </RadioGroup>
      </div>
    </div>
  );
}
