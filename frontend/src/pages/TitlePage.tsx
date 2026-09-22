import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { Poster } from "@/components/media/Poster";
import { PosterCard } from "@/components/media/PosterCard";
import { TypeChip } from "@/components/media/TypeChip";
import { ShelfForm } from "@/components/shelf/ShelfForm";
import { Button } from "@/components/ui/Button";
import { Dialog } from "@/components/ui/Dialog";
import { RowSkeleton } from "@/components/ui/Skeleton";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { useAuth } from "@/hooks/useAuth";
import { useInvalidateMediaCaches } from "@/hooks/useInvalidateMediaCaches";
import { isNotFoundError } from "@/lib/api/errors";
import { fetchMediaById } from "@/lib/api/media";
import { fetchSimilar } from "@/lib/api/recommendations";
import { deleteShelfEntry, fetchShelfEntry, upsertShelf } from "@/lib/api/userMedia";
import type { RecommendationItemDTO, UserMediaRequestDTO } from "@/lib/api/types";
import { queryKeys } from "@/lib/queryKeys";
import { loginPathWithNext } from "@/lib/redirect";
import {
  formatCount,
  formatInstant,
  formatScore,
  formatYear,
  statusLabel,
} from "@/lib/media";

const SIMILAR_ROWS: Array<{
  key: keyof {
    movies: RecommendationItemDTO[];
    tvShows: RecommendationItemDTO[];
    anime: RecommendationItemDTO[];
    games: RecommendationItemDTO[];
  };
  label: string;
}> = [
  { key: "movies", label: "Similar movies" },
  { key: "tvShows", label: "Similar TV shows" },
  { key: "anime", label: "Similar anime" },
  { key: "games", label: "Similar games" },
];

