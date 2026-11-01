import { Link } from 'react-router-dom';
import { SessionEditor } from '../features/training/components/SessionEditor';
import type { CurrentUser } from '../features/training/model/session.types';
import { useResource } from '../shared/api/useResource';
import { RequestState } from '../shared/ui/RequestState';

export function NewSessionPage({ live = false }: { live?: boolean }) {
  const user = useResource<CurrentUser>('/identity/me');
  return <main className="workspace editor-workspace"><Link className="text-link" to="/">← Dashboard</Link><div className="page-header"><div><p className="eyebrow">{live ? 'Climb now, save when you finish' : 'Add a completed training'}</p><h1>{live ? 'Start now' : 'New session'}</h1><p className="muted">One place, all your climbs, your own pace.</p></div></div>
    <RequestState loading={user.loading} error={user.error} retry={user.refresh}/>
    {user.data && <SessionEditor key={`${user.data.userId}:${live}`} userId={user.data.userId} mode={live ? 'live' : 'manual'}/>}</main>;
}
