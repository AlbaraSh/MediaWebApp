import { useEffect, useState, type FormEvent } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Pagination } from "@/components/media/Pagination";
import { PosterCard } from "@/components/media/PosterCard";
import { Button } from "@/components/ui/Button";
import { Input, Label } from "@/components/ui/Input";
import { PosterGridSkeleton } from "@/components/ui/Skeleton";
import { useAuth } from "@/hooks/useAuth";
import { useInvalidateMediaCaches } from "@/hooks/useInvalidateMediaCaches";
import { fetchDiscover, importMedia, searchExternal } from "@/lib/api/media";
import type { CatalogType, ExternalMediaDTO, Provider } from "@/lib/api/types";
import { CATALOG_TYPES } from "@/lib/api/types";
import { isProvider, normalizeProvider, TYPE_LABEL } from "@/lib/media";
import { queryKeys } from "@/lib/queryKeys";
import { loginPathWithNext } from "@/lib/redirect";
import { optionalText, parsePage, parseType, setOrDelete } from "@/lib/url";

const autoImportGuard = new Set<string>();

function guestResumePath(q: string, type: CatalogType, provider: string, externalId: string): string {
  return `/search?q=${encodeURIComponent(q)}&type=${encodeURIComponent(type)}&importProvider=${encodeURIComponent(provider)}&importExternalId=${encodeURIComponent(externalId)}`;
}

