import type { CatalogType, DiscoverSort, SortDirection } from "@/lib/api/types";
import { isCatalogType } from "@/lib/media";

export function parsePage(raw: string | null): number {
  if (!raw) {
    return 0;
  }
  const value = Number.parseInt(raw, 10);
  return Number.isFinite(value) && value >= 0 ? value : 0;
}

export function parseType(raw: string | null): CatalogType | undefined {
  return isCatalogType(raw) ? raw : undefined;
}

export function parseSort(raw: string | null): DiscoverSort {
  if (raw === "QUALITY" || raw === "YEAR" || raw === "TITLE") {
    return raw;
  }
  return "QUALITY";
}

export function parseDirection(raw: string | null, sort: DiscoverSort): SortDirection {
  if (raw === "ASC" || raw === "DESC") {
    return raw;
  }
  return sort === "TITLE" ? "ASC" : "DESC";
}

export function parseYear(raw: string | null): number | undefined {
  if (!raw) {
    return undefined;
  }
  const value = Number.parseInt(raw, 10);
  if (!Number.isFinite(value) || value < 1800 || value > 2100) {
    return undefined;
  }
  return value;
}

export function optionalText(raw: string | null): string | undefined {
  const trimmed = raw?.trim();
  return trimmed ? trimmed : undefined;
}

export function setOrDelete(params: URLSearchParams, key: string, value: string | undefined): void {
  if (value === undefined || value === "") {
    params.delete(key);
  } else {
    params.set(key, value);
  }
}

export function resetPage(params: URLSearchParams): void {
  params.delete("page");
}
