import { useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { LogOut, User } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { logoutAccount } from "@/lib/api/auth";
import { persistUsername } from "@/lib/storage";
import { useAuth } from "@/hooks/useAuth";
import { loginPathWithNext } from "@/lib/redirect";
import { cn } from "@/lib/cn";

const NAV = [
  { to: "/discover", label: "Discover" },
  { to: "/search", label: "Search" },
  { to: "/shelf", label: "Shelf" },
  { to: "/for-you", label: "For You" },
] as const;

export function TopBar() {
  const location = useLocation();
  const { isAuthenticated, email, username } = useAuth();

  return (
    <header className="sticky top-0 z-40 border-b border-white/8 bg-zinc-950/80 backdrop-blur-md">
      <div className="mx-auto flex max-w-7xl items-center gap-6 px-4 py-3 sm:px-6">
        <Link to="/" className="shrink-0">
          <span className="font-display text-xl italic tracking-wide text-zinc-50">
            MyShelf
          </span>
        </Link>
        <nav className="flex min-w-0 flex-1 items-center gap-1 overflow-x-auto">
          {NAV.map((item) => {
            const active = location.pathname === item.to;
            return (
              <Link
                key={item.to}
                to={item.to}
                className={cn(
                  "rounded-full px-3 py-1.5 text-sm whitespace-nowrap transition",
                  "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40",
                  active
                    ? "bg-white/10 text-zinc-50"
                    : "text-zinc-400 hover:bg-white/5 hover:text-zinc-100",
                )}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>
        <AccountMenu
          isAuthenticated={isAuthenticated}
          email={email}
          username={username}
        />
      </div>
    </header>
  );
}

function AccountMenu({
  isAuthenticated,
  email,
  username,
}: {
  isAuthenticated: boolean;
  email: string | null;
  username: string | null;
}) {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { setToken } = useAuth();
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    const onDoc = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", onDoc);
    window.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDoc);
      window.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const next = `${location.pathname}${location.search}`;

  async function onLogout() {
    setOpen(false);
    try {
      await logoutAccount();
    } catch {
      // Always clear locally.
    }
    setToken(null);
    persistUsername(null);
    navigate("/");
  }

  if (!isAuthenticated) {
    return (
      <div className="flex shrink-0 items-center gap-2">
        <Link
          to={loginPathWithNext(next)}
          className="rounded-full px-3 py-1.5 text-sm text-zinc-300 hover:text-zinc-50"
        >
          Sign in
        </Link>
        <Button size="sm" onClick={() => navigate(`/register?next=${encodeURIComponent(next)}`)}>
          Create account
        </Button>
      </div>
    );
  }

  return (
    <div ref={rootRef} className="relative shrink-0">
      <Button
        variant="secondary"
        size="sm"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        <User className="size-4" />
        <span className="max-w-40 truncate">{email ?? "Account"}</span>
      </Button>
      {open ? (
        <div
          role="menu"
          className="absolute right-0 mt-2 w-64 rounded-xl border border-white/10 bg-zinc-950 p-2 shadow-xl"
        >
          <div className="px-3 py-2">
            {username ? <p className="text-sm font-medium text-zinc-100">{username}</p> : null}
            <p className="truncate text-xs text-zinc-400">{email}</p>
          </div>
          <button
            type="button"
            role="menuitem"
            onClick={() => void onLogout()}
            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-zinc-200 hover:bg-white/8"
          >
            <LogOut className="size-4" />
            Log out
          </button>
        </div>
      ) : null}
    </div>
  );
}
