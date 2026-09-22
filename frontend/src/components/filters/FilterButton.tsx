import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

type FilterButtonProps = {
  selected: boolean;
  onClick: () => void;
  children: ReactNode;
};

export function FilterButton({ selected, onClick, children }: FilterButtonProps) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      onClick={onClick}
      className={cn(
        "rounded-full px-3 py-1.5 text-left text-sm transition",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40",
        selected
          ? "bg-zinc-100 font-medium text-zinc-950"
          : "bg-white/6 text-zinc-300 hover:bg-white/10 hover:text-zinc-50",
      )}
    >
      {children}
    </button>
  );
}

type FilterGroupProps = {
  legend: string;
  children: ReactNode;
};

export function FilterGroup({ legend, children }: FilterGroupProps) {
  return (
    <fieldset className="space-y-2">
      <legend className="text-xs font-semibold tracking-wider text-zinc-500 uppercase">
        {legend}
      </legend>
      <div className="flex flex-wrap gap-1.5">{children}</div>
    </fieldset>
  );
}
