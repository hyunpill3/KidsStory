"use client";

import { Download, Share2 } from "lucide-react";
import { Button } from "@/components/ui/button";

interface VideoPlayerProps {
  videoUrl: string;
  title?: string;
}

export function VideoPlayer({ videoUrl, title }: VideoPlayerProps) {
  return (
    <div className="space-y-4">
      <div className="aspect-video rounded-lg overflow-hidden bg-black">
        <video src={videoUrl} controls className="h-full w-full" />
      </div>
      <div className="flex items-center justify-between">
        {title && <h3 className="font-medium">{title}</h3>}
        <div className="flex gap-2 ml-auto">
          <Button variant="outline" size="sm" render={<a href={videoUrl} download />}>
            <Download className="h-4 w-4 mr-1" /> Download
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              if (navigator.share) {
                navigator.share({ url: videoUrl, title });
              }
            }}
          >
            <Share2 className="h-4 w-4 mr-1" /> Share
          </Button>
        </div>
      </div>
    </div>
  );
}
