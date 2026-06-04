import type { DisciplineSummaryItem } from '../../../shared/types/dashboard.types';

type DisciplineSummaryCardProps = {
  disciplines: DisciplineSummaryItem[];
};

export function DisciplineSummaryCard({
  disciplines,
}: DisciplineSummaryCardProps) {
  return (
    <section className="card">
      <h3>Discipline summary</h3>

      <div className="discipline-list">
        {disciplines.map((discipline) => (
          <div className="discipline-item" key={discipline.id}>
            <div>
              <strong>{discipline.name}</strong>
              <p className="muted">{discipline.sessionsCount} sessions</p>
            </div>

            <div className="discipline-metrics">
              <span>{discipline.totalScore} pts</span>
              <span>{discipline.totalMoves} moves</span>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}