import { Link } from "react-router-dom";
import { Button } from "@/components/ui/Button";

export function NotFoundPage() {
  return (
    <div className="mx-auto max-w-lg space-y-4 pt-16 text-center">
      <p className="text-xs font-semibold tracking-[0.25em] text-zinc-500 uppercase">404</p>
      <h1 className="font-display text-4xl text-zinc-50 italic">Not found</h1>
      <p className="text-zinc-400">That page or title is not in the catalog.</p>
      <Link to="/discover">
        <Button>Back to Discover</Button>
      </Link>
    </div>
  );
}
