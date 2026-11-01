import { useState } from 'react';
import type { Area, CatalogClimb, Sector } from '../features/catalog/model/catalog.types';
import { useResource } from '../shared/api/useResource';
import { RequestState } from '../shared/ui/RequestState';
import { Link } from 'react-router-dom';

function AreaClimbs({ area }: { area: Area }) {
  const [query, setQuery] = useState('');
  const [sector, setSector] = useState('');
  const sectors = useResource<Sector[]>(`/catalog/areas/${area.id}/sectors`);
  const climbs = useResource<CatalogClimb[]>(`/catalog/areas/${area.id}/climbs?limit=50&q=${encodeURIComponent(query)}${sector ? `&sectorId=${sector}` : ''}`);
  return <section className="card"><div className="section-heading"><h2>{area.name}</h2><Link className="text-link" to="/sessions/new">Add a session →</Link></div>
    <div className="form-grid"><label>Find a climb<input value={query} onChange={e => setQuery(e.target.value)}/></label><label>Sector<select value={sector} onChange={e => setSector(e.target.value)}><option value="">All sectors</option>{sectors.data?.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select></label></div>
    <RequestState loading={climbs.loading} error={climbs.error} retry={climbs.refresh}/><div className="catalog-list">{climbs.data?.map(climb => <article key={climb.id}><div><strong>{climb.name}</strong><p className="muted">{climb.sectorName} · {climb.climbType.toLowerCase()} · {climb.isPublic ? 'public' : 'private'}</p></div><div><strong>{climb.inputGradeLabel ?? climb.gradeLabel ?? 'Ungraded'}</strong><p className="muted">{climb.lengthMeters ? `${climb.lengthMeters} m · ` : ''}{climb.suggestedMoveCount ?? '—'} moves</p></div></article>)}</div>
    {climbs.data?.length === 0 && <p className="muted">No matching climbs. You can add a new private climb while recording a session.</p>}{climbs.data?.length === 50 && <p className="muted">Showing 50 matches. Refine your search for more.</p>}
  </section>;
}
export function AreasPage() {
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<Area | null>(null);
  const areas = useResource<Area[]>(`/catalog/areas?limit=50&q=${encodeURIComponent(query)}`);
  return <main className="workspace"><div className="page-header"><div><p className="eyebrow">Your climbing places</p><h1>Areas</h1><p className="muted">Public places and your private climbs.</p></div></div>
    <label className="search-label">Search places<input value={query} onChange={e => setQuery(e.target.value)} placeholder="Bronx, Bolechowicka…"/></label><RequestState loading={areas.loading} error={areas.error} retry={areas.refresh}/>
    <div className="area-picker section-gap">{areas.data?.map(area => <button className={`card area-option ${selected?.id === area.id ? 'selected' : ''}`} key={area.id} onClick={() => setSelected(area)}><span className="eyebrow">{area.areaType === 'ROCK' ? 'Outdoor' : 'Indoor'} · {area.isPublic ? 'public' : 'private'}</span><strong>{area.name}</strong><span className="muted">{area.region ?? area.facilityType?.replaceAll('_', ' ') ?? 'Climbing area'}</span></button>)}</div>
    {selected && <AreaClimbs key={selected.id} area={selected}/>}
  </main>;
}
