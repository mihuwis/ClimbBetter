import { AreaCard } from '../features/areas/components/AreaCard';
import { myAreasMock } from '../shared/mock/areas.mock';

export function AreasPage() {
  return (
    <main className="page-layout">
      <div className="page-header">
        <div>
          <h1>My Areas</h1>

          <p className="muted">
            Areas where you climb and train.
          </p>
        </div>
      </div>

      <div className="areas-grid">
        {myAreasMock.map((area) => (
          <AreaCard
            key={area.id}
            area={area}
          />
        ))}
      </div>
    </main>
  );
}