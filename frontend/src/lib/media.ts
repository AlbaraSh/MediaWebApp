import type { CatalogType, MediaTypeName, Provider, UserMediaStatus } from "@/lib/api/types";
import { CATALOG_TYPES } from "@/lib/api/types";

export const TYPE_LABEL: Record<CatalogType, MediaTypeName> = {
  MOVIE: "Movie",
  TV: "TV Show",
  ANIME: "Anime",
  GAME: "Game",
};

const NAME_TO_TYPE: Record<string, CatalogType> = {
  Movie: "MOVIE",
  "TV Show": "TV",
  Anime: "ANIME",
  Game: "GAME",
};

export function catalogTypeFromName(name: string | undefined | null): CatalogType | undefined {
  if (!name) {
    return undefined;
  }
  return NAME_TO_TYPE[name];
}

export function isCatalogType(value: string | null | undefined): value is CatalogType {
  return CATALOG_TYPES.includes(value as CatalogType);
}

export function isProvider(value: string | null | undefined): value is Provider {
  return value === "tmdb" || value === "jikan" || value === "rawg";
}

export type TypeAccent = {
  text: string;
  bg: string;
  border: string;
  chip: string;
  placeholder: string;
};

export const TYPE_ACCENT: Record<MediaTypeName, TypeAccent> = {
  Movie: {
    text: "text-amber-400",
    bg: "bg-amber-400/15",
    border: "border-amber-400/35",
    chip: "bg-amber-400 text-zinc-950",
    placeholder: "from-amber-500/40 to-amber-900/70",
  },
  "TV Show": {
    text: "text-sky-400",
    bg: "bg-sky-400/15",
    border: "border-sky-400/35",
    chip: "bg-sky-400 text-zinc-950",
    placeholder: "from-sky-500/40 to-sky-950/70",
  },
  Anime: {
    text: "text-fuchsia-400",
    bg: "bg-fuchsia-400/15",
    border: "border-fuchsia-400/35",
    chip: "bg-fuchsia-400 text-zinc-950",
    placeholder: "from-fuchsia-500/40 to-fuchsia-950/70",
  },
  Game: {
    text: "text-emerald-400",
    bg: "bg-emerald-400/15",
    border: "border-emerald-400/35",
    chip: "bg-emerald-400 text-zinc-950",
    placeholder: "from-emerald-400/40 to-emerald-950/70",
  },
};

export function accentFor(name: string | undefined | null): TypeAccent {
  if (name === "Movie" || name === "TV Show" || name === "Anime" || name === "Game") {
    return TYPE_ACCENT[name];
  }
  return {
    text: "text-zinc-300",
    bg: "bg-zinc-400/15",
    border: "border-zinc-400/35",
    chip: "bg-zinc-300 text-zinc-950",
    placeholder: "from-zinc-500/40 to-zinc-900/70",
  };
}

export function titleInitials(title: string): string {
  const parts = title.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return "?";
  }
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }
  return `${parts[0][0] ?? ""}${parts[1][0] ?? ""}`.toUpperCase();
}

export function watchingLabel(options: {
  type?: CatalogType | "ALL";
  mediaTypeName?: string;
}): string {
  if (options.mediaTypeName === "Game" || options.type === "GAME") {
    return "Playing";
  }
  if (options.type === "ALL" || options.type === undefined) {
    return "Watching / Playing";
  }
  return "Watching";
}

export function statusLabel(
  status: UserMediaStatus,
  mediaTypeName?: string,
  typeFilter?: CatalogType | "ALL",
): string {
  switch (status) {
    case "PLANNED":
      return "Plan";
    case "WATCHING":
      return watchingLabel({ type: typeFilter, mediaTypeName });
    case "COMPLETED":
      return "Completed";
    case "DROPPED":
      return "Dropped";
  }
}

export function formatScore(rating: number | null | undefined): string {
  if (rating === null || rating === undefined) {
    return "—";
  }
  return Number.isInteger(rating) ? String(rating) : rating.toFixed(1);
}

export function formatCount(count: number | null | undefined): string {
  if (count === null || count === undefined) {
    return "";
  }
  return count.toLocaleString();
}

export function formatYear(year: number | null | undefined): string {
  if (year === null || year === undefined) {
    return "";
  }
  return String(year);
}

export function formatInstant(value: string | null | undefined): string {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

export function defaultDiscoverDirection(sort: string): "ASC" | "DESC" {
  return sort === "TITLE" ? "ASC" : "DESC";
}

export function normalizeProvider(provider: string): string {
  return provider.toLowerCase();
}
