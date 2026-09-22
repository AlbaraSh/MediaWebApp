import { useMemo, useState } from "react";
import { FilterButton } from "@/components/filters/FilterButton";
import { Input } from "@/components/ui/Input";

type GenreFilterProps = {
  genres: string[];
  selected: string | undefined;
  onSelect: (genre: string | undefined) => void;
  showSearch?: boolean;
};

export function GenreFilter({
  genres,
  selected,
  onSelect,
  showSearch = true,
}: GenreFilterProps) {
  const [query, setQuery] = useState("");
  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) {
      return genres;
    }
    return genres.filter((genre) => genre.toLowerCase().includes(needle));
  }, [genres, query]);

  return (
    <fieldset className="space-y-2">
      <legend className="text-xs font-semibold tracking-wider text-zinc-500 uppercase">
        Genre
      </legend>
      {showSearch ? (
        <Input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Filter genres"
          aria-label="Filter genre list"
          className="h-9"
        />
      ) : null}
      <div className="flex flex-wrap gap-1.5">
        <FilterButton selected={!selected} onClick={() => onSelect(undefined)}>
          All
        </FilterButton>
        {filtered.map((genre) => (
          <FilterButton
            key={genre}
            selected={selected === genre}
            onClick={() => onSelect(selected === genre ? undefined : genre)}
          >
            {genre}
          </FilterButton>
        ))}
      </div>
    </fieldset>
  );
}
