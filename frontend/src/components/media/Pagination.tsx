import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/Button";

type PaginationProps = {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
};

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }

  const human = page + 1;

  return (
    <nav className="mt-8 flex items-center justify-center gap-3" aria-label="Pagination">
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(page - 1)}
        disabled={page <= 0}
        aria-label="Previous page"
      >
        <ChevronLeft className="size-4" />
        Prev
      </Button>
      <p className="text-sm text-zinc-400">
        Page <span className="text-zinc-100">{human}</span> of {totalPages}
      </p>
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
        aria-label="Next page"
      >
        Next
        <ChevronRight className="size-4" />
      </Button>
    </nav>
  );
}
