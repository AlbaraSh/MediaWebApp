import type { ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

type Variant = "primary" | "secondary" | "ghost" | "danger" | "outline";
type Size = "sm" | "md" | "lg";

const variantClass: Record<Variant, string> = {
  primary:
    "bg-zinc-100 text-zinc-950 hover:bg-white focus-visible:ring-zinc-100",
  secondary:
    "bg-white/8 text-zinc-100 hover:bg-white/12 focus-visible:ring-white/40",
  ghost:
    "bg-transparent text-zinc-300 hover:bg-white/8 hover:text-zinc-50 focus-visible:ring-white/30",
  danger:
    "bg-red-500/15 text-red-300 hover:bg-red-500/25 focus-visible:ring-red-400",
  outline:
    "border border-white/15 bg-transparent text-zinc-100 hover:bg-white/8 focus-visible:ring-white/30",
};

const sizeClass: Record<Size, string> = {
  sm: "h-8 px-3 text-xs",
  md: "h-10 px-4 text-sm",
  lg: "h-12 px-5 text-sm",
};

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  size?: Size;
};

export function Button({
  className,
  variant = "primary",
  size = "md",
  type = "button",
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-full font-medium transition",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-zinc-950",
        "disabled:pointer-events-none disabled:opacity-40",
        variantClass[variant],
        sizeClass[size],
        className,
      )}
      {...props}
    />
  );
}
