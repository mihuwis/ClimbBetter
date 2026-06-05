import { buildMonthCalendar } from '../features/calendar/calendar.utils';
import { activityFeedMock } from '../shared/mock/dashboard.mock';

const monthDays = buildMonthCalendar(2026, 4); // maj 2026, bo monthIndex: 0 = styczeń


export function CalendarPage() {
  return (
    <main className="page-layout">
      <div className="page-header">
        <div>
          <h1>Calendar</h1>
          <p className="muted">
            Training load and climbing activity overview.
          </p>
        </div>

        <div className="view-switcher">
          <button className="active">Month</button>
          <button>Week</button>
          <button>Day</button>
        </div>
      </div>

      <div className="calendar-layout">
        <section className="calendar-panel">
          <div className="calendar-panel-header">
            <h2>May 2026</h2>
            <span className="muted">Score-based activity calendar</span>
          </div>

          <div className="calendar-weekdays">
            <span>Mon</span>
            <span>Tue</span>
            <span>Wed</span>
            <span>Thu</span>
            <span>Fri</span>
            <span>Sat</span>
            <span>Sun</span>
          </div>

    <div className="calendar-grid">
    {monthDays.map((day) => {
        const activitiesForDay = activityFeedMock.filter(
        (activity) => activity.sessionDate === day.date
        );

        const score = activitiesForDay.reduce(
        (sum, activity) => sum + activity.score,
        0
        );

        const label =
        activitiesForDay.length > 0
            ? activitiesForDay[0].discipline
            : 'Rest';

        return (
        <div
            className={[
            'calendar-day',
            score > 0 ? 'has-activity' : '',
            !day.isCurrentMonth ? 'outside-month' : '',
            ].join(' ')}
            key={day.date}
        >
            <div className="calendar-day-number">
            {day.dayOfMonth}
            </div>

            {score > 0 ? (
            <>
                <strong>{score}</strong>
                <span>{label}</span>
            </>
            ) : (
            <span className="muted">
                {day.isCurrentMonth ? label : ''}
            </span>
            )}
        </div>
        );
    })}
    </div>
        </section>

        <aside className="day-summary-panel">
          <section className="card">
            <h3>Selected day</h3>
            <p className="muted">20 May 2026</p>

            <h2>Bronx Boulder Session</h2>

            <div className="session-meta">
              <div>
                <strong>125</strong>
                <span>Score</span>
              </div>
              <div>
                <strong>6B+</strong>
                <span>Max grade</span>
              </div>
              <div>
                <strong>42</strong>
                <span>Moves</span>
              </div>
            </div>
          </section>

          <section className="card">
            <h3>This week</h3>
            <p>Total score</p>
            <strong>415 pts</strong>

            <p className="muted">
              3 climbing-related activities logged this week.
            </p>
          </section>
        </aside>
      </div>
    </main>
  );
}