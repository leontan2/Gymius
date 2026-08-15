import { HttpErrorResponse } from '@angular/common/http';
import { apiErrorMessage } from './http-error';

describe('apiErrorMessage', () => {
  it('uses a safe API message when one is provided', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { message: '  Please correct the highlighted fields.  ' }
    });

    expect(apiErrorMessage(error, 'Fallback')).toBe('Please correct the highlighted fields.');
  });

  it('uses a connection-specific message for network failures', () => {
    const error = new HttpErrorResponse({ status: 0 });

    expect(apiErrorMessage(error, 'Fallback')).toContain('could not be reached');
  });

  it('does not expose arbitrary non-HTTP errors', () => {
    expect(apiErrorMessage(new Error('Sensitive implementation detail'), 'Safe fallback')).toBe('Safe fallback');
  });
});
