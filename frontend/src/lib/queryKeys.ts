import type { CatalogType, DiscoverQuery, LibraryQuery } from "@/lib/api/types";

export const queryKeys = {
  media: {
    all: ["media"] as const,
    discover: (params: DiscoverQuery) => ["media", "discover", params] as const,
    detail: (id: string) => ["media", "detail", id] as const,
    searchCatalog: (q: string, type: CatalogType, page: number) =>
      ["media", "catalog-search", q, type, page] as const,
    searchWeb: (q: string, type: CatalogType) => ["media", "web-search", q, type] as const,
  },
  genres: ["genres"] as const,
  userMedia: {
    all: ["user-media"] as const,
    list: (params: LibraryQuery) => ["user-media", "list", params] as const,
    byMedia: (mediaId: string) => ["user-media", "by-media", mediaId] as const,
    genres: ["user-media", "genres"] as const,
    highRatings: ["user-media", "high-ratings"] as const,
  },
  recommendations: {
    all: ["recommendations"] as const,
    similar: (mediaId: string) => ["recommendations", "similar", mediaId] as const,
    user: (type: CatalogType) => ["recommendations", "user", type] as const,
  },
};
