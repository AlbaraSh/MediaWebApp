import { useQuery } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router-dom";
import { FilterButton, FilterGroup } from "@/components/filters/FilterButton";
import { FilterSidebar, PageWithSidebar } from "@/components/filters/PageWithSidebar";
import { GenreFilter } from "@/components/filters/GenreFilter";
import { TypeFilter } from "@/components/filters/TypeFilter";
import { Pagination } from "@/components/media/Pagination";
import { Poster } from "@/components/media/Poster";
import { TypeChip } from "@/components/media/TypeChip";
import { Input } from "@/components/ui/Input";
import { Toggle } from "@/components/ui/Toggle";
import { PosterGridSkeleton } from "@/components/ui/Skeleton";
import { fetchLibrary, fetchShelfGenres } from "@/lib/api/userMedia";
import type { CatalogType, LibrarySort, SortDirection, UserMediaStatus } from "@/lib/api/types";
import { USER_MEDIA_STATUSES } from "@/lib/api/types";
import { queryKeys } from "@/lib/queryKeys";
import { statusLabel, watchingLabel } from "@/lib/media";
import { cn } from "@/lib/cn";
import {
  optionalText,
  parsePage,
  parseType,
  resetPage,
  setOrDelete,
} from "@/lib/url";

function parseStatus(raw: string | null): UserMediaStatus {
  if (raw && USER_MEDIA_STATUSES.includes(raw as UserMediaStatus)) {
    return raw as UserMediaStatus;
  }
  return "COMPLETED";
}

function parseBound(raw: string | null): number | undefined {
  if (!raw) {
    return undefined;
  }
  const value = Number.parseInt(raw, 10);
  return Number.isFinite(value) && value >= 1 && value <= 10 ? value : undefined;
}

function parseLibrarySort(raw: string | null): LibrarySort | undefined {
  return raw === "RATING" ? "RATING" : undefined;
}

function parseLibraryDirection(raw: string | null): SortDirection {
  return raw === "ASC" ? "ASC" : "DESC";
}

function emptyCopy(status: UserMediaStatus): string {
  switch (status) {
    case "PLANNED":
      return "Nothing planned yet. Add titles from Discover or Search.";
    case "WATCHING":
      return "You are not in the middle of anything here.";
    case "COMPLETED":
      return "You haven't completed anything yet. Finish a title and rate it 7 or higher to improve For You.";
    case "DROPPED":
      return "You haven't dropped anything.";
  }
}

