"use client";

import { useRef } from "react";
import { ImagePlus, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { AdSlot } from "@/components/ads/ad-slot";
import { FREE_TIER } from "@/lib/constants";

const MAX_PHOTOS = FREE_TIER.maxPhotos;

interface PhotoUploadProps {
  photos: File[];
  onChange: (photos: File[]) => void;
}

export function PhotoUpload({ photos, onChange }: PhotoUploadProps) {
  const inputRef = useRef<HTMLInputElement>(null);

  const handleFiles = (fileList: FileList | null) => {
    if (!fileList) return;
    const newFiles = Array.from(fileList).slice(0, MAX_PHOTOS - photos.length);
    onChange([...photos, ...newFiles].slice(0, MAX_PHOTOS));
  };

  const removePhoto = (index: number) => {
    onChange(photos.filter((_, i) => i !== index));
  };

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-xl font-semibold">Upload a Photo</h2>
        <p className="text-muted-foreground text-sm mt-1">
          Upload one photo of your child, family, pet, drawing, or toy. Free videos are{" "}
          {FREE_TIER.videoLengthSeconds} seconds, include a small watermark, and are
          automatically deleted after {FREE_TIER.expiresAfterHours} hours.
        </p>
      </div>

      <button
        type="button"
        onClick={() => inputRef.current?.click()}
        disabled={photos.length >= MAX_PHOTOS}
        className="w-full border-2 border-dashed rounded-lg p-10 flex flex-col items-center gap-2 text-muted-foreground hover:border-primary hover:text-primary transition-colors disabled:opacity-50 disabled:pointer-events-none"
      >
        <ImagePlus className="h-10 w-10" />
        <span>Click to upload or drag and drop a file</span>
        <input
          ref={inputRef}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={(e) => handleFiles(e.target.files)}
        />
      </button>

      {photos.length > 0 && (
        <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-3">
          {photos.map((file, index) => (
            <div key={`${file.name}-${index}`} className="relative aspect-square rounded-md overflow-hidden border group">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={URL.createObjectURL(file)}
                alt={`Uploaded photo ${index + 1}`}
                className="h-full w-full object-cover"
              />
              <Button
                type="button"
                size="icon"
                variant="destructive"
                className="absolute top-1 right-1 h-6 w-6 opacity-0 group-hover:opacity-100 transition-opacity"
                onClick={() => removePhoto(index)}
              >
                <X className="h-3 w-3" />
              </Button>
            </div>
          ))}
        </div>
      )}

      <AdSlot className="mt-6" />
    </div>
  );
}
