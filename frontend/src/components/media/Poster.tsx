import { useState } from "react";
import { cn } from "@/lib/cn";
import { accentFor, titleInitials } from "@/lib/media";

type PosterProps = {
  posterUrl: string | null | undefined;
  title: string;
  mediaTypeName: string;
  className?: string;
};

export function Poster({ posterUrl, title, mediaTypeName, className }: PosterProps) {
  const [broken, setBroken] = useState(false);
  const showImage = Boolean(posterUrl) && !broken;
  const accent = accentFor(mediaTypeName);

  if (showImage) {
    return (
      <img
        src={posterUrl ?? ""}
        alt={title}
        className={cn("h-full w-full object-cover", className)}
        onError={() => setBroken(true)}
      />
    );
  }

  return (
    <div
      className={cn(
        "flex h-full w-full items-center justify-center bg-gradient-to-br",
        accent.placeholder,
        className,
      )}
      aria-hidden="true"
    >
      <span className="px-2 text-center text-lg font-semibold tracking-wide text-white/90">
        {titleInitials(title)}
      </span>
    </div>
  );
}
