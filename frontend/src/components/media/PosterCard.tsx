import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { Bookmark } from "lucide-react";
import { Poster } from "@/components/media/Poster";
import { TypeChip } from "@/components/media/TypeChip";
import { cn } from "@/lib/cn";
import { formatCount, formatScore, formatYear } from "@/lib/media";

type PosterCardProps = {
  to?: string;
  onClick?: () => void;
  title: string;
  year?: number | null;
  mediaTypeName: string;
  posterUrl?: string | null;
  rating?: number | null;
  ratingCount?: number | null;
  inLibrary?: boolean | null;
  loading?: boolean;
  footer?: ReactNode;
};

export function PosterCard({
  to,
  onClick,
  title,
  year,
  mediaTypeName,
  posterUrl,
  rating,
  ratingCount,
  inLibrary,
  loading,
  footer,
}: PosterCardProps) {
  const body = (
    <>
      <div className="relative aspect-[2/3] overflow-hidden rounded-xl bg-zinc-900 ring-1 ring-white/10">
        <Poster posterUrl={posterUrl} title={title} mediaTypeName={mediaTypeName} />
        {inLibrary === true ? (
          <span className="absolute top-2 left-2 inline-flex items-center gap-1 rounded-full bg-zinc-950/80 px-2 py-0.5 text-[10px] font-medium text-zinc-100 ring-1 ring-white/20">
            <Bookmark className="size-3" aria-hidden />
            On shelf
          </span>
        ) : null}
        {loading ? (
          <div className="absolute inset-0 flex items-center justify-center bg-zinc-950/70 text-sm text-zinc-100">
            Importing…
          </div>
        ) : null}
      </div>
      <div className="mt-2 space-y-1">
        <p className="line-clamp-2 text-sm font-medium text-zinc-50">{title}</p>
        <div className="flex flex-wrap items-center gap-1.5 text-xs text-zinc-400">
          {year ? <span>{formatYear(year)}</span> : null}
          <TypeChip name={mediaTypeName} />
        </div>
        <p className="text-xs text-zinc-300">
          {formatScore(rating)}
          {ratingCount != null ? (
            <span className="ml-1 text-zinc-500">{formatCount(ratingCount)}</span>
          ) : null}
        </p>
        {footer}
      </div>
    </>
  );

  const className = cn(
    "group text-left transition hover:-translate-y-0.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40",
    loading && "pointer-events-none",
  );

  if (to) {
    return (
      <Link to={to} className={className}>
        {body}
      </Link>
    );
  }

  return (
    <button type="button" onClick={onClick} className={cn(className, "w-full")} disabled={loading}>
      {body}
    </button>
  );
}