export function LibraryPage() {
  const [params, setParams] = useSearchParams();
  const status = parseStatus(params.get("status"));
  const type = parseType(params.get("type"));
  const genre = optionalText(params.get("genre"));
  const minRating = parseBound(params.get("minRating"));
  const maxRating = parseBound(params.get("maxRating"));
  const q = optionalText(params.get("q"));
  const sort = parseLibrarySort(params.get("sort"));
  const direction = parseLibraryDirection(params.get("direction"));
  const page = parsePage(params.get("page"));
  const exactScoreOn = minRating != null && maxRating != null && minRating === maxRating;
  const exactScore = exactScoreOn ? minRating : 5;

  const query = {
    status,
    type,
    genre,
    minRating: exactScoreOn ? minRating : undefined,
    maxRating: exactScoreOn ? maxRating : undefined,
    q,
    sort,
    direction: sort ? direction : undefined,
    page,
    size: 20 as const,
  };

  const genres = useQuery({
    queryKey: queryKeys.userMedia.genres,
    queryFn: fetchShelfGenres,
  });

  const library = useQuery({
    queryKey: queryKeys.userMedia.list(query),
    queryFn: () => fetchLibrary(query),
  });

  function update(mutate: (next: URLSearchParams) => void) {
    const next = new URLSearchParams(params);
    mutate(next);
    setParams(next, { replace: true });
  }

  const counts = library.data?.counts;
  const typeFilter: CatalogType | "ALL" = type ?? "ALL";
  const watchingTabLabel = watchingLabel({ type: typeFilter });
  const orderCopy =
    sort === "RATING"
      ? direction === "ASC"
        ? "Lowest score first."
        : "Highest score first."
      : "Most recently added first.";

  return (
    <PageWithSidebar
      sidebar={
        <FilterSidebar>
          <TypeFilter
            selected={type}
            onSelect={(value) =>
              update((next) => {
                setOrDelete(next, "type", value);
                resetPage(next);
              })
            }
          />
          <FilterGroup legend="Score">
            <FilterButton
              selected={sort === "RATING" && direction === "ASC"}
              onClick={() =>
                update((next) => {
                  if (sort === "RATING" && direction === "ASC") {
                    next.delete("sort");
                    next.delete("direction");
                  } else {
                    next.set("sort", "RATING");
                    next.set("direction", "ASC");
                  }
                  resetPage(next);
                })
              }
            >
              Ascending score
            </FilterButton>
            <FilterButton
              selected={sort === "RATING" && direction === "DESC"}
              onClick={() =>
                update((next) => {
                  if (sort === "RATING" && direction === "DESC") {
                    next.delete("sort");
                    next.delete("direction");
                  } else {
                    next.set("sort", "RATING");
                    next.set("direction", "DESC");
                  }
                  resetPage(next);
                })
              }
            >
              Descending score
            </FilterButton>
            <div className="flex w-full items-center justify-between gap-3 pt-1">
              <span className="text-sm text-zinc-300">Exact score</span>
              <Toggle
                label="Filter by exact score"
                checked={exactScoreOn}
                onChange={(checked) =>
                  update((next) => {
                    if (checked) {
                      next.set("minRating", String(exactScore));
                      next.set("maxRating", String(exactScore));
                    } else {
                      next.delete("minRating");
                      next.delete("maxRating");
                    }
                    resetPage(next);
                  })
                }
              />
            </div>
            {exactScoreOn ? (
              <div className="flex w-full items-center gap-3">
                <input
                  type="range"
                  min={1}
                  max={10}
                  step={1}
                  aria-label="Exact score"
                  value={exactScore}
                  onChange={(event) =>
                    update((next) => {
                      next.set("minRating", event.target.value);
                      next.set("maxRating", event.target.value);
                      resetPage(next);
                    })
                  }
                  className="h-2 w-full cursor-pointer appearance-none rounded-full bg-white/15 accent-zinc-100"
                />
                <span className="w-6 text-right text-sm tabular-nums text-zinc-100">
                  {exactScore}
                </span>
              </div>
            ) : null}
          </FilterGroup>
          <GenreFilter
            genres={genres.data ?? []}
            selected={genre}
            showSearch={false}
            onSelect={(value) =>
              update((next) => {
                setOrDelete(next, "genre", value);
                resetPage(next);
              })
            }
          />
        </FilterSidebar>
      }
    >
      <div className="mb-6">
        <h1 className="font-display text-3xl text-zinc-50 italic">Shelf</h1>
        <p className="mt-1 text-sm text-zinc-400">{orderCopy}</p>
      </div>

      <div className="mb-6 flex flex-wrap items-center gap-3">
        <div className="flex min-w-0 flex-1 flex-wrap gap-2" role="tablist" aria-label="Shelf status">
        {USER_MEDIA_STATUSES.map((value) => {
          const label =
            value === "WATCHING" ? watchingTabLabel : statusLabel(value);
          const badge =
            value === "PLANNED"
              ? counts?.planned
              : value === "WATCHING"
                ? counts?.watching
                : value === "COMPLETED"
                  ? counts?.completed
                  : counts?.dropped;
          const selected = status === value;
          return (
            <button
              key={value}
              type="button"
              role="tab"
              aria-selected={selected}
              onClick={() =>
                update((next) => {
                  next.set("status", value);
                  resetPage(next);
                })
              }
              className={cn(
                "rounded-full px-4 py-2 text-sm transition",
                "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40",
                selected
                  ? "bg-zinc-100 font-medium text-zinc-950"
                  : "bg-white/6 text-zinc-300 hover:bg-white/10",
              )}
            >
              {label}
              {badge != null ? (
                <span className="ml-2 text-xs opacity-70">{badge}</span>
              ) : null}
            </button>
          );
        })}
        </div>
        <Input
          id="library-q"
          value={params.get("q") ?? ""}
          placeholder="Search your shelf"
          aria-label="Search your shelf"
          className="h-10 w-full max-w-xs"
          onChange={(event) =>
            update((next) => {
              setOrDelete(next, "q", event.target.value.trim() || undefined);
              resetPage(next);
            })
          }
        />
      </div>

      {library.isLoading ? (
        <PosterGridSkeleton />
      ) : library.data && library.data.content.length === 0 ? (
        <div className="rounded-2xl border border-white/8 bg-white/3 px-6 py-12 text-center text-zinc-300">
          {emptyCopy(status)}
        </div>
      ) : (
        <>
          <ul className="space-y-3">
            {library.data?.content.map((item) => (
              <li key={item.mediaId}>
                <Link
                  to={`/media/${item.mediaId}`}
                  className="flex gap-4 rounded-2xl border border-white/8 bg-white/3 p-3 transition hover:bg-white/6 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40"
                >
                  <div className="h-28 w-20 shrink-0 overflow-hidden rounded-lg">
                    <Poster
                      posterUrl={item.posterUrl}
                      title={item.title}
                      mediaTypeName={item.mediaType.name}
                    />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="font-medium text-zinc-50">{item.title}</p>
                    <p className="mt-1 flex flex-wrap items-center gap-2 text-xs text-zinc-400">
                      <span>{item.releaseYear ?? ""}</span>
                      <TypeChip name={item.mediaType.name} />
                      <span>
                        {statusLabel(item.status, item.mediaType.name, typeFilter)}
                      </span>
                      <span>{item.rating == null ? "Unrated" : `Your score ${item.rating}`}</span>
                    </p>
                    {item.review ? (
                      <p className="mt-2 line-clamp-2 text-sm text-zinc-400">{item.review}</p>
                    ) : null}
                  </div>
                </Link>
              </li>
            ))}
          </ul>
          <Pagination
            page={page}
            totalPages={library.data?.totalPages ?? 0}
            onPageChange={(nextPage) =>
              update((next) => {
                if (nextPage <= 0) {
                  next.delete("page");
                } else {
                  next.set("page", String(nextPage));
                }
              })
            }
          />
        </>
      )}
    </PageWithSidebar>
  );
}
