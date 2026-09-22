import { useEffect, useState } from "react";
import { subscribeToasts } from "@/lib/toast";

type ToastItem = { id: number; message: string };

export function Toaster() {
  const [items, setItems] = useState<ToastItem[]>([]);

  useEffect(() => {
    return subscribeToasts((message) => {
      const id = Date.now() + Math.random();
      setItems((current) => [...current, { id, message }]);
      window.setTimeout(() => {
        setItems((current) => current.filter((item) => item.id !== id));
      }, 4500);
    });
  }, []);

  if (items.length === 0) {
    return null;
  }

  return (
    <div
      className="pointer-events-none fixed bottom-6 right-6 z-60 flex w-[min(24rem,calc(100vw-2rem))] flex-col gap-2"
      role="status"
      aria-live="polite"
    >
      {items.map((item) => (
        <div
          key={item.id}
          className="pointer-events-auto rounded-xl border border-white/10 bg-zinc-900/95 px-4 py-3 text-sm text-zinc-100 shadow-lg"
          style={{ animation: "toast-in 180ms ease-out" }}
        >
          {item.message}
        </div>
      ))}
    </div>
  );
}
