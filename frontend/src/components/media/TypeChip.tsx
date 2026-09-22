import { cn } from "@/lib/cn";
import { accentFor } from "@/lib/media";

export function TypeChip({ name, className }: { name: string; className?: string }) {
  const accent = accentFor(name);
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide",
        accent.chip,
        className,
      )}
    >
      {name}
    </span>
  );
}
