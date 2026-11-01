import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { deleteSession } from '../api/sessions';
import { errorMessage } from '../../../shared/api/http';
export function DeleteSessionButton({ id, version }: { id: string; version: number }) {
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  async function remove() {
    setBusy(true); setError('');
    try { await deleteSession(id, version); navigate('/'); }
    catch (error) { setError(errorMessage(error)); setBusy(false); }
  }
  return <div className="delete-control">{!confirming ? <button className="danger" onClick={() => setConfirming(true)}>Delete session</button>
    : <div className="notice"><p>Delete this entire session and all its entries from your journal?</p><div className="actions"><button className="danger" disabled={busy} onClick={remove}>{busy ? 'Deleting…' : 'Yes, delete session'}</button><button disabled={busy} onClick={() => setConfirming(false)}>Cancel</button></div></div>}
    {error && <p className="error-text" role="alert">{error} Reload this session before trying again.</p>}</div>;
}
