import { errorMessage } from '../api/http';

export function RequestState({ loading, error, retry }: { loading?: boolean; error?: unknown; retry?: () => void }) {
  if (loading) return <div className="card" role="status">Loading…</div>;
  if (error) return <div className="notice error" role="alert"><p>{errorMessage(error)}</p>
    {retry && <button onClick={retry}>Try again</button>}</div>;
  return null;
}
