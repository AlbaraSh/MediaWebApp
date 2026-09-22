import type { ReactNode } from "react";
import { TopBar } from "@/components/layout/TopBar";

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen">
      <p className="narrow-note bg-amber-400/15 px-4 py-2 text-center text-sm text-amber-200">
        This app is designed for a larger window. Please widen your browser.
      </p>
      <TopBar />
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6">{children}</main>
    </div>
  );
}
