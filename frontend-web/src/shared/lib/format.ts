export const number = (value: number | null | undefined) => value == null ? '—'
  : new Intl.NumberFormat('en-GB', { maximumFractionDigits: 2 }).format(value);
export function dateLabel(value: string) {
  return new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
    .format(new Date(`${value}T12:00:00`));
}
export function localDate(value = new Date()) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
}
