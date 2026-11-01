import { useState } from 'react';
import type { CatalogClimb, EffortProfile, Grade, Sector } from '../../catalog/model/catalog.types';
import type { ClimbType, EntryCommand } from '../model/session.types';
import { useResource } from '../../../shared/api/useResource';
import { RequestState } from '../../../shared/ui/RequestState';
import type { EntryDraft } from '../model/session-draft';

type Props = { areaId: string | null; entries: EntryCommand[]; initial?: EntryCommand; savedDraft?: EntryDraft; onDraftChange: (value: EntryDraft) => void; onSave: (entry: EntryCommand) => void; onCancel: () => void };
export function EntryForm({ areaId, entries, initial, savedDraft, onDraftChange, onSave, onCancel }: Props) {
  const [entry, setEntry] = useState<EntryCommand>(() => savedDraft?.entry ?? initial ?? { clientEntryId: crypto.randomUUID(), entryOrder: entries.length, resultType: 'ASCENT', climbType: 'BOULDER', ascentMode: 'OS', totalMoves: 8, executedMoves: 8, effortProfileCode: 'BOULDER' });
  const [mode, setMode] = useState(savedDraft?.mode ?? (initial?.resultType === 'WARMUP' ? 'warmup' : initial?.climbId ? 'existing' : initial?.clientClimbId ? 'new' : areaId ? 'existing' : 'new'));
  const [query, setQuery] = useState('');
  const [sector, setSector] = useState('');
  const [error, setError] = useState('');
  const type = entry.climbType ?? 'BOULDER';
  const climbs = useResource<CatalogClimb[]>(mode === 'existing' && areaId ? `/catalog/areas/${areaId}/climbs?limit=50&climbType=${type}&q=${encodeURIComponent(query)}${sector ? `&sectorId=${sector}` : ''}` : null);
  const sectors = useResource<Sector[]>(areaId ? `/catalog/areas/${areaId}/sectors` : null);
  const grades = useResource<Grade[]>(mode !== 'warmup' ? `/catalog/grades?climbType=${type}` : null);
  const profiles = useResource<EffortProfile[]>('/catalog/effort-profiles');
  const localClimbs = [...new Map(entries.filter(e => e.clientClimbId).map(e => [e.clientClimbId!, e])).values()];
  function patch(value: Partial<EntryCommand>, nextMode = mode) { const next = { ...entry, ...value }; setEntry(next); onDraftChange({ entry: next, mode: nextMode }); }
  function changeMode(value: string) {
    setMode(value); setError('');
    patch({ climbId: null, clientClimbId: value === 'new' ? crypto.randomUUID() : null, climbName: '', historyConflictConfirmed: false, resultType: value === 'warmup' ? 'WARMUP' : 'ASCENT' }, value);
  }
  function chooseClimb(id: string) {
    const climb = climbs.data?.find(c => c.id === id);
    if (!climb) { patch({ climbId: null }); return; }
    patch({ climbId: climb.id, clientClimbId: null, climbName: climb.name, climbType: climb.climbType,
      gradeLabel: climb.gradeLabel ?? '', effortProfileCode: climb.effortProfileCode ?? (type === 'BOULDER' ? 'BOULDER' : type === 'CIRCUIT' ? 'CIRCUIT' : 'MEDIUM_CONTINUOUS'),
      totalMoves: climb.suggestedMoveCount, executedMoves: climb.suggestedMoveCount ?? 1,
      protectionMode: type === 'ROUTE' ? 'LEAD' : null, ascentMode: 'RP', historyConflictConfirmed: false });
  }
  function changeType(value: ClimbType) {
    patch({ climbType: value, climbId: null, gradeLabel: '', effortProfileCode: value === 'BOULDER' ? 'BOULDER' : value === 'CIRCUIT' ? 'CIRCUIT' : 'MEDIUM_CONTINUOUS', protectionMode: value === 'ROUTE' ? 'LEAD' : null, totalMoves: value === 'BOULDER' ? 8 : null });
  }
  function submit() {
    setError('');
    if (!Number.isInteger(entry.executedMoves) || entry.executedMoves <= 0) { setError('Executed moves must be a positive whole number.'); return; }
    if (mode === 'warmup') { onSave({ clientEntryId: entry.clientEntryId, entryOrder: entry.entryOrder, resultType: 'WARMUP', executedMoves: entry.executedMoves, notes: entry.notes }); return; }
    if (mode === 'existing' && !entry.climbId) { setError('Choose a climb.'); return; }
    if ((mode === 'new' || mode === 'draft') && !entry.climbName?.trim()) { setError('Enter a climb name.'); return; }
    if (!entry.gradeLabel || !entry.effortProfileCode || !entry.totalMoves || entry.totalMoves <= 0 || !Number.isInteger(entry.totalMoves)) { setError('Choose a grade, effort profile and a positive whole number of total moves.'); return; }
    onSave({ ...entry, climbType: type, clientClimbId: entry.climbId ? null : entry.clientClimbId ?? crypto.randomUUID(), protectionMode: type === 'ROUTE' ? entry.protectionMode ?? 'LEAD' : null });
  }
  return <section className="card entry-form"><div className="section-heading"><h2>{initial ? 'Edit entry' : 'Add entry'}</h2><button type="button" onClick={onCancel}>Cancel</button></div>
    <div className="form-grid"><label>Entry source<select value={mode} onChange={e => changeMode(e.target.value)}>
      {areaId && <option value="existing">Find a known climb</option>}<option value="new">New private climb</option>{localClimbs.length > 0 && <option value="draft">Repeat a new climb from this draft</option>}<option value="warmup">Warm-up / unspecified climbing</option></select></label>
      {mode !== 'warmup' && <label>Climb type<select value={type} onChange={e => changeType(e.target.value as ClimbType)}><option value="BOULDER">Boulder</option><option value="ROUTE">Route</option><option value="CIRCUIT">Circuit</option></select></label>}
      {mode === 'existing' && <><label>Search by name<input value={query} onChange={e => setQuery(e.target.value)} placeholder="e.g. niebieski"/></label>
        {!!sectors.data?.length && <label>Sector<select value={sector} onChange={e => setSector(e.target.value)}><option value="">All sectors</option>{sectors.data.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select></label>}
        <label className="full-width">Climb<select value={entry.climbId ?? ''} onChange={e => chooseClimb(e.target.value)}><option value="">Choose a climb…</option>
          {entry.climbId && !climbs.data?.some(c => c.id === entry.climbId) && <option value={entry.climbId}>{entry.climbName ?? 'Current climb'}</option>}
          {climbs.data?.map(c => <option key={c.id} value={c.id}>{c.name} · {c.inputGradeLabel ?? c.gradeLabel ?? 'Ungraded'}{c.sectorName ? ` · ${c.sectorName}` : ''}</option>)}</select></label><RequestState loading={climbs.loading} error={climbs.error} retry={climbs.refresh}/>
        {climbs.data?.length === 50 && <p className="muted">Showing 50 matches. Refine the name or sector to find more.</p>}</>}
      {mode === 'draft' && <label className="full-width">Climb in this draft<select value={entry.clientClimbId ?? ''} onChange={e => { const selected = localClimbs.find(c => c.clientClimbId === e.target.value); if (selected) patch({ ...selected, clientEntryId: entry.clientEntryId, entryOrder: entry.entryOrder, ascentMode: 'RP', historyConflictConfirmed: false }); }}><option value="">Choose a climb…</option>{localClimbs.map(c => <option key={c.clientClimbId!} value={c.clientClimbId!}>{c.climbName}</option>)}</select></label>}
      {mode === 'new' && <label className="full-width">Climb name<input value={entry.climbName ?? ''} onChange={e => patch({ climbName: e.target.value })} placeholder="Name your climb"/></label>}
      {mode !== 'warmup' && <><label>Grade (canonical)<select value={entry.gradeLabel ?? ''} onChange={e => patch({ gradeLabel: e.target.value })}><option value="">Choose grade…</option>{grades.data?.map(g => <option key={g.id} value={g.label}>{g.label}</option>)}</select></label>
        <label>Effort profile<select value={entry.effortProfileCode ?? ''} onChange={e => patch({ effortProfileCode: e.target.value })}><option value="">Choose profile…</option>{profiles.data?.map(p => <option key={p.code} value={p.code}>{p.displayName}</option>)}</select></label>
        <label>Result<select value={entry.resultType} onChange={e => patch({ resultType: e.target.value as EntryCommand['resultType'] })}><option value="ASCENT">Completed</option><option value="ATTEMPT">Attempt</option></select></label>
        <label>Ascent mode<select value={entry.ascentMode ?? 'OS'} onChange={e => patch({ ascentMode: e.target.value as EntryCommand['ascentMode'], historyConflictConfirmed: false })}><option value="OS">On-sight (OS)</option><option value="FLASH">Flash</option><option value="RP">Redpoint (RP)</option></select></label>
        {type === 'ROUTE' && <label>Protection<select value={entry.protectionMode ?? 'LEAD'} onChange={e => patch({ protectionMode: e.target.value as EntryCommand['protectionMode'] })}><option value="LEAD">Lead</option><option value="TOP_ROPE">Top rope</option></select></label>}
        <label>Total moves<input type="number" min="1" step="1" value={entry.totalMoves ?? ''} onChange={e => patch({ totalMoves: Number(e.target.value) })}/></label>
        <RequestState loading={grades.loading || profiles.loading} error={grades.error ?? profiles.error}/>
      </>}
      <label>Executed moves<input type="number" min="1" step="1" value={entry.executedMoves || ''} onChange={e => patch({ executedMoves: Number(e.target.value) })}/></label>
      <label className="full-width">Entry notes<textarea rows={2} value={entry.notes ?? ''} onChange={e => patch({ notes: e.target.value })}/></label>
      {entry.historyConflictConfirmed && <label className="checkbox-row full-width"><input type="checkbox" checked onChange={() => patch({ historyConflictConfirmed: false })}/>History exception confirmed — clear if you changed the history or style</label>}
    </div>{error && <p role="alert" className="error-text">{error}</p>}<button type="button" className="primary" onClick={submit}>{initial ? 'Apply entry changes' : 'Add to session'}</button>
  </section>;
}
