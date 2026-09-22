type Listener = (message: string) => void;

const listeners = new Set<Listener>();

export function toast(message: string): void {
  const text = message.trim();
  if (!text) {
    return;
  }
  for (const listener of listeners) {
    listener(text);
  }
}

export function subscribeToasts(listener: Listener): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}