export function SearchPage() {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const invalidate = useInvalidateMediaCaches();

  const q = optionalText(params.get("q")) ?? "";
  const type = parseType(params.get("type")) ?? "MOVIE";
  const page = parsePage(params.get("page"));
  const importProvider = params.get("importProvider");
  const importExternalId = params.get("importExternalId");
  const showWeb = params.get("web") === "1";

  const [draftQ, setDraftQ] = useState(q);
  const [draftType, setDraftType] = useState<CatalogType>(type);
  const [importingKey, setImportingKey] = useState<string | null>(null);

  useEffect(() => {
    setDraftQ(q);
    setDraftType(type);
  }, [q, type]);

  const catalog = useQuery({
    queryKey: queryKeys.media.searchCatalog(q, type, page),
    queryFn: () =>
      fetchDiscover({
        q,
        type,
        sort: "QUALITY",
        direction: "DESC",
        page,
        size: 20,
      }),
    enabled: q.length > 0,
  });

  const web = useQuery({
    queryKey: queryKeys.media.searchWeb(q, type),
    queryFn: () => searchExternal(q, type),
    enabled: q.length > 0 && showWeb,
  });

  const importMutation = useMutation({
    mutationFn: importMedia,
    onSuccess: async (media) => {
      await invalidate();
      navigate(`/media/${media.id}`, { replace: true });
    },
  });

  useEffect(() => {
    if (!isAuthenticated || !importProvider || !importExternalId) {
      return;
    }
    const provider = importProvider.toLowerCase();
    if (!isProvider(provider)) {
      return;
    }
    const key = `${provider}:${importExternalId}`;
    if (autoImportGuard.has(key)) {
      return;
    }
    autoImportGuard.add(key);

    void (async () => {
      try {
        const media = await importMedia({
          provider,
          externalId: importExternalId,
        });
        await invalidate();
        navigate(`/media/${media.id}`, { replace: true });
      } catch {
        autoImportGuard.delete(key);
        const next = new URLSearchParams(window.location.search);
        next.delete("importProvider");
        next.delete("importExternalId");
        setParams(next, { replace: true });
      }
    })();
  }, [importExternalId, importProvider, invalidate, isAuthenticated, navigate, setParams]);

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const next = new URLSearchParams();
    const query = draftQ.trim();
    if (query) {
      next.set("q", query);
    }
    next.set("type", draftType);
    setParams(next);
  }

  function searchMore() {
    const next = new URLSearchParams(params);
    next.set("web", "1");
    setParams(next);
  }

  async function onWebClick(item: ExternalMediaDTO) {
    const provider = normalizeProvider(item.provider) as Provider;
    if (!isProvider(provider)) {
      return;
    }
    const key = `${provider}:${item.externalId}`;
    if (!isAuthenticated) {
      const resume = guestResumePath(q || draftQ.trim(), type, provider, item.externalId);
      navigate(loginPathWithNext(resume));
      return;
    }
    setImportingKey(key);
    try {
      await importMutation.mutateAsync({
        provider,
        externalId: item.externalId,
      });
    } finally {
      setImportingKey(null);
    }
  }

  return (
    <div className="space-y-10">
      <div>
        <h1 className="font-display text-3xl text-zinc-50 italic">Search</h1>
        <p className="mt-1 text-sm text-zinc-400">
          Search the catalog first. Use Search more if the title is not here yet.
        </p>
      </div>

      <form onSubmit={submitSearch} className="flex flex-wrap items-end gap-3">
        <div className="min-w-60 flex-1">
          <Label htmlFor="search-q">Query</Label>
          <Input
            id="search-q"
            value={draftQ}
            onChange={(event) => setDraftQ(event.target.value)}
            placeholder="Title, studio, series…"
            required
          />
        </div>
        <div>
          <Label htmlFor="search-type">Type</Label>
          <select
            id="search-type"
            value={draftType}
            onChange={(event) => setDraftType(event.target.value as CatalogType)}
            className="h-10 rounded-xl border border-white/10 bg-zinc-950/60 px-3 text-sm text-zinc-100 outline-none focus:ring-2 focus:ring-white/15"
          >
            {CATALOG_TYPES.map((option) => (
              <option key={option} value={option}>
                {TYPE_LABEL[option]}
              </option>
            ))}
          </select>
        </div>
        <Button type="submit">Search</Button>
      </form>

      {q ? (
        <>
          <div>
            <Button variant="outline" onClick={searchMore} disabled={showWeb}>
              {showWeb ? "Showing web results" : "Search more"}
            </Button>
          </div>
          <section className="space-y-4">
            <h2 className="text-lg font-semibold text-zinc-100">In the catalog</h2>
            {catalog.isLoading ? (
              <PosterGridSkeleton count={8} />
            ) : catalog.data && catalog.data.content.length === 0 ? (
              <p className="text-sm text-zinc-400">
                No catalog matches.
                {showWeb ? "" : " Use Search more to look on the web."}
              </p>
            ) : (
              <>
                <div className="grid grid-cols-[repeat(auto-fill,minmax(10rem,1fr))] gap-4">
                  {catalog.data?.content.map((item) => (
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
                  totalPages={catalog.data?.totalPages ?? 0}
                  onPageChange={(nextPage) => {
                    const next = new URLSearchParams(params);
                    if (nextPage <= 0) {
                      next.delete("page");
                    } else {
                      next.set("page", String(nextPage));
                    }
                    setOrDelete(next, "q", q);
                    next.set("type", type);
                    setParams(next);
                  }}
                />
              </>
            )}
          </section>

          {showWeb ? (
          <section className="space-y-4">
            <h2 className="text-lg font-semibold text-zinc-100">On the web</h2>
            {web.isLoading ? (
              <PosterGridSkeleton count={6} />
            ) : web.data && web.data.length === 0 ? (
              <p className="text-sm text-zinc-400">No web results for this query.</p>
            ) : (
              <div className="grid grid-cols-[repeat(auto-fill,minmax(10rem,1fr))] gap-4">
                {web.data?.map((item) => {
                  const key = `${normalizeProvider(item.provider)}:${item.externalId}`;
                  return (
                    <PosterCard
                      key={key}
                      title={item.title}
                      year={item.releaseYear}
                      mediaTypeName={item.mediaType}
                      posterUrl={item.posterUrl}
                      rating={item.externalRating}
                      ratingCount={item.externalRatingCount}
                      loading={importingKey === key}
                      onClick={() => void onWebClick(item)}
                    />
                  );
                })}
              </div>
            )}
          </section>
          ) : null}
        </>
      ) : (
        <p className="text-sm text-zinc-500">Enter a query to search the catalog.</p>
      )}
    </div>
  );
}
