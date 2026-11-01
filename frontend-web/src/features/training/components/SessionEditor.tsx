import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import type { Area } from '../../catalog/model/catalog.types';
import type { EntryCommand, SessionDetails } from '../model/session.types';
import { commandFromDraft, draftKey, editDraft, newDraft, parseDraft } from '../model/session-draft';
import type { SessionDraft } from '../model/session-draft';
import { reorderEntries, zonedInput } from '../model/session.helpers';
import { createSession, updateSession } from '../api/sessions';
import { useResource } from '../../../shared/api/useResource';
import { ApiError, errorMessage } from '../../../shared/api/http';
import { RequestState } from '../../../shared/ui/RequestState';
import { EntryForm } from './EntryForm';

type Props = { userId: string; mode: 'manual' | 'live' | 'edit'; session?: SessionDetails };
export function SessionEditor({ userId, mode, session }: Props) {
  const key = draftKey(userId, mode, session?.sessionId);
  const [fallback] = useState(() => session ? editDraft(userId, session) : newDraft(userId, mode === 'live' ? 'live' : 'manual'));
  const [restored] = useState(() => {
    try { const draft = parseDraft(localStorage.getItem(key), userId); return { draft: draft ?? fallback, recovered: !!draft, error: '' }; }
    catch (error) { return { draft: fallback, recovered: false, error: errorMessage(error) }; }
  });
  const [draft, setDraft] = useState<SessionDraft>(restored.draft);
  const [storageError, setStorageError] = useState(restored.error);
  const [otherTab, setOtherTab] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [historyConflict, setHistoryConflict] = useState<string | null>(null);
  const reason = draft.changeReason ?? '';
  const [areaSearch, setAreaSearch] = useState('');
  const [searchParams] = useSearchParams();
  const [entryEditor, setEntryEditor] = useState<string | null>(() => restored.draft.editingEntryId ?? searchParams.get('entry'));
  const [clock, setClock] = useState(Date.now);
  const areas = useResource<Area[]>(`/catalog/areas?limit=50&q=${encodeURIComponent(areaSearch)}`);
  const navigate = useNavigate();
  const stale = !!session && draft.expectedVersion !== session.version;
  const locked = saving || !!draft.pendingRequest || otherTab || !!restored.error || stale;
  const entries = draft.command.entries;

  useEffect(() => {
    if (!draft.running) return;
    const timer = window.setInterval(() => setClock(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [draft.running]);
  useEffect(() => {
    const changed = (event: StorageEvent) => {
      if (event.key === key && event.newValue !== JSON.stringify(draft)) setOtherTab(true);
    };
    window.addEventListener('storage', changed);
    return () => window.removeEventListener('storage', changed);
  }, [key, draft]);

  function commit(next: SessionDraft) {
    const value = { ...next, revision: crypto.randomUUID() };
    try { localStorage.setItem(key, JSON.stringify(value)); setStorageError(''); }
    catch { setStorageError('Browser storage is unavailable or full. Keep this tab open until the session is saved.'); }
    setDraft(value);
  }
  function patchCommand(patch: Partial<SessionDraft['command']>) {
    commit({ ...draft, command: { ...draft.command, ...patch } });
  }
  function changeArea(value: string) {
    if ((entries.length || draft.entryDraft) && !window.confirm('Changing the place clears this draft’s entries. Continue?')) return;
    const area = areas.data?.find(a => a.id === value);
    commit({ ...draft, editingEntryId: null, entryDraft: undefined, placeName: area?.name, command: { ...draft.command, entries: [],
      areaId: value && value !== '__new' ? value : null, clientAreaId: value === '__new' ? crypto.randomUUID() : null, areaName: null } });
    setEntryEditor(null);
  }
  function putEntry(entry: EntryCommand) {
    const exists = entries.some(e => e.clientEntryId === entry.clientEntryId);
    commit({ ...draft, editingEntryId: null, entryDraft: undefined, command: { ...draft.command, entries: reorderEntries(exists ? entries.map(e => e.clientEntryId === entry.clientEntryId ? entry : e) : [...entries, entry]) } });
    setEntryEditor(null);
  }
  function openEntry(id: string | null) {
    if (entryEditor === id) return;
    if (entryEditor && entryEditor !== id && !window.confirm('Discard the entry currently being edited?')) return;
    commit({ ...draft, editingEntryId: id, entryDraft: undefined }); setEntryEditor(id);
  }
  function removeEntry(id: string) { patchCommand({ entries: reorderEntries(entries.filter(e => e.clientEntryId !== id)) }); }
  function removeClimb(entry: EntryCommand) {
    if (!window.confirm('Remove every entry for this climb from this session draft?')) return;
    const id = entry.climbId ?? entry.clientClimbId;
    patchCommand({ entries: reorderEntries(entries.filter(e => (e.climbId ?? e.clientClimbId) !== id)) });
  }
  function discard() {
    if (!window.confirm('Discard this saved draft? Unsaved changes will be lost.')) return;
    try { localStorage.removeItem(key); } catch { setStorageError('Could not discard the saved draft.'); return; }
    window.location.reload();
  }
  function timerAction() {
    const now = new Date();
    try {
    if (!draft.liveStarted) {
      const startInput = zonedInput(now.toISOString(), draft.command.timeZoneId);
      commit({ ...draft, running: true, liveStarted: true, startInput, endInput: startInput,
        command: { ...draft.command, sessionDate: startInput.slice(0, 10), startedAt: now.toISOString(), endedAt: now.toISOString() } });
    } else {
      commit({ ...draft, running: false, endInput: zonedInput(now.toISOString(), draft.command.timeZoneId), command: { ...draft.command, endedAt: now.toISOString() } });
    }
    setClock(now.getTime());
    } catch { setError('Enter a valid IANA time zone, for example Europe/Warsaw.'); }
  }
  async function save() {
    setError(''); setHistoryConflict(null);
    if (entryEditor) { setError('Apply or cancel the entry you are editing before saving the session.'); return; }
    if (mode === 'edit' && !reason.trim()) { setError('Enter a reason for this correction.'); return; }
    let command;
    try { command = commandFromDraft(draft); } catch (error) { setError(errorMessage(error)); return; }
    const pending = { ...draft, pendingRequest: command };
    commit(pending); setSaving(true);
    try {
      const result = mode === 'edit' && session
        ? await updateSession(session.sessionId, draft.expectedVersion!, reason, command)
        : await createSession(command);
      try { localStorage.removeItem(key); } catch { /* The confirmed result remains available on the server. */ }
      navigate(`/sessions/${result.sessionId}`, { replace: true });
    } catch (error) {
      setError(errorMessage(error));
      if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
        commit({ ...draft, pendingRequest: null });
        if (error.code === 'ASCENT_MODE_HISTORY_CONFLICT') setHistoryConflict(error.clientEntryId ?? null);
      }
    } finally { setSaving(false); }
  }
  const elapsed = draft.liveStarted ? Math.max(0, Math.floor(((draft.running ? clock : Date.parse(draft.command.endedAt)) - Date.parse(draft.command.startedAt)) / 1000)) : 0;
  const editing = entries.find(entry => entry.clientEntryId === entryEditor);
  return <div className="stack session-editor">
    {restored.recovered && <div className="notice">Your saved draft was recovered. It stays in this browser until a successful save or explicit discard.</div>}
    {storageError && <div className="notice error" role="alert">{storageError}</div>}
    {otherTab && <div className="notice error" role="alert">This draft changed in another tab. <button onClick={() => window.location.reload()}>Reload latest draft</button></div>}
    {stale && <div className="notice error">This session has a newer server version. Discard the old draft to edit the latest version.</div>}
    {draft.pendingRequest && <div className="notice">Waiting for a confirmed save. Your exact request is preserved; use Retry save before making more changes.</div>}
    <fieldset disabled={locked} className="editor-fields"><section className="card"><h2>Session</h2><div className="form-grid">
      <label className="full-width">Session name (optional)<input value={draft.command.sessionName ?? ''} placeholder="The backend can generate a name" onChange={e => patchCommand({ sessionName: e.target.value })}/></label>
      <label>Find a place<input value={areaSearch} onChange={e => setAreaSearch(e.target.value)} placeholder="Bronx, Bolechowicka…"/></label>
      <label>Place<select value={draft.command.areaId ?? (draft.command.clientAreaId ? '__new' : '')} onChange={e => changeArea(e.target.value)}><option value="">Choose a place…</option><option value="__new">Create a private place</option>
        {draft.command.areaId && !areas.data?.some(a => a.id === draft.command.areaId) && <option value={draft.command.areaId}>{draft.placeName ?? session?.areaName ?? 'Selected place'}</option>}
        {areas.data?.map(area => <option key={area.id} value={area.id}>{area.name} · {area.isPublic ? 'public' : 'private'}</option>)}</select></label>
      {draft.command.clientAreaId && <label className="full-width">New place name<input value={draft.command.areaName ?? ''} onChange={e => patchCommand({ areaName: e.target.value })}/></label>}
      <RequestState loading={areas.loading} error={areas.error} retry={areas.refresh}/>
      <label>Time zone<input value={draft.command.timeZoneId} readOnly={mode === 'live' && !!draft.liveStarted} onChange={e => patchCommand({ timeZoneId: e.target.value })} placeholder="Europe/Warsaw"/></label>
      <label>Start<input type="datetime-local" readOnly={mode === 'live'} value={draft.startInput} onChange={e => commit({ ...draft, startInput: e.target.value })}/></label>
      <label>End<input type="datetime-local" readOnly={mode === 'live'} value={draft.endInput} onChange={e => commit({ ...draft, endInput: e.target.value })}/></label>
    </div></section>
    {mode === 'live' && <section className="card timer-panel"><div><span className="eyebrow">{draft.running ? 'Climbing now' : draft.liveStarted ? 'Session stopped' : 'Ready when you are'}</span><strong className="timer">{String(Math.floor(elapsed / 3600)).padStart(2, '0')}:{String(Math.floor(elapsed / 60) % 60).padStart(2, '0')}:{String(elapsed % 60).padStart(2, '0')}</strong></div>
      {(!draft.liveStarted || draft.running) && <button className={draft.running ? 'danger' : 'primary'} onClick={timerAction} disabled={!draft.command.areaId && !draft.command.areaName?.trim()}>{draft.running ? 'Stop session' : 'Start climbing'}</button>}
      <p className="muted">The timer continues across refreshes. Stop before finishing.</p></section>}
    <section className="card"><div className="section-heading"><h2>Entries <span className="muted">({entries.length})</span></h2><button type="button" onClick={() => openEntry('new')} disabled={!draft.command.areaId && !draft.command.clientAreaId}>+ Add entry</button></div>
      {!entries.length && <p className="muted">Add a warm-up, an attempt or a completed climb.</p>}
      {entries.map((entry, i) => <div className={`draft-entry ${entryEditor === entry.clientEntryId ? 'selected' : ''}`} key={entry.clientEntryId}>
        <div><strong>#{i + 1} · {entry.resultType === 'WARMUP' ? 'Warm-up' : entry.climbName ?? 'Climb'}</strong><p className="muted">{entry.resultType} · {entry.gradeLabel ?? 'Unspecified'} {entry.ascentMode ?? ''} · {entry.executedMoves}{entry.totalMoves ? ` / ${entry.totalMoves}` : ''} moves</p></div>
        <div className="entry-actions"><button onClick={() => openEntry(entry.clientEntryId)}>Edit</button><button aria-label={`Move entry ${i + 1} up`} disabled={i === 0} onClick={() => { const next = [...entries]; [next[i - 1], next[i]] = [next[i], next[i - 1]]; patchCommand({ entries: reorderEntries(next) }); }}>↑</button><button className="danger" onClick={() => removeEntry(entry.clientEntryId)}>Remove entry</button>{entry.resultType !== 'WARMUP' && <button className="subtle" onClick={() => removeClimb(entry)}>Remove climb's entries</button>}</div>
      </div>)}
    </section>
    {entryEditor && <EntryForm key={entryEditor} areaId={draft.command.areaId ?? null} entries={entries} initial={editing} savedDraft={draft.entryDraft} onDraftChange={entryDraft => commit({ ...draft, editingEntryId: entryEditor, entryDraft })} onSave={putEntry} onCancel={() => openEntry(null)}/>}
    {mode === 'edit' && <section className="card"><label>Reason for correction<textarea value={reason} onChange={e => commit({ ...draft, changeReason: e.target.value })} placeholder="What did you correct?" required/></label></section>}
    </fieldset>
    {error && <div className="notice error" role="alert">{error}</div>}
    {historyConflict && <div className="notice"><p>OS/Flash conflicts with recorded history for this entry. Change the style to RP, or explicitly confirm your original declaration.</p><button onClick={() => {
      commit({ ...draft, command: { ...draft.command, entries: entries.map(e => e.clientEntryId === historyConflict ? { ...e, historyConflictConfirmed: true } : e) } });
      setHistoryConflict(null); setError('');
    }}>Confirm OS/Flash despite history</button></div>}
    <div className="sticky-actions"><span className="muted">{draft.running ? 'Recording · draft saved locally' : 'Changes stay in your local draft until you save.'}</span><div className="actions">
      <Link className="button secondary" to={session ? `/sessions/${session.sessionId}` : '/'}>Back</Link><button className="subtle danger" disabled={saving || otherTab} onClick={discard}>Discard draft</button>
      <button className="primary" disabled={saving || draft.running || otherTab || !!restored.error || stale || (mode === 'live' && !draft.liveStarted)} onClick={save}>{saving ? 'Saving…' : draft.pendingRequest ? 'Retry save' : mode === 'edit' ? 'Save changes' : 'Finish & save session'}</button></div></div>
  </div>;
}
