import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router-dom";
import { ArrowDownUp } from "lucide-react";
import { FilterButton, FilterGroup } from "@/components/filters/FilterButton";
import { FilterSidebar, PageWithSidebar } from "@/components/filters/PageWithSidebar";
import { GenreFilter } from "@/components/filters/GenreFilter";
import { TypeFilter } from "@/components/filters/TypeFilter";
import { Pagination } from "@/components/media/Pagination";
import { PosterCard } from "@/components/media/PosterCard";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { PosterGridSkeleton } from "@/components/ui/Skeleton";
import { fetchGenres } from "@/lib/api/genres";
import { fetchDiscover } from "@/lib/api/media";
import type { CatalogType, DiscoverSort } from "@/lib/api/types";
import { queryKeys } from "@/lib/queryKeys";
import { defaultDiscoverDirection } from "@/lib/media";
import {
  optionalText,
  parseDirection,
  parsePage,
  parseSort,
  parseType,
  parseYear,
  resetPage,
  setOrDelete,
} from "@/lib/url";

export function DiscoverPage() {
  const [params, setParams] = useSearchParams();
  const type = parseType(params.get("type"));
  const genre = optionalText(params.get("genre"));
  const year = parseYear(params.get("year"));
  const q = optionalText(params.get("q"));
  const sort = parseSort(params.get("sort"));
  const direction = parseDirection(params.get("direction"), sort);
  const page = parsePage(params.get("page"));
  const [yearDraft, setYearDraft] = useState(params.get("year") ?? "");

  const query = { type, genre, year, q, sort, direction, page, size: 20 as const };

  const genres = useQuery({
    queryKey: queryKeys.genres,
    queryFn: fetchGenres,
  });

  const discover = useQuery({
    queryKey: queryKeys.media.discover(query),
    queryFn: () => fetchDiscover(query),
  });

  function update(mutate: (next: URLSearchParams) => void) {
    const next = new URLSearchParams(params);
    mutate(next);
    setParams(next, { replace: true });
  }

  function setType(value: CatalogType | undefined) {
    update((next) => {
      setOrDelete(next, "type", value);
      resetPage(next);
    });
  }

  function setGenre(value: string | undefined) {
    update((next) => {
      setOrDelete(next, "genre", value);
      resetPage(next);
    });
  }

  function setYear(value: number | undefined) {
    update((next) => {
      setOrDelete(next, "year", value === undefined ? undefined : String(value));
      resetPage(next);
    });
  }

  function setSort(value: DiscoverSort) {
    update((next) => {
      next.set("sort", value);
      next.set("direction", defaultDiscoverDirection(value));
      resetPage(next);
    });
  }

  function toggleDirection() {
    update((next) => {
      next.set("sort", sort);
      next.set("direction", direction === "ASC" ? "DESC" : "ASC");
      resetPage(next);
    });
  }

  return (
    <PageWithSidebar
      sidebar={
        <FilterSidebar>
          <TypeFilter selected={type} onSelect={setType} />
          <GenreFilter
            genres={genres.data ?? []}
            selected={genre}
            onSelect={setGenre}
          />
          <FilterGroup legend="Year">
            <FilterButton
              selected={year === undefined}
              onClick={() => {
                setYearDraft("");
                setYear(undefined);
              }}
            >
              All
            </FilterButton>
            <Input
              type="number"
              inputMode="numeric"
              min={1800}
              max={2100}
              placeholder="e.g. 2024"
              aria-label="Release year"
              value={yearDraft}
              onChange={(event) => {
                const raw = event.target.value;
                setYearDraft(raw);
                if (!raw) {
                  setYear(undefined);
                  return;
                }
                if (/^\d{4}$/.test(raw)) {
                  const parsed = parseYear(raw);
                  if (parsed !== undefined) {
                    setYear(parsed);
                  }
                }
              }}
              className="h-9 w-28"
            />
          </FilterGroup>
          <FilterGroup legend="Sort">
            {(["QUALITY", "YEAR", "TITLE"] as const).map((option) => (
              <FilterButton
                key={option}
                selected={sort === option}
                onClick={() => setSort(option)}
              >
                {option === "QUALITY" ? "Quality" : option === "YEAR" ? "Year" : "Title"}
              </FilterButton>
            ))}
            <Button variant="outline" size="sm" onClick={toggleDirection} aria-label="Toggle sort direction">
              <ArrowDownUp className="size-3.5" />
              {direction === "ASC" ? "Ascending" : "Descending"}
            </Button>
          </FilterGroup>
        </FilterSidebar>
      }
    >
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl text-zinc-50 italic">Discover</h1>
          <p className="mt-1 text-sm text-zinc-400">Browse the shared catalog.</p>
        </div>
      </div>

      {discover.isLoading ? (
        <PosterGridSkeleton />
      ) : discover.data && discover.data.content.length === 0 ? (
        <div className="rounded-2xl border border-white/8 bg-white/3 px-6 py-12 text-center">
          <p className="text-zinc-300">Nothing in the catalog matches these filters.</p>
          <Link to="/search" className="mt-4 inline-block">
            <Button>Search the web</Button>
          </Link>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-[repeat(auto-fill,minmax(10rem,1fr))] gap-4">
            {discover.data?.content.map((item) => (
              <PosterCard
                key={item.id}
                to={`/media/${item.id}`}
                title={item.title}
                year={item.releaseYear}
                mediaTypeName={item.mediaType.name}
                posterUrl={item.posterUrl}
                rating={item.rating}
                ratingCount={item.ratingCount}
                inLibrary={item.inLibrary}
              />
            ))}
          </div>
          <Pagination
            page={page}
            totalPages={discover.data?.totalPages ?? 0}
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
