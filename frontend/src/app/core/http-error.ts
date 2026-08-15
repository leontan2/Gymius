import { HttpErrorResponse } from '@angular/common/http';

interface ApiErrorBody {
  message?: unknown;
  detail?: unknown;
}

export function apiErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  if (error.status === 0) {
    return 'The server could not be reached. Check your connection and try again.';
  }

  const body = error.error as ApiErrorBody | string | null;
  if (typeof body === 'object' && body !== null) {
    const message = firstSafeMessage(body.message, body.detail);
    if (message) {
      return message;
    }
  }

  return fallback;
}

function firstSafeMessage(...values: unknown[]): string | null {
  for (const value of values) {
    if (typeof value === 'string' && value.trim()) {
      return value.trim().slice(0, 300);
    }
  }

  return null;
}
