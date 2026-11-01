import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useResource } from '../shared/api/useResource';
import { ApiError } from '../shared/api/http';
import { RequestState } from '../shared/ui/RequestState';
import { dateLabel, number } from '../shared/lib/format';
import type { SessionDetails, SessionEntry } from '../features/training/model/session.types';
import { groupEntries } from '../features/training/model/session.helpers';
import { SessionSummary } from '../features/training/components/SessionSummary';
import { DeleteSessionButton } from '../features/training/components/DeleteSessionButton';

function EntryRows({ entries, sessionId }: { entries: SessionEntry[]; sessionId: string }) {
  return <div className="entry-list">{entries.map(entry => <article className="entry-detail" key={entry.clientEntryId}>
    <div className="section-heading"><h3>#{entry.entryOrder + 1} · {entry.resultType === 'ASCENT' ? 'Completed' : entry.resultType === 'ATTEMPT' ? 'Attempt' : 'Warm-up'}</h3><Link className="text-link" to={`/sessions/${sessionId}/edit?entry=${entry.clientEntryId}`}>Edit</Link></div>
    <div className="badges"><span>{entry.gradeLabel ?? 'Unspecified climbing'}</span>{entry.ascentMode && <span>{entry.ascentMode}</span>}{entry.protectionMode && <span>{entry.protectionMode.replace('_', ' ')}</span>}<span>{entry.executedMoves}{entry.totalMoves ? ` / ${entry.totalMoves}` : ''} moves</span></div>
    <div className="stat-row"><span>Adjusted Load</span><strong>{number(entry.calculation.adjustedLoad)}</strong></div>
    <div className="stat-row"><span>Classic Load</span><strong>{number(entry.calculation.classicLoad)}</strong></div>
    {entry.notes && <p className="notes">{entry.notes}</p>}
    {entry.resultType !== 'WARMUP' && <details><summary>Calculation details</summary><dl className="calculation-details">
      <dt>EDL</dt><dd>{number(entry.calculation.edlCount)}</dd><dt>Move intensity</dt><dd>{number(entry.calculation.moveIntensity)}</dd>
      <dt>Reference level</dt><dd>{entry.calculation.currentLevelGradeLabel ?? '—'}</dd><dt>Style multiplier</dt><dd>{number(entry.calculation.styleMultiplier)}</dd>
      <dt>Relative effort multiplier</dt><dd>{number(entry.calculation.relativeEffortMultiplier)}</dd><dt>All-time contact</dt><dd>{entry.history?.attemptOrdinalAllTime ?? '—'}</dd>
    </dl></details>}
  </article>)}</div>;
}

function Details({ session }: { session: SessionDetails }) {
  const groups = groupEntries(session.entries);
  const [selected, setSelected] = useState(groups[0]?.key ?? '');
  const [chronology, setChronology] = useState(false);
  const group = groups.find(item => item.key === selected) ?? groups[0];
  return <main className="workspace"><Link className="text-link" to="/">← Dashboard</Link>
    <div className="page-header"><div><p className="eyebrow">{dateLabel(session.sessionDate)} · {session.areaName}</p><h1>{session.sessionName}</h1><p className="muted">{session.timeZoneId}</p></div><div className="actions"><Link className="button primary" to={`/sessions/${session.sessionId}/edit`}>Edit session</Link></div></div>
    <SessionSummary session={session}/>
    <div className="section-heading section-gap"><h2>Climbs & attempts</h2><button aria-pressed={chronology} onClick={() => setChronology(!chronology)}>{chronology ? 'Group by climb' : 'Show chronology'}</button></div>
    {chronology ? <section className="card"><EntryRows entries={[...session.entries].sort((a, b) => a.entryOrder - b.entryOrder)} sessionId={session.sessionId}/></section>
      : <div className="details-grid"><aside className="card climb-groups">{groups.map(item => <button className={`group-option ${item.key === group?.key ? 'selected' : ''}`} onClick={() => setSelected(item.key)} key={item.key}><strong>{item.name}</strong><span>{item.entries.length} {item.entries.length === 1 ? 'entry' : 'entries'} · {item.entries.filter(e => e.resultType === 'ASCENT').length} completed</span></button>)}</aside>
        <section className="card">{group && <><div className="section-heading"><h2>{group.name}</h2><Link className="text-link" to={`/sessions/${session.sessionId}/edit`}>Add or remove entries</Link></div><EntryRows entries={group.entries} sessionId={session.sessionId}/></>}</section></div>}
    <div className="section-gap"><DeleteSessionButton id={session.sessionId} version={session.version}/></div>
  </main>;
}

export function SessionDetailsPage() {
  const { sessionId } = useParams();
  const result = useResource<SessionDetails>(sessionId ? `/training/sessions/${sessionId}` : null);
  if (result.error instanceof ApiError && result.error.status === 404) return <main className="workspace"><h1>Session not found</h1><p>This session is unavailable.</p><Link to="/">Back to dashboard</Link></main>;
  return result.data ? <Details key={`${result.data.sessionId}:${result.data.version}`} session={result.data}/> : <main className="workspace"><RequestState loading={result.loading} error={result.error} retry={result.refresh}/></main>;
}
