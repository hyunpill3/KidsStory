"use client";

import { useRouter } from "next/navigation";
import { ArrowRight, ImagePlus, Sparkles, Wand2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { FREE_TIER } from "@/lib/constants";

const STEPS = [
  {
    icon: ImagePlus,
    title: "1. Upload a Photo",
    description: "Upload one photo of your child, family, pet, drawing, or toy.",
  },
  {
    icon: Sparkles,
    title: "2. Write a Short Story",
    description: "A couple of sentences are enough for the AI to expand into a full story.",
  },
  {
    icon: Wand2,
    title: "3. Get Your Video",
    description: `The AI creates a ${FREE_TIER.videoLengthSeconds}-second animated story video, free.`,
  },
];

export default function Home() {
  const router = useRouter();

  return (
    <main className="flex-1">
      <section className="container mx-auto px-4 py-24 text-center">
        <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight">
          Start with one photo
          <br />
          <span className="text-primary">and create a personalized story video for your child</span>
        </h1>
        <p className="mt-6 text-lg text-muted-foreground max-w-2xl mx-auto">
          No sign-up needed. Upload a photo and add a story prompt, and the AI will create a free{" "}
          {FREE_TIER.videoLengthSeconds}-second animated story video for you.
        </p>
        <div className="mt-8 flex justify-center gap-4">
          <Button size="lg" onClick={() => router.push("/create")}>
            Create Now <ArrowRight className="ml-1 h-4 w-4" />
          </Button>
        </div>
      </section>

      <section className="container mx-auto px-4 pb-24 grid gap-6 md:grid-cols-3">
        {STEPS.map((step) => (
          <Card key={step.title}>
            <CardHeader>
              <step.icon className="h-8 w-8 text-primary" />
              <CardTitle className="mt-2">{step.title}</CardTitle>
            </CardHeader>
            <CardContent className="text-muted-foreground">
              {step.description}
            </CardContent>
          </Card>
        ))}
      </section>
    </main>
  );
}

