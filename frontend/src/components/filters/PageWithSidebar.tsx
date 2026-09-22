import type { ReactNode } from "react";

export function FilterSidebar({ children }: { children: ReactNode }) {
  return (
    <aside className="space-y-6 rounded-2xl border border-white/8 bg-white/3 p-4 lg:sticky lg:top-24 lg:self-start">
      {children}
    </aside>
  );
}

export function PageWithSidebar({
  sidebar,
  children,
}: {
  sidebar: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className="grid grid-cols-1 gap-8 lg:grid-cols-[15rem_minmax(0,1fr)]">
      {sidebar}
      <div className="min-w-0">{children}</div>
    </div>
  );
}
