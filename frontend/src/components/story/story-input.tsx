"use client";

import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent } from "@/components/ui/card";

interface StoryInputProps {
  value: string;
  onChange: (value: string) => void;
}

const EXAMPLE_PROMPTS = [
  "A child and their puppy Coco travel through space to find a lost star",
  "A child meets a new friend in a magical forest",
];

export function StoryInput({ value, onChange }: StoryInputProps) {
  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-xl font-semibold">Write Your Story</h2>
        <p className="text-muted-foreground text-sm mt-1">
          A couple of sentences are enough for the AI to expand into a full story. If you leave it blank, the AI will create one automatically from your photos.
        </p>
      </div>

      <Textarea
        placeholder="Example: A child and their puppy Coco travel through space to find a lost star"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        rows={5}
      />

      <Card className="bg-muted/50">
        <CardContent className="pt-4 text-sm text-muted-foreground space-y-1">
          <p className="font-medium text-foreground">Try writing something like this</p>
          {EXAMPLE_PROMPTS.map((example) => (
            <p key={example}>“{example}”</p>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