export function TitlePage() {
  const { id = "" } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const invalidate = useInvalidateMediaCaches();
  const [editorOpen, setEditorOpen] = useState(false);

  const mediaQuery = useQuery({
    queryKey: queryKeys.media.detail(id),
    queryFn: () => fetchMediaById(id),
    enabled: Boolean(id),
    retry: false,
  });

  const shelfQuery = useQuery({
    queryKey: queryKeys.userMedia.byMedia(id),
    queryFn: () => fetchShelfEntry(id),
    enabled: Boolean(id) && isAuthenticated,
    retry: false,
  });

  const similarQuery = useQuery({
    queryKey: queryKeys.recommendations.similar(id),
    queryFn: () => fetchSimilar(id),
    enabled: Boolean(id) && mediaQuery.isSuccess,
    retry: false,
  });

  const upsert = useMutation({
    mutationFn: upsertShelf,
    onSuccess: async () => {
      await invalidate();
      setEditorOpen(false);
    },
  });

  const remove = useMutation({
    mutationFn: () => deleteShelfEntry(id),
    onSuccess: async () => {
      await invalidate();
    },
  });

  if (mediaQuery.isError && isNotFoundError(mediaQuery.error)) {
    return <NotFoundPage />;
  }

  if (mediaQuery.isError) {
    return (
      <p className="text-sm text-zinc-400">
        {mediaQuery.error instanceof Error ? mediaQuery.error.message : "Could not load this title."}
      </p>
    );
  }

  if (mediaQuery.isLoading || !mediaQuery.data) {
    return (
      <div className="grid gap-8 lg:grid-cols-[16rem_minmax(0,1fr)]">
        <div className="aspect-[2/3] animate-pulse rounded-2xl bg-white/8" />
        <div className="space-y-3">
          <div className="h-8 w-2/3 animate-pulse rounded bg-white/8" />
          <div className="h-24 animate-pulse rounded bg-white/8" />
        </div>
      </div>
    );
  }

  const media = mediaQuery.data;
  const shelf = shelfQuery.data ?? null;

  async function saveShelf(body: UserMediaRequestDTO) {
    await upsert.mutateAsync(body);
  }

  function onAddToShelf() {
    if (!isAuthenticated) {
      navigate(loginPathWithNext(`/media/${id}`));
      return;
    }
    setEditorOpen(true);
  }

  function onRemove() {
    if (!window.confirm("Remove this title from your shelf?")) {
      return;
    }
    void remove.mutate();
  }

  const similar = similarQuery.data;
  const similarUnavailable = similarQuery.isSuccess && similar == null;

  return (
    <div className="space-y-12">
      <div className="grid gap-8 lg:grid-cols-[16rem_minmax(0,1fr)]">
        <div className="overflow-hidden rounded-2xl ring-1 ring-white/10">
          <div className="aspect-[2/3]">
            <Poster
              posterUrl={media.posterUrl}
              title={media.title}
              mediaTypeName={media.mediaType.name}
            />
          </div>
        </div>
        <div className="min-w-0 space-y-4">
          <TypeChip name={media.mediaType.name} />
          <h1 className="font-display text-[clamp(1.8rem,4vw,3rem)] leading-tight text-zinc-50">
            {media.title}
          </h1>
          <p className="text-sm text-zinc-400">
            {formatYear(media.releaseYear)}
            {media.genres.length > 0 ? ` · ${media.genres.join(" · ")}` : ""}
          </p>
          <p className="text-zinc-200">
            Community score {formatScore(media.rating)}
            <span className="ml-2 text-sm text-zinc-500">
              {formatCount(media.ratingCount)} ratings
            </span>
          </p>
          <p className="max-w-2xl text-sm leading-relaxed text-zinc-300 whitespace-pre-wrap">
            {media.description || "No description yet."}
          </p>
          <p className="text-xs text-zinc-500">
            Added {formatInstant(media.createdAt)}
            {media.updatedAt ? ` · Updated ${formatInstant(media.updatedAt)}` : ""}
          </p>

          {isAuthenticated && shelfQuery.isLoading ? (
            <div className="h-24 animate-pulse rounded-2xl bg-white/8" />
          ) : isAuthenticated && shelf ? (
            <div className="rounded-2xl border border-white/10 bg-white/4 p-4">
              <p className="text-sm text-zinc-200">
                On your shelf as{" "}
                <span className="font-medium">
                  {statusLabel(shelf.status, media.mediaType.name)}
                </span>
                {" · "}
                {shelf.rating == null ? "Unrated" : `Your score ${shelf.rating}`}
              </p>
              {shelf.review ? (
                <p className="mt-2 line-clamp-4 text-sm text-zinc-400">{shelf.review}</p>
              ) : null}
              <p className="mt-2 text-xs text-zinc-500">
                Updated {formatInstant(shelf.updatedAt)}
              </p>
              <div className="mt-3 flex flex-wrap gap-2">
                <Button size="sm" onClick={() => setEditorOpen(true)}>
                  Edit
                </Button>
                <Button size="sm" variant="danger" onClick={onRemove} disabled={remove.isPending}>
                  Remove
                </Button>
              </div>
            </div>
          ) : (
            <Button onClick={onAddToShelf}>Add to shelf</Button>
          )}
        </div>
      </div>

      <section className="space-y-6">
        <h2 className="font-display text-2xl text-zinc-100">Similar</h2>
        {similarQuery.isLoading ? (
          <RowSkeleton />
        ) : similarUnavailable ? (
          <p className="text-sm text-zinc-400">
            Recommendations for this title will appear shortly.
          </p>
        ) : (
          SIMILAR_ROWS.map((row) => {
            const items = similar?.[row.key] ?? [];
            if (items.length === 0) {
              return null;
            }
            return <SimilarRow key={row.key} label={row.label} items={items} />;
          })
        )}
      </section>

      <Dialog
        open={editorOpen}
        title={shelf ? "Edit shelf" : "Add to shelf"}
        onClose={() => setEditorOpen(false)}
      >
        <ShelfForm
          key={shelf?.updatedAt ?? "new"}
          mediaId={media.id}
          mediaTypeName={media.mediaType.name}
          existing={shelf}
          submitting={upsert.isPending}
          onSubmit={saveShelf}
        />
      </Dialog>
    </div>
  );
}

function SimilarRow({ label, items }: { label: string; items: RecommendationItemDTO[] }) {
  return (
    <div className="space-y-3">
      <h3 className="text-sm font-semibold tracking-wide text-zinc-400 uppercase">{label}</h3>
      <div className="grid grid-cols-[repeat(auto-fill,minmax(9rem,1fr))] gap-3">
        {items.map((item) => (
          <PosterCard
            key={item.mediaId}
            to={`/media/${item.mediaId}`}
            title={item.title}
            year={item.releaseYear}
            mediaTypeName={item.mediaType}
            posterUrl={item.posterUrl}
            rating={item.externalRating}
            ratingCount={item.externalRatingCount}
          />
        ))}
      </div>
    </div>
  );
}
