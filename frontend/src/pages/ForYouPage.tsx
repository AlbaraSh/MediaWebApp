import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { FilterButton, FilterGroup } from "@/components/filters/FilterButton";
import { PosterCard } from "@/components/media/PosterCard";
import { PosterGridSkeleton, RowSkeleton } from "@/components/ui/Skeleton";
import { fetchLibrary } from "@/lib/api/userMedia";
import { fetchForYou } from "@/lib/api/recommendations";
import { CATALOG_TYPES } from "@/lib/api/types";
import type { CatalogType, RecommendationItemDTO, UserMediaStatus } from "@/lib/api/types";
import { queryKeys } from "@/lib/queryKeys";
import { isCatalogType, TYPE_LABEL } from "@/lib/media";

const OTHER_ROWS: Array<{
  type: CatalogType;
  key: "movies" | "tvShows" | "anime" | "games";
}> = [
  { type: "MOVIE", key: "movies" },
  { type: "TV", key: "tvShows" },
  { type: "ANIME", key: "anime" },
  { type: "GAME", key: "games" },
];

async function hasHighRating(): Promise<boolean> {
  const statuses: UserMediaStatus[] = ["PLANNED", "WATCHING", "COMPLETED", "DROPPED"];
  const pages = await Promise.all(
    statuses.map((status) =>
      fetchLibrary({ status, minRating: 7, page: 0, size: 1 }),
    ),
  );
  return pages.some((page) => page.totalElements > 0);
}

export function ForYouPage() {
  const [params, setParams] = useSearchParams();
  const typeParam = params.get("type");
  const type: CatalogType = isCatalogType(typeParam) ? typeParam : "MOVIE";

  useEffect(() => {
    if (!isCatalogType(typeParam)) {
      const next = new URLSearchParams(params);
      next.set("type", "MOVIE");
      setParams(next, { replace: true });
    }
  }, [params, setParams, typeParam]);

  const recs = useQuery({
    queryKey: queryKeys.recommendations.user(type),
    queryFn: () => fetchForYou(type),
  });

  const taste = useQuery({
    queryKey: queryKeys.userMedia.highRatings,
    queryFn: hasHighRating,
  });

  const personalized = taste.data === true;
  const heroKey = OTHER_ROWS.find((row) => row.type === type)?.key ?? "movies";
  const heroItems = recs.data?.[heroKey] ?? [];

  return (
    <div className="space-y-10">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl text-zinc-50 italic">For You</h1>
          <p className="mt-2 max-w-2xl text-sm text-zinc-400">
            {taste.isLoading
              ? "Loading your taste profile…"
              : personalized
                ? "Picked from titles you rated 7 or higher."
                : "These are highly rated titles not on your shelf. Rate something 7–10 to unlock recommendations based on your taste."}
          </p>
        </div>
        <FilterGroup legend="Type">
          {CATALOG_TYPES.map((option) => (
            <FilterButton
              key={option}
              selected={type === option}
              onClick={() => setParams({ type: option }, { replace: true })}
            >
              {TYPE_LABEL[option]}
            </FilterButton>
          ))}
        </FilterGroup>
      </div>

      <section className="space-y-4">
        <h2 className="text-lg font-semibold text-zinc-100">{TYPE_LABEL[type]}</h2>
        {recs.isLoading ? (
          <PosterGridSkeleton count={10} />
        ) : heroItems.length === 0 ? (
          <p className="text-sm text-zinc-400">No recommendations in this type yet.</p>
        ) : (
          <div className="grid grid-cols-[repeat(auto-fill,minmax(10rem,1fr))] gap-4">
            {heroItems.map((item) => (
              <RecCard key={item.mediaId} item={item} />
            ))}
          </div>
        )}
      </section>

      {OTHER_ROWS.filter((row) => row.type !== type).map((row) => {
        const items = recs.data?.[row.key] ?? [];
        return (
          <section key={row.type} className="space-y-3">
            <h2 className="text-sm font-semibold tracking-wide text-zinc-400 uppercase">
              {TYPE_LABEL[row.type]}
            </h2>
            {recs.isLoading ? (
              <RowSkeleton count={5} />
            ) : items.length === 0 ? (
              <p className="text-sm text-zinc-500">Nothing here yet.</p>
            ) : (
              <div className="grid grid-cols-[repeat(auto-fill,minmax(8.5rem,1fr))] gap-3">
                {items.map((item) => (
                  <RecCard key={item.mediaId} item={item} />
                ))}
              </div>
            )}
          </section>
        );
      })}
    </div>
  );
}

function RecCard({ item }: { item: RecommendationItemDTO }) {
  return (
    <PosterCard
      to={`/media/${item.mediaId}`}
      title={item.title}
      year={item.releaseYear}
      mediaTypeName={item.mediaType}
      posterUrl={item.posterUrl}
      rating={item.externalRating}
      ratingCount={item.externalRatingCount}
    />
  );
}
