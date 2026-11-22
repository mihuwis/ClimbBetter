import { Link, useParams } from "react-router-dom";
import { SessionEditor } from "../features/training/components/SessionEditor";
import type {
  SessionDetails,
} from "../features/training/model/session.types";
import { useResource } from "../shared/api/useResource";
import { useCurrentUser } from "../features/auth/hooks/useAuth";
import { ApiError } from "../shared/api/http";
import { RequestState } from "../shared/ui/RequestState";

export function RepeatSessionPage() {
  const { sessionId } = useParams();
  const user = useCurrentUser();
  const source = useResource<SessionDetails>(
    sessionId ? `/training/sessions/${sessionId}` : null,
  );
  if (source.error instanceof ApiError && source.error.status === 404)
    return (
      <main className="workspace">
        <h1>Session not found</h1>
        <p>The session you want to repeat is unavailable.</p>
        <Link to="/">Back to dashboard</Link>
      </main>
    );
  return (
    <main className="workspace editor-workspace">
      <Link className="text-link" to={`/sessions/${sessionId}`}>
        ← Original session
      </Link>
      <div className="page-header">
        <div>
          <p className="eyebrow">Repeat a session</p>
          <h1>New session</h1>
          <p className="muted">
            Choose a date, then adjust the copied entries before saving.
          </p>
        </div>
      </div>
      <RequestState
        loading={user.loading || source.loading}
        error={user.error ?? source.error}
        retry={() => {
          user.refresh();
          source.refresh();
        }}
      />
      {user.data && source.data && (
        <SessionEditor
          key={`${user.data.userId}:repeat:${sessionId}`}
          userId={user.data.userId}
          mode="manual"
          repeatSource={source.data}
        />
      )}
    </main>
  );
}
