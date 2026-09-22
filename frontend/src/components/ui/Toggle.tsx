import { cn } from "@/lib/cn";

type ToggleProps = {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label: string;
};

export function Toggle({ checked, onChange, label }: ToggleProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      onClick={() => onChange(!checked)}
      className={cn(
        "relative h-7 w-12 shrink-0 rounded-full transition",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40",
        checked ? "bg-emerald-400" : "bg-zinc-600",
      )}
    >
      <span
        className={cn(
          "absolute top-0.5 size-6 rounded-full bg-white shadow transition-transform",
          checked ? "translate-x-5" : "translate-x-0.5",
        )}
      />
    </button>
  );
}
