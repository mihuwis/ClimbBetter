import { Link, useParams } from 'react-router-dom';
import { SessionEditor } from '../features/training/components/SessionEditor';
import type { CurrentUser, SessionDetails } from '../features/training/model/session.types';
import { useResource } from '../shared/api/useResource';
import { ApiError } from '../shared/api/http';
import { RequestState } from '../shared/ui/RequestState';

export function SessionEditPage() {
  const { sessionId } = useParams();
  const user = useResource<CurrentUser>('/identity/me');
  const session = useResource<SessionDetails>(sessionId ? `/training/sessions/${sessionId}` : null);
  if (session.error instanceof ApiError && session.error.status === 404) return <main className="workspace"><h1>Session not found</h1><Link to="/">Back to dashboard</Link></main>;
  return <main className="workspace editor-workspace"><Link className="text-link" to={`/sessions/${sessionId}`}>← Session details</Link><div className="page-header"><div><p className="eyebrow">Correct your training journal</p><h1>Edit session</h1><p className="muted">Changes are recalculated when you save.</p></div></div>
    <RequestState loading={user.loading || session.loading} error={user.error ?? session.error} retry={() => { user.refresh(); session.refresh(); }}/>
    {user.data && session.data && <SessionEditor key={`${user.data.userId}:${sessionId}:${session.data.version}`} userId={user.data.userId} mode="edit" session={session.data}/>}</main>;
}
