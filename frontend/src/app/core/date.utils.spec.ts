import { toLocalDateInputValue } from './date.utils';

describe('toLocalDateInputValue', () => {
  it('formats the calendar date in the browser timezone', () => {
    const localDate = new Date(2025, 0, 2, 0, 30);

    expect(toLocalDateInputValue(localDate)).toBe('2025-01-02');
  });
});
