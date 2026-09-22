import { useEffect, useRef, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { FieldError, Label, TextArea } from "@/components/ui/Input";
import { isApiError } from "@/lib/api/errors";
import type { UserMediaRequestDTO, UserMediaResponseDTO } from "@/lib/api/types";
import { USER_MEDIA_STATUSES } from "@/lib/api/types";
import { statusLabel } from "@/lib/media";
import { cn } from "@/lib/cn";

const schema = z
  .object({
    status: z.enum(["PLANNED", "WATCHING", "COMPLETED", "DROPPED"]),
    rating: z.union([z.number().int().min(1).max(10), z.nan()]).optional(),
    review: z.string().max(2000, "Review must be at most 2000 characters"),
  })
  .superRefine((value, ctx) => {
    if (value.status === "COMPLETED" && (value.rating === undefined || Number.isNaN(value.rating))) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["rating"],
        message: "A score is required when marking as Completed.",
      });
    }
  });

type FormValues = z.infer<typeof schema>;

type ShelfFormProps = {
  mediaId: string;
  mediaTypeName: string;
  existing: UserMediaResponseDTO | null;
  onSubmit: (body: UserMediaRequestDTO) => Promise<void>;
  submitting: boolean;
};

export function ShelfForm({
  mediaId,
  mediaTypeName,
  existing,
  onSubmit,
  submitting,
}: ShelfFormProps) {
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      status: existing?.status ?? "PLANNED",
      rating: existing?.rating ?? Number.NaN,
      review: existing?.review ?? "",
    },
  });

  const status = useWatch({ control: form.control, name: "status" });
  const rating = useWatch({ control: form.control, name: "rating" });
  const review = useWatch({ control: form.control, name: "review" }) ?? "";
  const previousStatus = useRef(status);
  const [ratingMoved, setRatingMoved] = useState(existing?.rating != null);

  useEffect(() => {
    if (previousStatus.current === "COMPLETED" && status !== "COMPLETED") {
      form.setValue("rating", Number.NaN, { shouldValidate: true });
      setRatingMoved(false);
    }
    previousStatus.current = status;
  }, [form, status]);

  async function handleSubmit(values: FormValues) {
    const ratingValue =
      values.status === "COMPLETED"
        ? values.rating
        : !ratingMoved || values.rating === undefined || Number.isNaN(values.rating)
          ? null
          : values.rating;
    const reviewValue = values.review.trim() === "" ? null : values.review;
    try {
      await onSubmit({
        mediaId,
        status: values.status,
        rating: ratingValue,
        review: reviewValue,
      });
    } catch (error) {
      if (isApiError(error) && error.status === 400 && error.details) {
        for (const [field, message] of Object.entries(error.details)) {
          if (field === "status" || field === "rating" || field === "review") {
            form.setError(field, { message });
          }
        }
        if (!error.details.status && !error.details.rating && !error.details.review) {
          form.setError("root", { message: error.message });
        }
        return;
      }
      form.setError("root", {
        message: error instanceof Error ? error.message : "Could not save",
      });
    }
  }

  const completedWithoutScore = status === "COMPLETED" && !ratingMoved;
  const sliderValue = ratingMoved && typeof rating === "number" && !Number.isNaN(rating) ? rating : 5;

  return (
    <form className="space-y-5" onSubmit={form.handleSubmit(handleSubmit)} noValidate>
      <fieldset>
        <legend className="mb-2 text-xs font-semibold tracking-wider text-zinc-500 uppercase">
          Status
        </legend>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {USER_MEDIA_STATUSES.map((value) => (
            <button
              key={value}
              type="button"
              aria-pressed={status === value}
              onClick={() => form.setValue("status", value, { shouldValidate: true })}
              className={cn(
                "rounded-full px-3 py-2 text-sm transition",
                "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40",
                status === value
                  ? "bg-zinc-100 font-medium text-zinc-950"
                  : "bg-white/8 text-zinc-300 hover:bg-white/12",
              )}
            >
              {statusLabel(value, mediaTypeName)}
            </button>
          ))}
        </div>
      </fieldset>

      <div>
        <Label htmlFor="shelf-rating">Score (1–10)</Label>
        <div className="flex items-center gap-3">
          <input
            id="shelf-rating"
            type="range"
            min={1}
            max={10}
            step={1}
            value={sliderValue}
            onChange={(event) => {
              setRatingMoved(true);
              form.setValue("rating", Number(event.target.value), { shouldValidate: true });
            }}
            className="h-2 w-full cursor-pointer appearance-none rounded-full bg-white/15 accent-zinc-100"
          />
          <span className="w-8 text-right text-sm tabular-nums text-zinc-100">
            {ratingMoved ? sliderValue : "—"}
          </span>
        </div>
        {status === "COMPLETED" ? (
          <p className="mt-1.5 text-xs text-zinc-500">
            A score is required when marking as Completed.
          </p>
        ) : null}
        <FieldError message={form.formState.errors.rating?.message} />
      </div>

      <div>
        <div className="mb-1.5 flex items-center justify-between">
          <Label htmlFor="shelf-review">Review</Label>
          <span className="text-xs text-zinc-500">{review.length}/2000</span>
        </div>
        <TextArea
          id="shelf-review"
          maxLength={2000}
          {...form.register("review")}
        />
        <FieldError message={form.formState.errors.review?.message} />
      </div>

      <FieldError message={form.formState.errors.root?.message} />

      <Button type="submit" className="w-full" disabled={submitting || completedWithoutScore}>
        {submitting ? "Saving…" : "Save"}
      </Button>
    </form>
  );
}
