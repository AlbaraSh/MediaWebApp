import { cn } from "@/lib/cn";

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn("animate-pulse rounded-xl bg-white/8", className)} />;
}

export function PosterGridSkeleton({ count = 10 }: { count?: number }) {
  return (
    <div className="grid grid-cols-[repeat(auto-fill,minmax(10rem,1fr))] gap-4">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="space-y-2">
          <Skeleton className="aspect-[2/3] w-full" />
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-3 w-1/2" />
        </div>
      ))}
    </div>
  );
}

export function RowSkeleton({ count = 6 }: { count?: number }) {
  return (
    <div className="flex gap-3 overflow-hidden">
      {Array.from({ length: count }, (_, index) => (
        <Skeleton key={index} className="h-48 w-32 shrink-0" />
      ))}
    </div>
  );
}
