import { FilterButton, FilterGroup } from "@/components/filters/FilterButton";
import { CATALOG_TYPES } from "@/lib/api/types";
import type { CatalogType } from "@/lib/api/types";
import { TYPE_LABEL } from "@/lib/media";

type TypeFilterProps = {
  selected: CatalogType | undefined;
  onSelect: (type: CatalogType | undefined) => void;
};

export function TypeFilter({ selected, onSelect }: TypeFilterProps) {
  return (
    <FilterGroup legend="Type">
      <FilterButton selected={!selected} onClick={() => onSelect(undefined)}>
        All
      </FilterButton>
      {CATALOG_TYPES.map((type) => (
        <FilterButton
          key={type}
          selected={selected === type}
          onClick={() => onSelect(selected === type ? undefined : type)}
        >
          {TYPE_LABEL[type]}
        </FilterButton>
      ))}
    </FilterGroup>
  );
}
