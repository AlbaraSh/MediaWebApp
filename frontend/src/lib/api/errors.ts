import type { ApiErrorBody } from "@/lib/api/types";

export class ApiError extends Error {
  readonly status: number;
  readonly details: Record<string, string> | null;

  constructor(body: ApiErrorBody) {
    super(body.error);
    this.name = "ApiError";
    this.status = body.status;
    this.details = body.details;
  }
}

export class NotFoundError extends ApiError {
  constructor(body: ApiErrorBody) {
    super(body);
    this.name = "NotFoundError";
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

export function isNotFoundError(error: unknown): error is NotFoundError {
  return error instanceof NotFoundError;
}
