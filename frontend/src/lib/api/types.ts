export const CATALOG_TYPES = ["MOVIE", "TV", "ANIME", "GAME"] as const;
export type CatalogType = (typeof CATALOG_TYPES)[number];

export const MEDIA_TYPE_NAMES = ["Movie", "TV Show", "Anime", "Game"] as const;
export type MediaTypeName = (typeof MEDIA_TYPE_NAMES)[number];

export const USER_MEDIA_STATUSES = ["PLANNED", "WATCHING", "COMPLETED", "DROPPED"] as const;
export type UserMediaStatus = (typeof USER_MEDIA_STATUSES)[number];

export const DISCOVER_SORTS = ["QUALITY", "YEAR", "TITLE"] as const;
export type DiscoverSort = (typeof DISCOVER_SORTS)[number];

export const SORT_DIRECTIONS = ["ASC", "DESC"] as const;
export type SortDirection = (typeof SORT_DIRECTIONS)[number];

export const PROVIDERS = ["tmdb", "jikan", "rawg"] as const;
export type Provider = (typeof PROVIDERS)[number];

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type MediaTypeDTO = {
  id: string;
  name: string;
};

export type MediaResponseDTO = {
  id: string;
  title: string;
  description: string | null;
  releaseYear: number | null;
  genres: string[];
  rating: number | null;
  ratingCount: number | null;
  mediaType: MediaTypeDTO;
  createdAt: string;
  updatedAt: string;
  inLibrary: boolean | null;
  posterUrl: string | null;
};

export type ExternalMediaDTO = {
  title: string;
  description: string | null;
  releaseYear: number | null;
  mediaType: string;
  provider: string;
  externalId: string;
  genres: string[];
  externalRating: number | null;
  externalRatingCount: number | null;
  posterUrl: string | null;
};

export type UserMediaResponseDTO = {
  status: UserMediaStatus;
  rating: number | null;
  review: string | null;
  createdAt: string;
  updatedAt: string;
  mediaId: string;
  title: string;
  releaseYear: number | null;
  mediaType: MediaTypeDTO;
  genres: string[];
  posterUrl: string | null;
};

export type UserMediaStatusCounts = {
  planned: number;
  watching: number;
  completed: number;
  dropped: number;
};

export type LibraryPageResponse = PageResponse<UserMediaResponseDTO> & {
  counts: UserMediaStatusCounts;
};

export type UserMediaRequestDTO = {
  mediaId: string;
  status: UserMediaStatus;
  rating?: number | null;
  review?: string | null;
};

export type ImportMediaRequestDTO = {
  provider: Provider;
  externalId: string;
};

export type RecommendationItemDTO = {
  mediaId: string;
  title: string;
  mediaType: string;
  releaseYear: number | null;
  externalRating: number | null;
  externalRatingCount: number | null;
  posterUrl: string | null;
};

export type RecommendationResponseDTO = {
  movies: RecommendationItemDTO[];
  tvShows: RecommendationItemDTO[];
  anime: RecommendationItemDTO[];
  games: RecommendationItemDTO[];
};

export type AuthResponseDTO = {
  token: string;
};

export type LoginRequestDTO = {
  email: string;
  password: string;
};

export type RegisterRequestDTO = {
  email: string;
  username: string;
  password: string;
};

export type ApiErrorBody = {
  error: string;
  status: number;
  details: Record<string, string> | null;
};

export type DiscoverQuery = {
  type?: CatalogType;
  genre?: string;
  year?: number;
  q?: string;
  sort: DiscoverSort;
  direction: SortDirection;
  page: number;
  size?: number;
};

export type LibrarySort = "ADDED" | "RATING";

export type LibraryQuery = {
  status: UserMediaStatus;
  type?: CatalogType;
  genre?: string;
  minRating?: number;
  maxRating?: number;
  q?: string;
  sort?: LibrarySort;
  direction?: SortDirection;
  page: number;
  size?: number;
};
