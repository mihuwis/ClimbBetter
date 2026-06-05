export type CalendarDay = {
  date: string;
  dayOfMonth: number;
  isCurrentMonth: boolean;
};

export function buildMonthCalendar(
  year: number,
  monthIndex: number
): CalendarDay[] {
  const firstDayOfMonth = new Date(year, monthIndex, 1);
  const lastDayOfMonth = new Date(year, monthIndex + 1, 0);

  const daysInMonth = lastDayOfMonth.getDate();

  const firstWeekday = firstDayOfMonth.getDay();
  const leadingEmptyDays = firstWeekday === 0 ? 6 : firstWeekday - 1;

  const days: CalendarDay[] = [];

  for (let i = 0; i < leadingEmptyDays; i++) {
    const date = new Date(year, monthIndex, 1 - leadingEmptyDays + i);

    days.push({
      date: toLocalDateString(date),
      dayOfMonth: date.getDate(),
      isCurrentMonth: false,
    });
  }

  for (let day = 1; day <= daysInMonth; day++) {
    const date = new Date(year, monthIndex, day);

    days.push({
      date: toLocalDateString(date),
      dayOfMonth: day,
      isCurrentMonth: true,
    });
  }

  while (days.length % 7 !== 0) {
    const previousDay = days[days.length - 1];

    const nextDate = new Date(
      Number(previousDay.date.slice(0, 4)),
      Number(previousDay.date.slice(5, 7)) - 1,
      Number(previousDay.date.slice(8, 10)) + 1
    );

    days.push({
      date: toLocalDateString(nextDate),
      dayOfMonth: nextDate.getDate(),
      isCurrentMonth: false,
    });
  }

  return days;
}

function toLocalDateString(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
}