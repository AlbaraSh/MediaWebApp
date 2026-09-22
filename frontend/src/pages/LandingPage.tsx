import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/Button";
import { PosterCard } from "@/components/media/PosterCard";
import { PosterGridSkeleton } from "@/components/ui/Skeleton";
import { useAuth } from "@/hooks/useAuth";
import { fetchDiscover } from "@/lib/api/media";
import { queryKeys } from "@/lib/queryKeys";

export function LandingPage() {
  const { isAuthenticated } = useAuth();
  const quality = useQuery({
    queryKey: queryKeys.media.discover({
      sort: "QUALITY",
      direction: "DESC",
      page: 0,
      size: 10,
    }),
    queryFn: () =>
      fetchDiscover({
        sort: "QUALITY",
        direction: "DESC",
        page: 0,
        size: 10,
      }),
  });

  return (
    <div className="space-y-12">
      <section className="max-w-2xl space-y-5 pt-8">
        <p className="text-xs font-semibold tracking-[0.25em] text-zinc-500 uppercase">
          Catalog · Shelf · Taste
        </p>
        <h1 className="font-display text-[clamp(2.4rem,6vw,4.2rem)] leading-[1.05] text-zinc-50 italic">
          All your media in one shelf. Recommendations that you will love.
        </h1>
        <p className="max-w-xl text-base text-zinc-400 sm:text-lg">
          Browse movies, TV, anime, and games. Keep an online personal shelf. Get
          personalized recommendations.
        </p>
        <div className="flex flex-wrap gap-3">
          {isAuthenticated ? (
            <Link to="/discover">
              <Button size="lg">Browse catalog</Button>
            </Link>
          ) : (
            <>
              <Link to="/register">
                <Button size="lg">Create account</Button>
              </Link>
              <Link to="/login">
                <Button size="lg" variant="outline">
                  Sign in
                </Button>
              </Link>
              <Link to="/discover">
                <Button size="lg" variant="ghost">
                  Browse as guest
                </Button>
              </Link>
            </>
          )}
        </div>
      </section>

      <section className="space-y-4">
        <div className="flex items-end justify-between gap-4">
          <h2 className="font-display text-2xl text-zinc-100">Quality Discover</h2>
          <Link to="/discover" className="text-sm text-zinc-400 hover:text-zinc-100">
            See all
          </Link>
        </div>
        {quality.isLoading ? (
          <PosterGridSkeleton count={8} />
        ) : (
          <div className="grid grid-cols-[repeat(auto-fill,minmax(10rem,1fr))] gap-4">
            {quality.data?.content.map((item) => (
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
        )}
      </section>
    </div>
  );
}
